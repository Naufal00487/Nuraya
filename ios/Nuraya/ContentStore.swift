import Foundation
import UIKit
import CryptoKit
import SQLite3
import NurayaCore

final class ContentStore {
    let root:URL,corpus:QuranCorpus,qaris:[ReciterSelection],chapterReciters:[ChapterReciter],verseReciters:[VerseReciter],portraits:[Portrait],modes:[ReadingMode],lessons:[Lesson],themes:[QuranTheme],manifest:MushafManifest
    private let imageCache:NSCache<NSString,UIImage> = {let cache=NSCache<NSString,UIImage>();cache.totalCostLimit=64*1024*1024;cache.countLimit=64;return cache}()
    private var studyCache=[Int:StudyChapter](),latinCache=[Int:LatinChapter](),indexCache=[String:PageIndex](),mapCache=[String:MushafMap]()
    lazy var search:QuranSearch? = try? QuranSearch(corpus:corpus,lexical:Data(contentsOf:root.appendingPathComponent("learning_index.bin")),semantic:Data(contentsOf:root.appendingPathComponent("semantic_index.bin")),metadata:Data(contentsOf:root.appendingPathComponent("semantic_index_sources.json")),names:Data(contentsOf:root.appendingPathComponent("surah_aliases.json")))
    init(root:URL) throws {
        self.root=root
        func decode<T:Decodable>(_ path:String,_ type:T.Type) throws -> T {try JSONDecoder().decode(type,from:Data(contentsOf:root.appendingPathComponent(path)))}
        corpus=try QuranCorpus.load(Data(contentsOf:root.appendingPathComponent("quran.json")))
        qaris=try decode("curated_reciters.json",[ReciterSelection].self)
        struct Chapters:Decodable {let qaris:[ChapterReciter]};struct Verses:Decodable {let reciters:[VerseReciter]};struct Pictures:Decodable {let portraits:[Portrait]}
        chapterReciters=try decode("reciters.json",Chapters.self).qaris;verseReciters=try decode("verse_reciters.json",Verses.self).reciters;portraits=try decode("portraits/catalog.json",Pictures.self).portraits
        modes=try decode("reading_modes.json",[ReadingMode].self);lessons=try decode("learning_course.json",LearningCourse.self).lessons;themes=try decode("themes.json",[QuranTheme].self)
        manifest=try decode("printed_mushaf/catalog.json",MushafManifest.self)
        guard qaris.count==28,lessons.count==25,manifest.commit=="df1ca931ca6e83f8ff6b2d3a9308406e11994d00",manifest.editions.count==7 else {throw ContentError.invalidCorpus}
        for edition in MushafEdition.allCases {try index(edition).validate(corpus)}
    }
    func decode<T:Decodable>(_ path:String,_ type:T.Type=T.self) throws -> T {try JSONDecoder().decode(type,from:Data(contentsOf:root.appendingPathComponent(path)))}
    func study(_ ref:Reference)->StudyVerse? {
        if studyCache[ref.surah]==nil {studyCache[ref.surah]=try? decode("study/\(ref.surah).json",StudyChapter.self)}
        return studyCache[ref.surah]?.verses.first {$0.ayah==ref.ayah}
    }
    func latin(_ ref:Reference)->String? {
        if latinCache[ref.surah]==nil {latinCache[ref.surah]=try? decode("latin/\(ref.surah).json",LatinChapter.self)}
        return latinCache[ref.surah]?.verses.first {$0.ayah==ref.ayah}?.latin
    }
    func index(_ edition:MushafEdition) throws -> PageIndex {
        if let i=indexCache[edition.index] {return i};let i:PageIndex=try decode("printed_mushaf/\(edition.index)-index.json");indexCache[edition.index]=i;return i
    }
    func regions(_ edition:MushafEdition,_ page:Int)->[MushafRegion] {
        guard let mapping=edition.mapping else {return []}
        if mapCache[mapping]==nil {mapCache[mapping]=try? decode("printed_mushaf/\(mapping)-map.json",MushafMap.self)}
        return mapCache[mapping]?.pages.first {$0.page==page}?.verses ?? []
    }
    func timing(_ qari:ReciterSelection,surah:Int)->TimedChapter? {
        guard let r=verseReciters.first(where:{$0.id==qari.verseId}),let folder=r.timingFolder,r.supportsWordSync==true,let t=try? decode("audio_timing/\(folder)/\(surah).json",TimedChapter.self),t.surah==surah,t.matches(r),t.hasReliableVerseRanges(ayahCount:corpus.surahs[surah-1].ayahCount),let u=URL(string:t.audioUrl),u.scheme=="https",["download.quranicaudio.com","audio-cdn.tarteel.ai","cdn.mp3quran.net"].contains(u.host ?? "") else {return nil}
        return t
    }
    func image(_ path:String)->UIImage? {if let image=imageCache.object(forKey:path as NSString){return image};guard let image=UIImage(contentsOfFile:root.appendingPathComponent(path).path) else{return nil};imageCache.setObject(image,forKey:path as NSString,cost:Int(image.size.width*image.size.height*image.scale*image.scale*4));return image}
    func portrait(_ qari:String)->UIImage? {portraits.first {$0.qariIds.contains(qari)}.flatMap {image($0.asset)}}
    func pageMetadata(_ edition:MushafEdition,_ page:Int)->MushafPage? {manifest.editions.first {$0.id==edition.id}?.pages.first {$0.page==page}}
}
struct HadithEntry:Identifiable {
    let id:Int,collection:String,number:String,section:String,arabic:String,indonesian:String,grades:String
    var title:String {(collection=="bukhari" ? "Shahih al-Bukhari":"Shahih Muslim")+" · No. "+number}
}
final class HadithStore {
    let url:URL
    init(content:ContentStore){url=content.root.appendingPathComponent("hadith/hadith.sqlite")}
    func search(_ query:String,collection:String?,offset:Int=0)throws->(rows:[HadithEntry],total:Int) {
        var db:OpaquePointer?;guard sqlite3_open_v2(url.path,&db,SQLITE_OPEN_READONLY|SQLITE_OPEN_FULLMUTEX,nil)==SQLITE_OK else {throw ContentError.invalidCorpus};defer {sqlite3_close(db)}
        let norm=Normalization.search(String(query.prefix(200))).trimmingCharacters(in:.whitespaces)
        let regex=try NSRegularExpression(pattern:"^(?:(?:hr[. ]*)?(bukhari|muslim)[ :#]*)?(\\d+(?:\\.\\d+)?)$")
        let ns=norm as NSString,m=regex.firstMatch(in:norm,range:NSRange(location:0,length:ns.length))
        var selected=collection,args=[String](),conditions=[String]();var from="hadith h"
        if let m=m,m.range(at:1).location != NSNotFound {selected=ns.substring(with:m.range(at:1))}
        if let selected=selected {conditions.append("h.collection = ?");args.append(selected)}
        if let m=m {conditions.append("h.number = ?");args.append(ns.substring(with:m.range(at:2)))}
        else {
            let tokens=Normalization.words(norm).filter {$0.count>=2}.prefix(10);if tokens.isEmpty {return ([],0)}
            from="hadith_search JOIN hadith h ON h.id = hadith_search.docid";conditions.append("hadith_search MATCH ?");args.append(tokens.map {"\"\($0)\"*"}.joined(separator:" "))
        }
        let whereClause=conditions.joined(separator:" AND ")
        func prepare(_ sql:String)throws->OpaquePointer {
            var st:OpaquePointer?;guard sqlite3_prepare_v2(db,sql,-1,&st,nil)==SQLITE_OK,let statement=st else {throw ContentError.invalidIndex}
            for (i,a) in args.enumerated() {a.withCString {p in _=sqlite3_bind_text(statement,Int32(i+1),p,-1,unsafeBitCast(-1,to:sqlite3_destructor_type.self))}}
            return statement
        }
        let count=try prepare("SELECT count(*) FROM \(from) WHERE \(whereClause)");let total=sqlite3_step(count)==SQLITE_ROW ? Int(sqlite3_column_int(count,0)):0;sqlite3_finalize(count)
        let st=try prepare("SELECT h.id,h.collection,h.number,h.section,h.arabic,h.indonesian,h.grades FROM \(from) WHERE \(whereClause) ORDER BY h.collection,h.sort_number LIMIT 40 OFFSET \(max(0,min(20000,offset)))");defer {sqlite3_finalize(st)}
        func str(_ n:Int32)->String {sqlite3_column_text(st,n).map {String(cString:$0)} ?? ""}
        var rows=[HadithEntry]();while sqlite3_step(st)==SQLITE_ROW {rows.append(HadithEntry(id:Int(sqlite3_column_int64(st,0)),collection:str(1),number:str(2),section:str(3),arabic:str(4),indonesian:str(5),grades:str(6)))}
        return (rows,total)
    }
}
