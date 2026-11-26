import XCTest
import AVFoundation
import NurayaCore
@testable import Nuraya

final class ReaderAudioTests:XCTestCase {
    private struct Fixture:Decodable {let file:String,url:URL}
    private func content()throws->ContentStore {
        try ContentStore(root:try XCTUnwrap(Bundle.main.resourceURL).appendingPathComponent("Content"))
    }
    @MainActor private func harness()throws->(AudioService,URL) {
        let store=try content(),directory=FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        let downloads=try DownloadStore(directory:directory,content:store)
        let fixtures=try XCTUnwrap(Bundle(for:Self.self).resourceURL).appendingPathComponent("TestFixtures")
        let files=try JSONDecoder().decode([Fixture].self,from:Data(contentsOf:fixtures.appendingPathComponent("reader-audio.json")))
        for file in files {
            let target=directory.appendingPathComponent(downloads.key(file.url)+"."+file.url.pathExtension)
            try FileManager.default.copyItem(at:fixtures.appendingPathComponent(file.file),to:target)
        }
        return (AudioService(content:store,downloads:downloads),directory)
    }
    @MainActor private func waitUntil(_ description:String,timeout:Double=12,condition:()->Bool) async throws {
        let deadline=Date().addingTimeInterval(timeout)
        while !condition(),Date()<deadline {try await Task.sleep(nanoseconds:20_000_000)}
        guard condition() else {XCTFail(description);throw NSError(domain:"ReaderAudioTests",code:1)}
    }
    func testEveryTimedChapterIsOneContinuousRunAndInvalidBoundariesFallBack() throws {
        let store=try content()
        let rejected=Set([3,4,6,13,14,20,23,26,27,29,33,37,59,60,66,70,84,93,98,101,113].map {"ar.alafasy/\($0)"}+["ar.ahmedalajmi/26","ar.haniarrifai/6"])
        var checked=0
        for qari in store.qaris {
            let reciter=try XCTUnwrap(store.verseReciters.first {$0.id==qari.verseId})
            guard reciter.timingFolder != nil else {continue}
            for surah in store.corpus.surahs {
                let tracks=try AudioTrack.reading(content:store,qari:qari,start:Reference(surah.id,1),end:Reference(surah.id,surah.ayahCount),each:1,passes:1)
                XCTAssertEqual(tracks.count,surah.ayahCount)
                let key="\(qari.id)/\(surah.id)"
                if rejected.contains(key) {
                    XCTAssertTrue(tracks.allSatisfy {$0.timing==nil},key)
                    for t in tracks {XCTAssertEqual(t.url,reciter.url(try XCTUnwrap(t.verse)),key)}
                }else {
                    XCTAssertEqual(AudioTrack.run(in:tracks,from:0),0..<surah.ayahCount,key)
                }
                checked+=1
            }
        }
        XCTAssertEqual(checked,1254)
    }
    func testRepeatsPassesAndSurahChangesRemainExplicitBoundaries() throws {
        let store=try content(),qari=try XCTUnwrap(store.qaris.first {$0.id=="ar.alafasy"})
        let tracks=try AudioTrack.reading(content:store,qari:qari,start:Reference(112,1),end:Reference(112,3),each:2,passes:2)
        XCTAssertEqual(tracks.compactMap {$0.verse?.ayah},[1,1,2,2,3,3,1,1,2,2,3,3])
        var runs=[[Int]](),index=0
        while index<tracks.count {
            let run=AudioTrack.run(in:tracks,from:index)
            runs.append(tracks[run].compactMap {$0.verse?.ayah});index=run.upperBound
        }
        XCTAssertEqual(runs,[[1],[1,2],[2,3],[3],[1],[1,2],[2,3],[3]])
        let crossing=try AudioTrack.reading(content:store,qari:qari,start:Reference(112,4),end:Reference(113,2),each:1,passes:1)
        XCTAssertEqual(AudioTrack.run(in:crossing,from:0),0..<1)
        XCTAssertEqual(AudioTrack.run(in:crossing,from:1),1..<3)
    }
    @MainActor func testActualSurahKeepsSamePlayerItemAndClockAcrossAyahs() async throws {
        let (audio,directory)=try harness()
        defer {audio.stop();try? FileManager.default.removeItem(at:directory)}
        let qari=try XCTUnwrap(audio.content.qaris.first {$0.id=="ar.alafasy"})
        audio.setSpeed(2)
        audio.verses(qari:qari,start:Reference(1,1),end:Reference(1,3))
        try await waitUntil("The cached reader recording must start") {audio.playing && audio.player.currentTime().seconds>0.1}
        let original=try XCTUnwrap(audio.player.currentItem)
        XCTAssertEqual(original.forwardPlaybackEndTime.seconds,16.245,accuracy:0.001)
        var jumps=0,stops=0
        let jumpToken=NotificationCenter.default.addObserver(forName:.AVPlayerItemTimeJumped,object:original,queue:.main){_ in jumps+=1}
        let rateToken=audio.player.observe(\.rate,options:[.new]) {player,_ in if player.rate==0 {stops+=1}}
        defer {NotificationCenter.default.removeObserver(jumpToken);rateToken.invalidate()}
        var previous=audio.player.currentTime().seconds
        let deadline=Date().addingTimeInterval(10)
        while audio.track?.verse?.ayah != 3,Date()<deadline {
            try await Task.sleep(nanoseconds:30_000_000)
            XCTAssertTrue(audio.player.currentItem === original,"An ayah boundary must not recreate the player item.")
            let now=audio.player.currentTime().seconds
            XCTAssertGreaterThanOrEqual(now,previous-0.02,"Do not seek backwards or replay the end of an ayah.")
            previous=now
        }
        XCTAssertEqual(audio.track?.verse?.reference,Reference(1,3))
        XCTAssertGreaterThanOrEqual(audio.player.currentTime().seconds,11.615)
        XCTAssertEqual(jumps,0,"Consecutive ayahs must not seek.")
        XCTAssertEqual(stops,0,"Consecutive ayahs must not pause the player.")
        rateToken.invalidate();NotificationCenter.default.removeObserver(jumpToken)
        try await waitUntil("Stop exactly at the selected final ayah") {audio.track==nil}
    }
    @MainActor func testVerseFilesAreQueuedBeforeEndAndReusePreparedItems() async throws {
        let (audio,directory)=try harness()
        defer {audio.stop();try? FileManager.default.removeItem(at:directory)}
        let qari=try XCTUnwrap(audio.content.qaris.first {$0.id=="ar.muhammadjibreel"})
        audio.setSpeed(1)
        audio.verses(qari:qari,start:Reference(112,1),end:Reference(112,3))
        let prepared=audio.player.items()
        XCTAssertEqual(prepared.count,3,"Prepare the next two files before finishing the first ayah.")
        guard prepared.count==3 else {return}
        try await waitUntil("Start the first cached verse") {audio.player.timeControlStatus == .playing && audio.position>0.1}
        var stalls=0
        let bufferToken=audio.player.observe(\.timeControlStatus,options:[.new]) {player,_ in
            if player.timeControlStatus == .waitingToPlayAtSpecifiedRate {stalls+=1}
        }
        defer {bufferToken.invalidate()}
        try await waitUntil("Reach the second verse through the native queue") {audio.track?.verse?.ayah==2 && audio.playing}
        XCTAssertTrue(audio.player.currentItem === prepared[1],"Must use the prepared item, not reload the next URL.")
        try await waitUntil("Reach the third verse through the native queue") {audio.track?.verse?.ayah==3 && audio.playing}
        XCTAssertTrue(audio.player.currentItem === prepared[2])
        XCTAssertEqual(stalls,0,"Cached verse transitions must not wait for a fresh item to load.")
        bufferToken.invalidate()
        XCTAssertNil(audio.error)
        try await waitUntil("The finite range must finish") {audio.track==nil}
    }
    @MainActor func testMidSurahStartAndPauseWhilePreparingAreRespected() async throws {
        let (audio,directory)=try harness()
        defer {audio.stop();try? FileManager.default.removeItem(at:directory)}
        let qari=try XCTUnwrap(audio.content.qaris.first {$0.id=="ar.alafasy"})
        audio.verses(qari:qari,start:Reference(1,2),end:Reference(1,3))
        audio.pause()
        try await waitUntil("Prepare the requested start without autoplaying") {audio.player.currentTime().seconds>=6.02}
        try await Task.sleep(nanoseconds:300_000_000)
        XCTAssertFalse(audio.playing)
        XCTAssertEqual(audio.player.rate,0)
        XCTAssertEqual(audio.player.currentTime().seconds,6.025,accuracy:0.025)
        XCTAssertEqual(audio.track?.verse?.reference,Reference(1,2))
        audio.resume()
        try await waitUntil("Resume the selected verse") {audio.player.currentTime().seconds>6.4}
        audio.stopReader()
        XCTAssertNil(audio.track)
        XCTAssertTrue(audio.player.items().isEmpty)
    }
}
