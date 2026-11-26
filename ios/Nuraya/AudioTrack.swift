import Foundation
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

    /// Consecutive verses from the same recording are one physical playback item.
    /// Keep the original breathing/waqaf between timestamps; do not cut or crossfade.
    func continues(after previous:AudioTrack)->Bool {
        guard let timing=timing,let before=previous.timing,
              let verse=verse,let previousVerse=previous.verse else {return false}
        return url==previous.url && qari.id==previous.qari.id && surah==previous.surah
            && verse.ayah==previousVerse.ayah+1 && timing.start>=before.start && timing.end>before.end
    }

    static func run(in tracks:[AudioTrack],from start:Int)->Range<Int> {
        var end=start+1
        while end<tracks.count && tracks[end].continues(after:tracks[end-1]) {end+=1}
        return start..<end
    }

    static func reading(content:ContentStore,qari:ReciterSelection,start:Reference,end:Reference,each:Int,passes:Int)throws->[AudioTrack] {
        let verses=try content.corpus.range(start,end,each:each,passes:passes)
        guard let reciter=content.verseReciters.first(where:{$0.id==qari.verseId}) else {throw ContentError.invalidReference}
        var timingCache=[Int:TimedChapter](),checked=Set<Int>(),tracks=[AudioTrack]()
        for v in verses {
            if checked.insert(v.surah).inserted {timingCache[v.surah]=content.timing(qari,surah:v.surah)}
            if let chapter=timingCache[v.surah],let timing=chapter.verses.first(where:{$0.ayah==v.ayah}),
               let url=URL(string:chapter.audioUrl),timing.end>timing.start {
                tracks.append(AudioTrack(url:url,qari:qari,surah:v.surah,verse:v,timing:timing))
            }else if let url=reciter.url(v) {
                tracks.append(AudioTrack(url:url,qari:qari,surah:v.surah,verse:v,timing:nil))
            }else {throw ContentError.invalidReference}
        }
        return tracks
    }
}
