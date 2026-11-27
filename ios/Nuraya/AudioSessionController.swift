import AVFoundation

@MainActor protocol AudioSessionDriver {
    func activate(category:AVAudioSession.Category,mode:AVAudioSession.Mode,options:AVAudioSession.CategoryOptions) throws
    func deactivate() throws
}

@MainActor private struct SystemAudioSessionDriver:AudioSessionDriver {
    func activate(category:AVAudioSession.Category,mode:AVAudioSession.Mode,options:AVAudioSession.CategoryOptions) throws {
        let session=AVAudioSession.sharedInstance()
        try session.setCategory(category,mode:mode,options:options)
        try session.setActive(true)
    }
    func deactivate() throws {
        try AVAudioSession.sharedInstance().setActive(false,options:.notifyOthersOnDeactivation)
    }
}

/// AVAudioSession is shared by playback and both microphones. Cleanup may only
/// deactivate the session that the caller actually acquired, never a later owner.
@MainActor final class AudioSessionController {
    static let shared=AudioSessionController()
    private let driver:AudioSessionDriver
    private var owner:UUID?
    init(driver:AudioSessionDriver?=nil){self.driver=driver ?? SystemAudioSessionDriver()}
    func playback() throws -> UUID {
        try acquire(category:.playback,mode:.default,options:[])
    }
    func recording(duckOthers:Bool=false) throws -> UUID {
        try acquire(category:.record,mode:.measurement,options:duckOthers ? .duckOthers:[])
    }
    private func acquire(category:AVAudioSession.Category,mode:AVAudioSession.Mode,options:AVAudioSession.CategoryOptions) throws -> UUID {
        try driver.activate(category:category,mode:mode,options:options)
        let lease=UUID();owner=lease;return lease
    }
    func release(_ lease:UUID?) {
        guard let lease=lease,owner==lease else {return}
        owner=nil
        try? driver.deactivate()
    }
}
