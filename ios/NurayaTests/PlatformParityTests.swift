import XCTest
import NurayaCore
@testable import Nuraya

final class PlatformParityTests:XCTestCase {
    func content() throws -> ContentStore {
        try ContentStore(root:try XCTUnwrap(Bundle.main.resourceURL).appendingPathComponent("Content"))
    }
    func testPackagedSourcesOpenAndMatchAllCuratedVoices() throws {
        let store=try content()
        XCTAssertEqual(store.corpus.verses.count,6236)
        XCTAssertEqual(store.modes.count,11)
        for qari in store.qaris {
            XCTAssertNotNil(store.portrait(qari.id),qari.name)
            XCTAssertNotNil(store.chapterReciters.first {$0.id==qari.chapterId})
            XCTAssertNotNil(store.verseReciters.first {$0.id==qari.verseId})
        }
        XCTAssertEqual(store.qaris.filter {store.timing($0,surah:112) != nil}.count,11)
        for verse in store.corpus.verses {
            XCTAssertNotNil(store.study(verse.reference),verse.reference.id)
            XCTAssertNotNil(store.latin(verse.reference),verse.reference.id)
        }
        let hadith=try HadithStore(content:store).search("Bukhari 1",collection:nil)
        XCTAssertFalse(hadith.rows.isEmpty)
        XCTAssertTrue(hadith.rows.allSatisfy {$0.collection=="bukhari" && $0.number=="1"})
    }
    func testPrayerOrderAndTangerangDirection() throws {
        var settings=AppSettings();settings.latitude = -6.1783;settings.longitude=106.6319;settings.timeZone="Asia/Jakarta"
        let date=try XCTUnwrap(ISO8601DateFormatter().date(from:"2026-11-17T05:00:00Z"))
        let prayers=PrayerService.day(date,settings:settings)
        XCTAssertEqual(prayers.map(\.id),["fajr","sunrise","dhuhr","asr","maghrib","isha"])
        XCTAssertEqual(prayers.map(\.date),prayers.map(\.date).sorted())
        XCTAssertEqual(try XCTUnwrap(PrayerService.bearing(settings:settings)),295.2,accuracy:1)
        XCTAssertTrue(PrayerService.hijri(date,settings:settings).contains("1448"))
    }
    func testNativeSpeechSilenceCancellationAndKnownQuranRecording() throws {
        let bundle=Bundle(for:Self.self)
        let fixture=try XCTUnwrap(bundle.resourceURL).appendingPathComponent("TestFixtures")
        let engine=try NurayaWhisper(modelPath:fixture.appendingPathComponent("model.bin").path)
        let silence=Data(count:16000*MemoryLayout<Float>.size)
        XCTAssertEqual(try engine.transcribePCM(silence,generation:engine.generation()),"")
        let stale=engine.generation();engine.cancel()
        XCTAssertThrowsError(try engine.transcribePCM(silence,generation:stale))
        let pcm=try Data(contentsOf:fixture.appendingPathComponent("quran.pcm"))
        let transcript=try engine.transcribePCM(pcm,generation:engine.generation())
        XCTAssertFalse(transcript.trimmingCharacters(in:.whitespacesAndNewlines).isEmpty)
        XCTAssertTrue(transcript.unicodeScalars.contains {$0.value>=0x0600 && $0.value<=0x06FF})
        let comparison=RecitationMatcher.compare(expected:"بسم الله الرحمن الرحيم الحمد لله رب العالمين",recognized:transcript,final:true)
        XCTAssertGreaterThanOrEqual(comparison.matched,4)
    }
}
