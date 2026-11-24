#if DEBUG
import Foundation

/// A real Alafasy recording is installed into the simulator data container by the
/// test script, never into the app bundle. Other UI tests start with a clean cache.
@MainActor enum UITestAudio {
    static func configure(content:ContentStore,storage:URL,downloads:DownloadStore) throws {
        let arguments=ProcessInfo.processInfo.arguments
        guard arguments.contains("--ui-testing") else {return}
        let fixture=storage.appendingPathComponent("uitest-fixtures/alafasy1.mp3")
        guard FileManager.default.fileExists(atPath:fixture.path),
              let qari=content.qaris.first(where:{$0.id=="ar.alafasy"}),
              let url=content.chapterReciters.first(where:{$0.id==qari.chapterId})?.url(1) else {return}
        let cache=downloads.directory.appendingPathComponent(downloads.key(url)+"."+url.pathExtension)
        if FileManager.default.fileExists(atPath:cache.path) {try FileManager.default.removeItem(at:cache)}
        if arguments.contains("--ui-testing-audio") {try FileManager.default.copyItem(at:fixture,to:cache)}
    }
}
#endif
