import Foundation

/// Text reading follows the selected surah or the entire juz, including surah boundaries.
public struct ReadingSequence {
    public let references:[Reference]
    public init(corpus:QuranCorpus,reference:Reference,juz:Int?=nil){
        if let juz=juz,(1...30).contains(juz),corpus.verse(reference)?.juz==juz {
            references=corpus.verses.filter {$0.juz==juz}.map(\.reference)
        }else {references=corpus.chapter(reference.surah).map(\.reference)}
    }
    public func neighbor(of reference:Reference,offset:Int)->Reference? {
        guard let position=references.firstIndex(of:reference),references.indices.contains(position+offset) else {return nil}
        return references[position+offset]
    }
}
