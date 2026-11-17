import Foundation
private struct BinaryReader {
    let bytes: [UInt8]; var offset=0
    mutating func uint(_ count: Int) throws -> UInt32 {
        guard offset+count<=bytes.count else {throw ContentError.invalidIndex}
        var n:UInt32=0;for _ in 0..<count {n=(n<<8)|UInt32(bytes[offset]);offset+=1};return n
    }
    mutating func float() throws -> Float {Float(bitPattern:try uint(4))}
    mutating func data(_ count: Int) throws -> [UInt8] {
        guard count>=0,offset+count<=bytes.count else {throw ContentError.invalidIndex}
        let d=Array(bytes[offset..<offset+count]);offset+=count;return d
    }
    mutating func text() throws -> String {let n=Int(try uint(2));guard n>0,n<=1024,let s=String(bytes:try data(n),encoding:.utf8) else {throw ContentError.invalidIndex};return s}
}
public struct SearchResult { public let verses: [Verse], surahs: [Surah], approximate: Bool, invalid: Bool }
public final class QuranSearch {
    private let corpus: QuranCorpus
    private let surahAliases:[[String]]
    private var lengths=[Int](),postings=[String:[(Int,Double)]]()
    private struct Term {let idf:Float,scale:Float,vector:[Int8]}
    private var terms=[String:Term](),docs=[[Int8]](),scales=[Float](),stop=Set<String>(),aliases=[String:String]()
    public init(corpus:QuranCorpus,lexical:Data,semantic:Data,metadata:Data,names:Data) throws {
        self.corpus=corpus
        surahAliases=try JSONDecoder().decode([[String]].self,from:names)
        guard surahAliases.count==114 else {throw ContentError.invalidIndex}
        var a=BinaryReader(bytes:Array(lexical))
        guard try a.uint(4)==0x4e4c4931,try a.uint(2)==6236 else {throw ContentError.invalidIndex}
        let count=Int(try a.uint(4));guard (1...100000).contains(count) else {throw ContentError.invalidIndex}
        for _ in 0..<6236 {lengths.append(Int(try a.uint(2)))}
        for _ in 0..<count {
            let t=try a.text(),n=Int(try a.uint(2));var values=[(Int,Double)]()
            for _ in 0..<n {let id=Int(try a.uint(2))-1,tf=Double(try a.uint(1));guard (0..<6236).contains(id),tf>0 else {throw ContentError.invalidIndex};values.append((id,tf))};postings[t]=values
        }
        guard a.offset==a.bytes.count else {throw ContentError.invalidIndex}
        var b=BinaryReader(bytes:Array(semantic))
        guard try b.uint(4)==0x4e53454d,try b.uint(2)==128,try b.uint(2)==6236 else {throw ContentError.invalidIndex}
        let vocab=Int(try b.uint(4));guard (1...20000).contains(vocab) else {throw ContentError.invalidIndex}
        for _ in 0..<vocab {let t=try b.text(),idf=try b.float(),scale=try b.float(),v=try b.data(128);guard idf.isFinite,scale.isFinite,scale>0 else {throw ContentError.invalidIndex};terms[t]=Term(idf:idf,scale:scale,vector:v.map { Int8(bitPattern:$0) })}
        for _ in 0..<6236 {scales.append(try b.float());docs.append(try b.data(128).map {Int8(bitPattern:$0)})}
        guard b.offset==b.bytes.count else {throw ContentError.invalidIndex}
        struct Meta:Decodable {let stopWords:[String],aliases:[String:String]}
        let meta=try JSONDecoder().decode(Meta.self,from:metadata);stop=Set(meta.stopWords);aliases=meta.aliases
    }
    public func find(_ raw:String,semantic:Bool=true) -> SearchResult {
        let q=String(raw.prefix(1000)).trimmingCharacters(in:.whitespacesAndNewlines)
        if q.isEmpty {return SearchResult(verses:[],surahs:[],approximate:false,invalid:false)}
        if let r=reference(q) {return r}
        let ws=Normalization.words(q).filter {$0.count>=2 && !stop.contains($0)}.flatMap {Normalization.words(aliases[$0] ?? $0)}.filter {!stop.contains($0)}
        let unique=Array(Set(ws)).sorted().prefix(16)
        if unique.isEmpty {return SearchResult(verses:[],surahs:[],approximate:false,invalid:false)}
        var score=Array(repeating:0.0,count:6236),coverage=Array(repeating:0,count:6236)
        let mean=Double(lengths.reduce(0,+))/6236
        for w in unique {if let values=postings[w] {
            let idf=log(1+(6236-Double(values.count)+0.5)/(Double(values.count)+0.5))
            for (i,tf) in values {score[i]+=idf*tf*2.2/(tf+1.2*(0.25+0.75*Double(lengths[i])/mean));coverage[i]+=1}
        }}
        var vector=Array(repeating:Float(0),count:128)
        for (w,group) in Dictionary(grouping:ws,by:{$0}) {if let t=terms[w] {let weight=Float(1+log(Double(group.count)))*t.idf*t.scale;for i in 0..<128 {vector[i]+=Float(t.vector[i])*weight}}}
        let norm=sqrt(vector.reduce(0){$0+$1*$1});if norm>0 {vector=vector.map {$0/norm}}
        let maxScore=max(score.max() ?? 1,1)
        var ranked=[(Int,Double)]()
        for i in 0..<6236 {
            var cosine:Float=0,length:Float=0
            if semantic && norm>0 {for j in 0..<128 {let v=Float(docs[i][j])*scales[i];cosine+=v*vector[j];length+=v*v};if length>0 {cosine/=sqrt(length)}}
            guard score[i]>0 || cosine>=0.5 else {continue}
            let translation=Set(Normalization.words(corpus.verses[i].indonesian+" "+corpus.verses[i].english))
            let direct=Double(unique.filter {translation.contains($0)}.count)/Double(unique.count)
            let rank=semantic ? score[i]/maxScore*0.55+Double(cosine)*0.35+Double(coverage[i])/Double(unique.count)*0.15+direct*0.45 : score[i]+direct*3
            ranked.append((i,rank))
        }
        ranked.sort { $0.1 == $1.1 ? $0.0<$1.0 : $0.1>$1.1 }
        return SearchResult(verses:ranked.prefix(20).map {corpus.verses[$0.0]},surahs:[],approximate:false,invalid:false)
    }
    private func reference(_ raw:String) -> SearchResult? {
        if raw.range(of:"(?:^|\\b(?:surat|surah|ayat)\\s+|:)\\s*-\\s*\\d",options:[.regularExpression,.caseInsensitive]) != nil {return SearchResult(verses:[],surahs:[],approximate:false,invalid:true)}
        if Normalization.search(raw).range(of:"\\bayat(?:ul)? kursi\\b",options:.regularExpression) != nil,let verse=corpus.verse(Reference(2,255)){return SearchResult(verses:[verse],surahs:[],approximate:false,invalid:false)}
        let direct=try! NSRegularExpression(pattern:"(?<!\\d)(\\d{1,4})\\s*[:.]\\s*(\\d{1,4})(?!\\d)")
        let ns=raw as NSString
        if let m=direct.firstMatch(in:raw,range:NSRange(location:0,length:ns.length)) {
            let r=Reference(Int(ns.substring(with:m.range(at:1)))!,Int(ns.substring(with:m.range(at:2)))!)
            let v=corpus.verse(r);return SearchResult(verses:v.map {[$0]} ?? [],surahs:[],approximate:false,invalid:v==nil)
        }
        let spoken=Normalization.spoken(raw);var words=spoken.split(separator:" ").map(String.init)
        var ayah:Int?
        if let i=words.firstIndex(of:"ayat"),i+1<words.count,let n=Int(words[i+1]) {ayah=n;words.removeSubrange(i...i+1)}
        let clean=words.filter {!["cari","carikan","tolong","buka","surat","surah","qs","quran","al","alquran"].contains($0)}.joined(separator:" ")
        guard !clean.isEmpty else {return nil}
        let compact: (String)->String = { value in
            var words=Normalization.words(value)
            if let first=words.first,["al","an","at","as","asy","ash","az","ad","ar"].contains(first) {words.removeFirst()}
            var key=words.joined().replacingOccurrences(of:"ee",with:"i").replacingOccurrences(of:"oo",with:"u").replacingOccurrences(of:"([aiu])\\1+",with:"$1",options:.regularExpression)
            if key.hasSuffix("h"){key.removeLast()};return key
        }
        let name=compact(clean)
        func names(_ s:Surah)->[String] {([s.name,s.arabicName]+surahAliases[s.id-1]).map(compact)}
        var approximate=false
        var matches=corpus.surahs.filter {String($0.id)==clean || names($0).contains(name)}
        if matches.isEmpty,clean.count>=4 {
            matches=corpus.surahs.filter {names($0).contains(where:{$0.contains(name)})}
            if matches.isEmpty && (words.contains("surat") || words.contains("surah") || ayah != nil) {
                matches=corpus.surahs.filter {names($0).contains(where:{editDistance($0,name)<=max(1,name.count/5)})}
                approximate = !matches.isEmpty
            }
        }
        guard !matches.isEmpty else {return nil}
        if let a=ayah {let verses=matches.compactMap {corpus.verse(Reference($0.id,a))};return SearchResult(verses:verses,surahs:[],approximate:approximate,invalid:verses.isEmpty)}
        return SearchResult(verses:[],surahs:matches,approximate:approximate,invalid:false)
    }
    private func editDistance(_ a:String,_ b:String)->Int {
        let x=Array(a),y=Array(b);var last=Array(0...y.count)
        for (i,c) in x.enumerated() {var row=[i+1];for (j,d) in y.enumerated() {row.append(min(row[j]+1,min(last[j+1]+1,last[j]+(c==d ? 0:1))))};last=row};return last[y.count]
    }
}
