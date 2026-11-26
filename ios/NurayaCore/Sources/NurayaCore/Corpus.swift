import Foundation

public enum ContentError: Error { case invalidCorpus, invalidReference, invalidEdition, invalidIndex, invalidRange, invalidModel }
public struct Reference: Codable, Hashable, Identifiable, Comparable {
    public let surah: Int; public let ayah: Int
    public init(_ surah: Int, _ ayah: Int) { self.surah = surah; self.ayah = ayah }
    public var id: String { "\(surah):\(ayah)" }
    public static func < (l: Self, r: Self) -> Bool { l.surah == r.surah ? l.ayah < r.ayah : l.surah < r.surah }
}
public struct Surah: Codable, Identifiable {
    public let id: Int, name: String, arabicName: String, englishName: String, ayahCount: Int, revelation: String
}
public struct Verse: Codable, Identifiable {
    public let number: Int, surah: Int, ayah: Int, arabic: String, indonesian: String, english: String, juz: Int
    public let indonesianFootnotes: String?
    public var id: Int { number }
    public var reference: Reference { Reference(surah, ayah) }
    public var opening: String? { OpeningPolicy.split(reference, arabic).opening }
    public var text: String { OpeningPolicy.split(reference, arabic).verse }
}
public enum OpeningPolicy {
    public static let basmalah = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
    public static func split(_ ref: Reference, _ raw: String) -> (verse: String, opening: String?) {
        guard (2...114).contains(ref.surah), ref.surah != 9, ref.ayah == 1 else { return (raw, nil) }
        for prefix in [basmalah, "بِّسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"] where raw.hasPrefix(prefix + " ") {
            let body = String(raw.dropFirst(prefix.count + 1))
            if !body.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { return (body, prefix) }
        }
        return (raw, nil)
    }
}
public struct QuranCorpus: Decodable {
    public let schemaVersion: Int; public let surahs: [Surah]; public let verses: [Verse]
    public static func load(_ data: Data) throws -> Self {
        let c = try JSONDecoder().decode(Self.self, from: data)
        guard c.schemaVersion == 1, c.surahs.count == 114, c.verses.count == 6236,
              c.surahs.map(\.id) == Array(1...114), c.verses.map(\.number) == Array(1...6236) else { throw ContentError.invalidCorpus }
        var number = 0
        for surah in c.surahs {
            guard surah.ayahCount > 0 else { throw ContentError.invalidCorpus }
            for ayah in 1...surah.ayahCount {
                guard number < c.verses.count, c.verses[number].surah == surah.id, c.verses[number].ayah == ayah else { throw ContentError.invalidCorpus }; number += 1
            }
        }
        guard number == 6236 else { throw ContentError.invalidCorpus }; return c
    }
    public func verse(_ r: Reference) -> Verse? {
        guard (1...114).contains(r.surah), (1...surahs[r.surah - 1].ayahCount).contains(r.ayah) else { return nil }
        let start = surahs.prefix(r.surah - 1).reduce(0) { $0 + $1.ayahCount }
        return verses[start + r.ayah - 1]
    }
    public func chapter(_ n: Int) -> [Verse] { verses.filter { $0.surah == n } }
    public func range(_ start: Reference, _ end: Reference, each: Int, passes: Int) throws -> [Verse] {
        guard let a = verse(start), let b = verse(end), a.number <= b.number,
              (1...20).contains(each), (1...20).contains(passes), each * passes <= 20,
              (b.number-a.number+1)*each*passes <= 12500 else { throw ContentError.invalidRange }
        let pass = verses[(a.number-1)..<b.number].flatMap { Array(repeating: $0, count: each) }
        return (0..<passes).flatMap { _ in pass }
    }
}
public enum Normalization {
    public static func search(_ s: String) -> String {
        String(s.decomposedStringWithCanonicalMapping.unicodeScalars.filter { !CharacterSet.nonBaseCharacters.contains($0) })
            .replacingOccurrences(of: "ـ", with: "").replacingOccurrences(of: "ٱ", with: "ا").lowercased()
    }
    public static func words(_ s: String) -> [String] {
        search(s).components(separatedBy: CharacterSet.alphanumerics.inverted).filter { !$0.isEmpty }
    }
    public static func spoken(_ s: String) -> String {
        var words = words(s)
        let ones = ["nol":0,"satu":1,"dua":2,"tiga":3,"empat":4,"lima":5,"enam":6,"tujuh":7,"delapan":8,"sembilan":9,"sepuluh":10,"sebelas":11]
        var out = [String](); var i = 0
        while i < words.count {
            let w = words[i]
            if let initial = ones[w] ?? (w == "seratus" ? 100 : nil) {
                var value = initial; i += 1
                while i < words.count {
                    if words[i] == "ratus" { value *= 100 }
                    else if words[i] == "puluh" { let unit = value % 10; value += unit * 9 }
                    else if words[i] == "belas" { value += 10 }
                    else if let n = ones[words[i]] { value += n }
                    else { break }; i += 1
                }; out.append(String(value))
            } else { out.append(w); i += 1 }
        }
        return out.joined(separator: " ")
    }
}
public struct StudySpan: Decodable { public let start: Int, end: Int, rule: String }
public struct StudyWord: Decodable, Identifiable {
    public let position: Int, arabic: String; public let indonesian: String?, english: String?; public let start: Int?, end: Int?
    public var id: Int { position }
}
public struct StudyVerse: Decodable {
    public let ayah: Int, arabic: String; public let tajwidSpans: [StudySpan], words: [StudyWord]
    public let tafsir: String?, tafsirSource: String?, tafsirLanguage: String?
}
public struct StudyChapter: Decodable { public let surah: Int, verses: [StudyVerse] }
public struct LatinVerse: Decodable { public let ayah: Int, latin: String }
public struct LatinChapter: Decodable { public let verses: [LatinVerse] }
public struct ChapterReciter: Decodable, Identifiable {
    public let id: String, name: String, description: String, sourceUrl: String, baseUrl: String; public let numberPadding: Int?
    public func url(_ surah: Int) -> URL? {
        guard (1...114).contains(surah), baseUrl.hasPrefix("https://") else { return nil }
        return URL(string: baseUrl.trimmingCharacters(in: CharacterSet(charactersIn: "/")) + "/" + String(format: "%0*d", numberPadding ?? 0, surah) + ".mp3")
    }
}
public struct ReciterSelection: Decodable, Identifiable {
    public let chapterId: String, verseId: String, name: String
    public var id: String { chapterId }
}
public struct VerseReciter: Decodable, Identifiable {
    public let id: String, name: String, bitrateKbps: Int; public let supportsWordSync: Bool?; public let timingFolder: String?, ayahBaseUrl: String?
    public func url(_ verse: Verse) -> URL? {
        if let base = ayahBaseUrl, base.hasPrefix("https://everyayah.com/data/") { return URL(string: base.trimmingCharacters(in: CharacterSet(charactersIn: "/")) + String(format: "/%03d%03d.mp3", verse.surah, verse.ayah)) }
        return URL(string: "https://cdn.islamic.network/quran/audio/\(bitrateKbps)/\(id)/\(verse.number).mp3")
    }
}
public struct VerseTiming: Decodable {
    public let ayah: Int, clipFrom: Double, clipTo: Double, segments: [[Double]], usable: Bool
    public var start: Double { clipFrom/1000 }
    public var end: Double { clipTo/1000 }
    public func word(at seconds: Double) -> Int? {
        guard usable != false else { return nil }
        return segments.first { $0.count == 3 && seconds*1000 >= $0[1] && seconds*1000 < $0[2] }.map { Int($0[0]) }
    }
}
public struct TimedChapter: Decodable { public let surah: Int, audioUrl: String, verses: [VerseTiming]; public let reciterId: String?, sourceSlug: String?, recitationId: Int?
    /// Reject incomplete/backward boundaries rather than seeking into another ayah
    /// or treating a placeholder (for example 0...1 ms) as an actual recitation.
    public func hasReliableVerseRanges(ayahCount:Int)->Bool {
        guard verses.map(\.ayah)==Array(1...ayahCount) else {return false}
        for (index,verse) in verses.enumerated() {
            guard verse.start.isFinite,verse.end.isFinite,verse.start>=0,verse.end>verse.start else {return false}
            if index>0 {
                let previous=verses[index-1]
                guard verse.start>=previous.start,verse.end>previous.end else {return false}
                // The source has already rejected the word alignment. Do not use
                // its overlapping clip edges to start/end a requested ayah range.
                if verse.start<previous.end && (!verse.usable || !previous.usable) {return false}
            }
        }
        return true
    }
    public func matches(_ reciter:VerseReciter)->Bool {
        if reciter.id=="ar.alafasy" {return recitationId==7 && audioUrl=="https://download.quranicaudio.com/qdc/mishari_al_afasy/murattal/\(surah).mp3"}
        return reciterId==reciter.id && sourceSlug==reciter.timingFolder
    } }
public struct Portrait: Decodable, Identifiable { public let id: String, name: String, qariIds: [String], asset: String, sourceName: String, sourcePage: String }
