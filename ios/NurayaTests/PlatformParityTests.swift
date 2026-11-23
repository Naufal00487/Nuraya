import XCTest
import NurayaCore
@testable import Nuraya

final class PlatformParityTests:XCTestCase {
    func content() throws -> ContentStore {
        try ContentStore(root:try XCTUnwrap(Bundle.main.resourceURL).appendingPathComponent("Content"))
    }
    @MainActor func testReadingModeAndBookmarksSurviveUpgradeAndRelaunch() throws {
        let directory=FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer {try? FileManager.default.removeItem(at:directory)}
        let store=try content()
        let state=try AppState(content:store,storageDirectory:directory)
        state.settings.name="Penguji"
        state.settings.dark=true
        state.open(Reference(114,1),mode:"madinah")
        state.mark(Reference(114,1))
        state.reader=nil
        let reloaded=try AppState(content:store,storageDirectory:directory)
        reloaded.resumeReading()
        XCTAssertEqual(reloaded.reader?.reference,Reference(114,1))
        XCTAssertEqual(reloaded.reader?.mode,"madinah")
        XCTAssertEqual(reloaded.settings.entries.first(where:{$0.kind=="bookmark"})?.reading?.mode,"madinah")
        reloaded.open(Reference(114,1))
        XCTAssertEqual(reloaded.reader?.mode,"madinah")
        reloaded.open(Reference(78,1),mode:"standard",juz:30)
        let restored=try AppState(content:store,storageDirectory:directory)
        restored.resumeReading()
        XCTAssertEqual(restored.reader?.juz,30)

        // Build 1 has neither lastReading nor SavedEntry.reading. Preserve its data.
        var legacy=try XCTUnwrap(JSONSerialization.jsonObject(with:JSONEncoder().encode(state.settings)) as? [String:Any])
        legacy.removeValue(forKey:"lastReading")
        var entries=try XCTUnwrap(legacy["entries"] as? [[String:Any]])
        for i in entries.indices {entries[i].removeValue(forKey:"reading")}
        legacy["entries"]=entries
        try JSONSerialization.data(withJSONObject:legacy).write(to:directory.appendingPathComponent("settings.json"))
        let upgraded=try AppState(content:store,storageDirectory:directory)
        XCTAssertEqual(upgraded.settings.name,"Penguji")
        XCTAssertTrue(upgraded.settings.dark)
        XCTAssertEqual(upgraded.settings.entries.filter {$0.kind=="bookmark"}.count,1)
        upgraded.resumeReading()
        XCTAssertEqual(upgraded.reader?.reference,Reference(114,1))
        XCTAssertEqual(upgraded.reader?.mode,"indonesia")
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
    @MainActor func testReadingAudioHasFiniteRepeatsAndStopsWithoutStoppingMurottal() throws {
        let store=try content()
        let directory=FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer {try? FileManager.default.removeItem(at:directory)}
        let downloads=try DownloadStore(directory:directory,content:store)
        let audio=AudioService(content:store,downloads:downloads)
        let qari=try XCTUnwrap(store.qaris.first)
        audio.chapter(qari,112)
        let chapter=try XCTUnwrap(audio.track?.id)
        audio.stopReader()
        XCTAssertEqual(audio.track?.id,chapter)
        audio.repeatMode=1
        audio.verses(qari:qari,start:Reference(112,1),end:Reference(112,2))
        XCTAssertEqual(audio.track?.verse?.reference,Reference(112,1))
        audio.next()
        XCTAssertEqual(audio.track?.verse?.reference,Reference(112,2))
        audio.next()
        XCTAssertNil(audio.track)
        audio.verses(qari:qari,start:Reference(112,1),end:Reference(112,1))
        audio.stopReader()
        XCTAssertNil(audio.track)
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
