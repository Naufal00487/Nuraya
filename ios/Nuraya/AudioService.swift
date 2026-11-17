import Foundation
import AVFoundation
import MediaPlayer
import NurayaCore
struct AudioTrack:Identifiable {
    let id=UUID()
    let url:URL
    let qari:ReciterSelection
    let surah:Int
    let verse:Verse?
    let timing:VerseTiming?
    var start:Double {timing?.start ?? 0}
    var end:Double? {timing?.end}
}
@MainActor final class AudioService:ObservableObject {
    enum Context {case murottal,reader}
    let player=AVPlayer(),content:ContentStore,downloads:DownloadStore
    @Published var track:AudioTrack?;@Published var playing=false;@Published var position=0.0;@Published var duration=0.0;@Published var speed:Float=1;@Published var repeatMode=0;@Published var error:String?;@Published var word:Int?
    var onTrack:((AudioTrack)->Void)?
    private(set) var context:Context = .murottal
    private var queue=[AudioTrack](),cursor=0,generation=0,advancing=false
    private var artwork:MPMediaItemArtwork?,artworkQari="",lastNowPlaying=Date.distantPast
    private var tick:Any?,endToken:NSObjectProtocol?,interruptToken:NSObjectProtocol?,routeToken:NSObjectProtocol?,status:NSKeyValueObservation?
    init(content:ContentStore,downloads:DownloadStore) {
        self.content=content;self.downloads=downloads
        tick=player.addPeriodicTimeObserver(forInterval:CMTime(seconds:0.06,preferredTimescale:600),queue:.main) {[weak self] t in Task {@MainActor in self?.update(t.seconds)}}
        endToken=NotificationCenter.default.addObserver(forName:.AVPlayerItemDidPlayToEndTime,object:nil,queue:.main){[weak self] n in Task {@MainActor in guard let self=self,n.object as? AVPlayerItem === self.player.currentItem else {return};self.next()}}
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
            let verses=try content.corpus.range(start,end,each:each,passes:passes)
            guard let reciter=content.verseReciters.first(where:{$0.id==qari.verseId}) else {throw ContentError.invalidReference}
            var timingCache=[Int:TimedChapter](),tracks=[AudioTrack]()
            for v in verses {
                if timingCache[v.surah]==nil {timingCache[v.surah]=content.timing(qari,surah:v.surah)}
                if let chapter=timingCache[v.surah],let timing=chapter.verses.first(where:{$0.ayah==v.ayah}),let u=URL(string:chapter.audioUrl),timing.end>timing.start {
                    tracks.append(AudioTrack(url:u,qari:qari,surah:v.surah,verse:v,timing:timing))
                } else if let u=reciter.url(v) {tracks.append(AudioTrack(url:u,qari:qari,surah:v.surah,verse:v,timing:nil))}
                else {throw ContentError.invalidReference}
            }
            context = .reader;queue=tracks;cursor=0;load()
        }catch {self.error="Rentang audio tidak valid. Periksa ayat awal, akhir, dan pengulangannya."}
    }
    private func load() {
        guard queue.indices.contains(cursor) else {stop();return}
        generation+=1;let currentGeneration=generation;advancing=true;error=nil;playing=false;player.pause();status=nil
        let t=queue[cursor];track=t;position=0;duration=t.end.map {$0-t.start} ?? 0;word=nil
        do {try AVAudioSession.sharedInstance().setCategory(.playback,mode:.default);try AVAudioSession.sharedInstance().setActive(true)}catch {self.error=error.localizedDescription;return}
        let item=AVPlayerItem(url:downloads.cached(t.url) ?? t.url);player.replaceCurrentItem(with:item)
        status=item.observe(\.status,options:[.initial,.new]) {[weak self] item,_ in Task {@MainActor in
            guard let self=self,self.generation==currentGeneration else {return}
            if item.status == .failed {self.error=item.error?.localizedDescription ?? "Audio gagal dimuat. Coba lagi.";self.playing=false;self.advancing=false}
            if item.status == .readyToPlay {
                if t.end==nil {self.duration=item.duration.seconds.isFinite ? item.duration.seconds:0}
                await self.player.seek(to:CMTime(seconds:t.start,preferredTimescale:600),toleranceBefore:.zero,toleranceAfter:.zero)
                guard self.generation==currentGeneration else {return};self.advancing=false;self.resume()
            }
        }}
        onTrack?(t);nowPlaying()
    }
    private func update(_ seconds:Double) {
        guard let t=track,seconds.isFinite else {return}
        position=max(0,seconds-t.start)
        if let end=t.end,seconds>=end-0.02,playing,!advancing {next();return}
        word=t.timing?.word(at:seconds)
        if t.end==nil,let d=player.currentItem?.duration.seconds,d.isFinite {duration=d}
        if Date().timeIntervalSince(lastNowPlaying)>=1 {nowPlaying()}
    }
    func resume() {guard track != nil else {return};playing=true;player.playImmediately(atRate:speed);nowPlaying()}
    func pause() {player.pause();playing=false;nowPlaying()}
    func toggle() {playing ? pause():resume()}
    func setSpeed(_ rate:Float) {speed=rate;if playing {player.rate=rate}}
    func seek(_ seconds:Double) {guard let t=track else {return};player.seek(to:CMTime(seconds:t.start+max(0,min(duration,seconds)),preferredTimescale:600),toleranceBefore:.zero,toleranceAfter:.zero)}
    func next(force:Bool=false) {
        guard !queue.isEmpty else {return}
        if context == .murottal && repeatMode==1 && !force {load();return}
        if cursor+1<queue.count {cursor+=1;load()}else if context == .murottal && repeatMode==2 {cursor=0;load()}else {stop()}
    }
    func previous() {if position>3 {seek(0)}else if cursor>0 {cursor-=1;load()}else {seek(0)}}
    func retry() {load()}
    func stopReader() {if context == .reader {stop()}}
    func stop() {generation+=1;player.pause();player.replaceCurrentItem(with:nil);status=nil;queue=[];cursor=0;track=nil;playing=false;word=nil;advancing=false;MPNowPlayingInfoCenter.default().nowPlayingInfo=nil;try? AVAudioSession.sharedInstance().setActive(false,options:.notifyOthersOnDeactivation)}
    private func nowPlaying() {
        guard let t=track else {return}
        var data:[String:Any]=[MPMediaItemPropertyTitle:content.corpus.surahs[t.surah-1].name+(t.verse.map {" · Ayat \($0.ayah)"} ?? ""),MPMediaItemPropertyArtist:t.qari.name,MPMediaItemPropertyPlaybackDuration:duration,MPNowPlayingInfoPropertyElapsedPlaybackTime:position,MPNowPlayingInfoPropertyPlaybackRate:playing ? speed:0]
        if artworkQari != t.qari.id {artworkQari=t.qari.id;artwork=content.portrait(t.qari.id).map {image in MPMediaItemArtwork(boundsSize:image.size){_ in image}}}
        if let artwork=artwork {data[MPMediaItemPropertyArtwork]=artwork}
        lastNowPlaying=Date()
        MPNowPlayingInfoCenter.default().nowPlayingInfo=data
    }
}
