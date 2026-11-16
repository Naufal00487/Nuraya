import Foundation
public enum MushafEdition: String, CaseIterable, Codable, Identifiable {
    case indonesia = "id", indonesiaTajwid = "id-tajwid", madinah = "md", madinahTajwid = "md-tajwid", word = "kata-tajwid", latin = "latin-tajwid", hafalan = "hafalan"
    public var id: String { rawValue }
    public var index: String { self == .madinah || self == .madinahTajwid ? "madinah" : "indonesia" }
    public var mapping: String? { switch self { case .hafalan: return nil; case .madinah,.madinahTajwid: return "md"; case .word,.latin: return "kata-tajwid"; default: return "id" } }
    public var height: Double { self == .word || self == .latin ? 2202 : 1745 }
    public var title: String { switch self { case .indonesia: return "Al-Qur’an Indonesia";case .indonesiaTajwid:return "Indonesia & tajwid";case .madinah:return "Al-Qur’an Madinah";case .madinahTajwid:return "Madinah & tajwid";case .word:return "Per kata & tajwid";case .latin:return "Latin & tajwid";case .hafalan:return "Hafalan 8 blok" } }
}
public struct ReadingMode: Decodable, Identifiable { public let id: String, titleId: String, titleEn: String, descriptionId: String, descriptionEn: String
    public var edition: MushafEdition? { switch id { case "indonesia":return .indonesia;case "indonesia_tajwid","tikrar","waqaf":return .indonesiaTajwid;case "madinah":return .madinah;case "madinah_tajwid":return .madinahTajwid;case "word_tajwid":return .word;case "latin_tajwid":return .latin;case "hafalan_8":return .hafalan;default:return nil } }
}
public struct MushafPage: Decodable { public let page: Int, url: String, bytes: Int, sha256: String }
public struct MushafManifest: Decodable { public let commit: String, editions: [MushafEditionManifest] }
public struct MushafEditionManifest: Decodable { public let id: String, index: String, pages: [MushafPage] }
public struct PageReference: Decodable { public let surah: Int, ayah: Int, page: Int, juz: Int
    public var reference: Reference { Reference(surah, ayah) }
}
public struct PageIndex: Decodable {
    public let verses: [PageReference]
    public func page(for ref: Reference) throws -> Int { guard let p=verses.first(where: { $0.reference == ref })?.page else { throw ContentError.invalidReference };return p }
    public func references(on page: Int) -> [Reference] { verses.filter { $0.page == page }.map(\.reference) }
    public func start(juz: Int) -> Reference? { verses.first { $0.juz == juz }?.reference }
    public func validate(_ corpus: QuranCorpus) throws {
        guard verses.count == 6236, zip(verses,corpus.verses).allSatisfy({ $0.reference == $1.reference && (1...604).contains($0.page) }), Set(verses.map(\.page)).count == 604 else { throw ContentError.invalidIndex }
    }
}
public struct MushafRegion: Decodable { public let surah: Int, ayah: Int, rects: [[Double]]
    public var reference: Reference { Reference(surah,ayah) }
    public func contains(x: Double,y: Double) -> Bool { rects.contains { $0.count == 4 && x >= $0[0] && x <= $0[2] && y >= $0[1] && y <= $0[3] } }
}
public struct MushafMapPage: Decodable { public let page: Int, verses: [MushafRegion] }
public struct MushafMap: Decodable { public let width: Double, height: Double, pages: [MushafMapPage] }
public struct QuranTheme: Decodable, Identifiable { public let id: String, title: String, references: [[Int]] }
public struct WaqafItem: Decodable, Identifiable { public let mark: String,name: String,explanation: String;public var id: String {mark} }
