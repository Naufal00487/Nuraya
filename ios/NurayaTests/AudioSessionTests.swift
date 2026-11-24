import XCTest
import AVFoundation
import NurayaCore
@testable import Nuraya

@MainActor private final class RecordingSessionDriver:AudioSessionDriver {
    var activations=[AVAudioSession.Category]()
    var deactivations=0
    func activate(category:AVAudioSession.Category,mode:AVAudioSession.Mode,options:AVAudioSession.CategoryOptions) throws {activations.append(category)}
    func deactivate() throws {deactivations+=1}
}

final class AudioSessionTests:XCTestCase {
    @MainActor func testBackgroundAndIdleMicrophoneCleanupLeaveMurottalSessionActive() throws {
        let content=try ContentStore(root:try XCTUnwrap(Bundle.main.resourceURL).appendingPathComponent("Content"))
        let directory=FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer {try? FileManager.default.removeItem(at:directory)}
        let driver=RecordingSessionDriver()
        let session=AudioSessionController(driver:driver)
        let state=try AppState(content:content,storageDirectory:directory,audioSession:session)
        defer {state.audio.stop()}
        let local=QuranRecitationService(state:state)
        state.audio.chapter(state.selectedQari,1)
        let track=try XCTUnwrap(state.audio.track?.id)
        XCTAssertEqual(driver.activations.last,.playback)
        state.enterBackground() // The actual RootView background lifecycle action.
        state.speech.clearResult()
        state.speech.finish()
        state.speech.stop()
        local.cancel()
        local.clearResult()
        XCTAssertEqual(state.audio.track?.id,track)
        XCTAssertEqual(driver.deactivations,0,"An idle microphone must not deactivate murottal's session.")
        state.audio.stop()
        XCTAssertEqual(driver.deactivations,1)
        state.enterBackground()
        XCTAssertEqual(driver.deactivations,1,"Repeated cleanup must be harmless.")
    }

    @MainActor func testObsoleteCaptureCleanupCannotReleaseNewPlaybackSession() throws {
        let driver=RecordingSessionDriver()
        // Use an isolated driver so no system audio or microphone permission is needed.
        let controller=AudioSessionController(driver:driver)
        let oldCapture=try controller.recording()
        let playback=try controller.playback()
        controller.release(oldCapture)
        controller.release(nil)
        XCTAssertEqual(driver.deactivations,0)
        controller.release(playback)
        controller.release(playback)
        XCTAssertEqual(driver.deactivations,1)
    }

    @MainActor func testQuranReaderStillStopsWhenAppEntersBackground() throws {
        let content=try ContentStore(root:try XCTUnwrap(Bundle.main.resourceURL).appendingPathComponent("Content"))
        let directory=FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer {try? FileManager.default.removeItem(at:directory)}
        let driver=RecordingSessionDriver()
        let state=try AppState(content:content,storageDirectory:directory,audioSession:AudioSessionController(driver:driver))
        state.audio.verses(qari:state.selectedQari,start:Reference(1,1),end:Reference(1,2))
        XCTAssertNotNil(state.audio.track)
        state.enterBackground()
        XCTAssertNil(state.audio.track)
        XCTAssertEqual(driver.deactivations,1)
    }
}
