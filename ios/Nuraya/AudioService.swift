import Foundation
import AVFoundation
import MediaPlayer
import NurayaCore
@MainActor final class AudioService:ObservableObject {
    enum Context {case murottal,reader}
    let player=AVQueuePlayer(),content:ContentStore,downloads:DownloadStore
    @Published var track:AudioTrack?;@Published var playing=false;@Published var position=0.0;@Published var duration=0.0;@Published var speed:Float=1;@Published var repeatMode=0;@Published var error:String?;@Published var word:Int?
    var onTrack:((AudioTrack)->Void)?
    private(set) var context:Context = .murottal
    private let audioSession:AudioSessionController
    private var sessionLease:UUID?
    private var queue=[AudioTrack](),cursor=0,generation=0,advancing=false
    private var wantsPlayback=false,readyHandled=false
    private var activeItem:AVPlayerItem?,activeRun=0..<0
    private var itemRuns=[ObjectIdentifier:Range<Int>]()
    private var currentItemObservation:NSKeyValueObservation?
    private var artwork:MPMediaItemArtwork?,artworkQari="",lastNowPlaying=Date.distantPast
    private var tick:Any?,endToken:NSObjectProtocol?,interruptToken:NSObjectProtocol?,routeToken:NSObjectProtocol?,status:NSKeyValueObservation?
    init(content:ContentStore,downloads:DownloadStore,audioSession:AudioSessionController?=nil) {
        self.content=content;self.downloads=downloads;self.audioSession=audioSession ?? .shared
        player.actionAtItemEnd = .advance
        currentItemObservation=player.observe(\.currentItem,options:[.new]) {[weak self] player,_ in
            let item=player.currentItem
            Task {@MainActor in
                guard let self=self,let item=item,item === self.player.currentItem,
                      item !== self.activeItem,let run=self.itemRuns[ObjectIdentifier(item)] else {return}
                self.adopt(item,run:run,seekToStart:false)
                self.enqueueUpcoming()
            }
        }
        tick=player.addPeriodicTimeObserver(forInterval:CMTime(seconds:0.06,preferredTimescale:600),queue:.main) {[weak self] _ in Task {@MainActor in
            guard let self=self else {return}
            // A queued callback can outlive its item; always read the current clock.
            self.update(self.player.currentTime().seconds)
        }}
        endToken=NotificationCenter.default.addObserver(forName:.AVPlayerItemDidPlayToEndTime,object:nil,queue:.main){[weak self] n in
            guard let item=n.object as? AVPlayerItem else {return}
            Task {@MainActor in self?.finished(item)}
        }
        interruptToken=NotificationCenter.default.addObserver(forName:AVAudioSession.interruptionNotification,object:nil,queue:.main){[weak self] n in
            let type=n.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt
            if type==AVAudioSession.InterruptionType.began.rawValue {Task {@MainActor in self?.pause()}}
        }
        routeToken=NotificationCenter.default.addObserver(forName:AVAudioSession.routeChangeNotification,object:nil,queue:.main){[weak self] n in
            if n.userInfo?[AVAudioSessionRouteChangeReasonKey] as? UInt == AVAudioSession.RouteChangeReason.oldDeviceUnavailable.rawValue {Task {@MainActor in self?.pause()}}
        }
        let remote=MPRemoteCommandCenter.shared()
        remote.playCommand.addTarget {[weak self] _ in Task {@MainActor in self?.resume()};return .success}
        remote.pauseCommand.addTarget {[weak self] _ in Task {@MainActor in self?.pause()};return .success}
        remote.stopCommand.addTarget {[weak self] _ in Task {@MainActor in self?.stop()};return .success}
        remote.nextTrackCommand.addTarget {[weak self] _ in Task {@MainActor in self?.next(force:true)};return .success}
        remote.previousTrackCommand.addTarget {[weak self] _ in Task {@MainActor in self?.previous()};return .success}
        remote.changePlaybackPositionCommand.addTarget {[weak self] e in guard let e=e as? MPChangePlaybackPositionCommandEvent else {return .commandFailed};Task {@MainActor in self?.seek(e.positionTime)};return .success}
    }
    func chapter(_ qari:ReciterSelection,_ surah:Int) {
        guard let edition=content.chapterReciters.first(where:{$0.id==qari.chapterId}) else {error="Audio qari tidak ditemukan.";return}
        context = .murottal;queue=(surah...114).compactMap {n in edition.url(n).map {AudioTrack(url:$0,qari:qari,surah:n,verse:nil,timing:nil)}};cursor=0;load()
    }
    func collection(_ entries:[SavedEntry]) {
        let tracks=entries.compactMap {entry -> AudioTrack? in
            let pieces=entry.key.split(separator:"|")
            guard pieces.count==2,let q=content.qaris.first(where:{$0.id==pieces[0]}),let s=Int(pieces[1]),let reciter=content.chapterReciters.first(where:{$0.id==q.chapterId}),let url=reciter.url(s) else{return nil}
            return AudioTrack(url:url,qari:q,surah:s,verse:nil,timing:nil)
        }
        guard !tracks.isEmpty else {return};context = .murottal;queue=tracks;cursor=0;load()
    }
    func verses(qari:ReciterSelection,start:Reference,end:Reference,each:Int=1,passes:Int=1) {
        do {
            let tracks=try AudioTrack.reading(content:content,qari:qari,start:start,end:end,each:each,passes:passes)
            context = .reader;queue=tracks;cursor=0;load()
        }catch {self.error="Rentang audio tidak valid. Periksa ayat awal, akhir, dan pengulangannya."}
    }
    private func makeItem(_ run:Range<Int>)->AVPlayerItem {
        let first=queue[run.lowerBound],last=queue[run.upperBound-1]
        let item=AVPlayerItem(url:downloads.cached(first.url) ?? first.url)
        if let end=last.end {item.forwardPlaybackEndTime=CMTime(seconds:end,preferredTimescale:600)}
        itemRuns[ObjectIdentifier(item)]=run
        return item
    }
    private func load(autoplay:Bool=true) {
        guard queue.indices.contains(cursor) else {stop();return}
        generation+=1;status=nil;activeItem=nil;itemRuns.removeAll();player.pause();player.removeAllItems()
        wantsPlayback=autoplay;playing=false;error=nil
        do {sessionLease=try audioSession.playback()}
        catch {self.error=error.localizedDescription;wantsPlayback=false;track=nil;return}
        let run=AudioTrack.run(in:queue,from:cursor),item=makeItem(run)
        adopt(item,run:run,seekToStart:true)
        player.insert(item,after:nil)
        enqueueUpcoming()
    }
    /// AVQueuePlayer prepares the next verse files while the current one plays.
    /// A timed run needing a seek is loaded only at a deliberate repeat/surah boundary.
    private func enqueueUpcoming() {
        guard context == .reader else {return}
        while player.items().count<3,let last=player.items().last,
              let previous=itemRuns[ObjectIdentifier(last)],queue.indices.contains(previous.upperBound) {
            let next=previous.upperBound
            guard queue[next].timing==nil else {break}
            let item=makeItem(AudioTrack.run(in:queue,from:next))
            player.insert(item,after:last)
        }
    }
    private func selectTrack(_ index:Int) {
        cursor=index;track=queue[index];position=0;word=nil
        duration=queue[index].end.map {$0-queue[index].start} ?? 0
        onTrack?(queue[index]);nowPlaying()
    }
    private func adopt(_ item:AVPlayerItem,run:Range<Int>,seekToStart:Bool) {
        generation+=1;let currentGeneration=generation
        status=nil;activeItem=item;activeRun=run;advancing=true;readyHandled=false
        selectTrack(run.lowerBound)
        status=item.observe(\.status,options:[.initial,.new]) {[weak self] item,_ in Task {@MainActor in
            guard let self=self,self.generation==currentGeneration,item === self.activeItem else {return}
            if item.status == .failed {
                self.error=item.error?.localizedDescription ?? "Audio gagal dimuat. Coba lagi."
                self.player.pause();self.wantsPlayback=false;self.playing=false;self.advancing=false
            }
            guard item.status == .readyToPlay,!self.readyHandled else {return}
            self.readyHandled=true
            if self.track?.end==nil {self.duration=item.duration.seconds.isFinite ? item.duration.seconds:0}
            if seekToStart,let start=self.track?.start,start>0 {
                await self.player.seek(to:CMTime(seconds:start,preferredTimescale:600),toleranceBefore:.zero,toleranceAfter:.zero)
            }
            guard self.generation==currentGeneration,item === self.activeItem else {return}
            self.advancing=false
            if self.wantsPlayback {self.playing=true;self.player.playImmediately(atRate:self.speed);self.nowPlaying()}else {self.player.pause();self.playing=false}
        }}
    }
    private func finished(_ item:AVPlayerItem) {
        guard let run=itemRuns[ObjectIdentifier(item)] else {return}
        let successorQueued=itemRuns.values.contains {$0.lowerBound==run.upperBound}
        itemRuns.removeValue(forKey:ObjectIdentifier(item))
        // The queue advances itself. Never pause, replace, or seek the next file here.
        if successorQueued {return}
        guard item === activeItem else {return}
        cursor=run.upperBound-1
        next(autoplay:wantsPlayback)
    }
    private func update(_ seconds:Double) {
        guard !advancing,activeItem === player.currentItem,track != nil,seconds.isFinite else {return}
        // Only metadata and highlighting change at an internal ayah boundary.
        // The original surah recording continues through its natural pauses.
        while cursor+1<activeRun.upperBound,seconds>=queue[cursor+1].start {
            selectTrack(cursor+1)
        }
        guard let t=track else {return}
        if t.end==nil,let d=player.currentItem?.duration.seconds,d.isFinite {duration=d}
        position=max(0,seconds-t.start)
        if duration>0 {position=min(position,duration)}
        word=t.timing?.word(at:seconds)
        if Date().timeIntervalSince(lastNowPlaying)>=1 {nowPlaying()}
    }
    func resume() {
        guard track != nil else {return}
        wantsPlayback=true
        guard !advancing else {return}
        // Explicit play must reactivate playback after a system interruption.
        do {sessionLease=try audioSession.playback();playing=true;player.playImmediately(atRate:speed);nowPlaying()}
        catch {self.error=error.localizedDescription;wantsPlayback=false;playing=false}
    }
    func pause() {wantsPlayback=false;player.pause();playing=false;nowPlaying()}
    func toggle() {playing ? pause():resume()}
    func setSpeed(_ rate:Float) {speed=rate;if playing {player.rate=rate}}
    func seek(_ seconds:Double) {guard let t=track else {return};player.seek(to:CMTime(seconds:t.start+max(0,min(duration,seconds)),preferredTimescale:600),toleranceBefore:.zero,toleranceAfter:.zero)}
    func next(force:Bool=false,autoplay:Bool=true) {
        guard !queue.isEmpty else {return}
        if context == .murottal && repeatMode==1 && !force {load(autoplay:autoplay);return}
        if cursor+1<queue.count {cursor+=1;load(autoplay:autoplay)}else if context == .murottal && repeatMode==2 {cursor=0;load(autoplay:autoplay)}else {stop()}
    }
    func previous() {if position>3 {seek(0)}else if cursor>0 {cursor-=1;load()}else {seek(0)}}
    func retry() {load()}
    func stopReader() {if context == .reader {stop()}}
    func stop() {
        generation+=1;status=nil;activeItem=nil;itemRuns.removeAll();wantsPlayback=false
        player.pause();player.removeAllItems();queue=[];cursor=0;activeRun=0..<0
        track=nil;playing=false;word=nil;advancing=false
        MPNowPlayingInfoCenter.default().nowPlayingInfo=nil;audioSession.release(sessionLease);sessionLease=nil
    }
    private func nowPlaying() {
        guard let t=track else {return}
        var data:[String:Any]=[MPMediaItemPropertyTitle:content.corpus.surahs[t.surah-1].name+(t.verse.map {" · Ayat \($0.ayah)"} ?? ""),MPMediaItemPropertyArtist:t.qari.name,MPMediaItemPropertyPlaybackDuration:duration,MPNowPlayingInfoPropertyElapsedPlaybackTime:position,MPNowPlayingInfoPropertyPlaybackRate:playing ? speed:0]
        if artworkQari != t.qari.id {artworkQari=t.qari.id;artwork=content.portrait(t.qari.id).map {image in MPMediaItemArtwork(boundsSize:image.size){_ in image}}}
        if let artwork=artwork {data[MPMediaItemPropertyArtwork]=artwork}
        lastNowPlaying=Date()
        MPNowPlayingInfoCenter.default().nowPlayingInfo=data
    }
}
