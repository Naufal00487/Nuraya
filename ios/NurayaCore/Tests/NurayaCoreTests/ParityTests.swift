import XCTest
@testable import NurayaCore
final class ParityTests: XCTestCase {
    var assets: URL { URL(fileURLWithPath:ProcessInfo.processInfo.environment["NURAYA_CONTENT"] ?? "../Nuraya/Resources/Content",isDirectory:true) }
    func corpus() throws -> QuranCorpus { try QuranCorpus.load(Data(contentsOf:assets.appendingPathComponent("quran.json"))) }
    func testCanonicalReferencesAndOpeningPolicyForEverySurah() throws {
        let c=try corpus();var separated=0
        for s in c.surahs {
            let v=try XCTUnwrap(c.verse(Reference(s.id,1)))
            if s.id==1 || s.id==9 {XCTAssertNil(v.opening)}
            else {XCTAssertNotNil(v.opening); XCTAssertEqual(v.opening!+" "+v.text,v.arabic);separated+=1}
            XCTAssertNotNil(c.verse(Reference(s.id,s.ayahCount)))
            XCTAssertNil(c.verse(Reference(s.id,s.ayahCount+1)))
        }
        XCTAssertEqual(separated,112)
        let naml=try XCTUnwrap(c.verse(Reference(27,30)));XCTAssertEqual(naml.text,naml.arabic)
    }
    func testAll6236ReferencesInBothPageIndexes() throws {
        let c=try corpus()
        for name in ["indonesia","madinah"] {
            let index=try JSONDecoder().decode(PageIndex.self,from:Data(contentsOf:assets.appendingPathComponent("printed_mushaf/\(name)-index.json")))
            try index.validate(c)
            for s in c.surahs {let first=try index.page(for:Reference(s.id,1));XCTAssertTrue((1...604).contains(first));if s.id>2 {XCTAssertGreaterThan(first,3)}}
            XCTAssertThrowsError(try index.page(for:Reference(115,1)))
        }
    }
    func testRepeatedQueueCrossesSurahWithoutAddingBasmalah() throws {
        let c=try corpus();let a=Reference(112,4),b=Reference(113,2)
        let queue=try c.range(a,b,each:2,passes:2)
        XCTAssertEqual(queue.map { $0.reference.id },["112:4","112:4","113:1","113:1","113:2","113:2","112:4","112:4","113:1","113:1","113:2","113:2"])
        XCTAssertThrowsError(try c.range(b,a,each:1,passes:1))
        XCTAssertThrowsError(try c.range(Reference(1,1),Reference(114,6),each:20,passes:1))
    }
    func testRecognitionPartialTailIsNotMarkedMissing() {
        let c=RecitationMatcher.compare(expected:"بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",recognized:"بسم الله",final:false)
        XCTAssertEqual(c.words.map(\.state),[.matched,.matched,.pending,.pending])
        let f=RecitationMatcher.compare(expected:"بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",recognized:"بسم الله",final:true)
        XCTAssertEqual(f.words.map(\.state),[.matched,.matched,.notHeard,.notHeard])
        XCTAssertTrue(RecitationMatcher.spellings("ٱلصَّلَوٰةَ").contains("الصلاة"))
        XCTAssertFalse(RecitationMatcher.spellings("وَٰحِد").contains("احد"))
    }
    func testSpokenIndonesianNumbers() {
        XCTAssertEqual(Normalization.spoken("cari surat Al Baqarah ayat dua ratus lima puluh lima"),"cari surat al baqarah ayat 255")
        XCTAssertEqual(Normalization.spoken("surat sebelas ayat tiga"),"surat 11 ayat 3")
    }
    func testCatalogHasSame28ExplicitPairs() throws {
        let rows=try JSONDecoder().decode([ReciterSelection].self,from:Data(contentsOf:assets.appendingPathComponent("curated_reciters.json")))
        XCTAssertEqual(rows.count,28);XCTAssertEqual(Set(rows.map(\.id)).count,28)
        XCTAssertEqual(rows.first { $0.chapterId == "ar.abdulbasitmurattal" }?.verseId,"ar.abdulsamad")
        XCTAssertFalse(rows.contains { $0.name.contains("Matrood") || $0.name.contains("Jaber") })
    }
}
