import Foundation
import SwiftUI
import NurayaCore
struct SavedEntry:Codable,Identifiable {
    var id=UUID().uuidString;var kind:String,key:String,title:String,subtitle:String,date=Date()
}
struct AppSettings:Codable {
    var name="",avatar=0,dark=false,language="id",qari="ar.alafasy",translation="id",arabicSize=30.0,tajwid=true,followAudio=true
    var latitude:Double?,longitude:Double?,locationName="",timeZone=TimeZone.current.identifier,hijriCorrection=0,prayerCorrection=2
    var quranReminders=false,prayerReminders=false,prayerLead=10,adhanSound=false,enabledPrayers=["fajr","dhuhr","asr","maghrib","isha"]
    var entries=[SavedEntry](),learning=LearningProgress(),lastRead:Reference?
}
@MainActor final class AppState:ObservableObject {
    let content:ContentStore;let storage:URL
    @Published var settings:AppSettings {didSet {save()}}
    @Published var error:String?
    @Published var reader:ReaderDestination?
    @Published var fullPlayer=false
    let audio:AudioService;let downloads:DownloadStore;let location=LocationService();let speech=SpeechService()
    init(content:ContentStore) throws {
        self.content=content
        storage=try FileManager.default.url(for:.applicationSupportDirectory,in:.userDomainMask,appropriateFor:nil,create:true).appendingPathComponent("Nuraya",isDirectory:true)
        try FileManager.default.createDirectory(at:storage,withIntermediateDirectories:true)
        var privateDirectory=storage;var values=URLResourceValues();values.isExcludedFromBackup=true;try privateDirectory.setResourceValues(values)
        let file=storage.appendingPathComponent("settings.json")
        if FileManager.default.fileExists(atPath:file.path) {settings=try JSONDecoder().decode(AppSettings.self,from:Data(contentsOf:file))}else {settings=AppSettings()}
        downloads=try DownloadStore(directory:storage.appendingPathComponent("offline"),content:content)
        audio=AudioService(content:content,downloads:downloads)
        audio.onTrack={ [weak self] track in self?.recordAudio(track) }
    }
    func save() {do {let data=try JSONEncoder().encode(settings);try data.write(to:storage.appendingPathComponent("settings.json"),options:[.atomic,.completeFileProtectionUntilFirstUserAuthentication])}catch {self.error="Pengaturan belum tersimpan: \(error.localizedDescription)"}}
    func open(_ reference:Reference,mode:String="standard") {guard content.corpus.verse(reference) != nil else {error="Rujukan ayat tidak ditemukan.";return};reader=ReaderDestination(reference:reference,mode:mode);recordReading(reference)}
    var selectedQari:ReciterSelection {content.qaris.first {$0.id==settings.qari} ?? content.qaris[0]}
    func mark(_ ref:Reference) {
        let exists=settings.entries.contains {$0.kind=="bookmark" && $0.key==ref.id}
        if exists {settings.entries.removeAll {$0.kind=="bookmark" && $0.key==ref.id}}
        else {settings.entries.append(SavedEntry(kind:"bookmark",key:ref.id,title:content.corpus.surahs[ref.surah-1].name,subtitle:"Ayat \(ref.ayah)"))}
    }
    func favorite(_ qari:String,_ surah:Int) {
        let key="\(qari)|\(surah)"
        if settings.entries.contains(where:{$0.kind=="favorite" && $0.key==key}) {settings.entries.removeAll {$0.kind=="favorite" && $0.key==key}}
        else {settings.entries.append(SavedEntry(kind:"favorite",key:key,title:content.corpus.surahs[surah-1].name,subtitle:content.qaris.first {$0.id==qari}?.name ?? qari))}
    }
    func history(_ qari:String,_ surah:Int) {let key="\(qari)|\(surah)";settings.entries.removeAll {$0.kind=="audio_history" && $0.key==key};settings.entries.append(SavedEntry(kind:"audio_history",key:key,title:content.corpus.surahs[surah-1].name,subtitle:content.qaris.first {$0.id==qari}?.name ?? qari));if settings.entries.filter({$0.kind=="audio_history"}).count>100,let id=settings.entries.first(where:{$0.kind=="audio_history"})?.id {settings.entries.removeAll {$0.id==id}}}
    func recordReading(_ ref:Reference) {
        settings.lastRead=ref
        record(SavedEntry(kind:"read_history",key:ref.id,title:content.corpus.surahs[ref.surah-1].name,subtitle:"Ayat \(ref.ayah) · Dibaca"))
    }
    private func record(_ entry:SavedEntry) {
        settings.entries.removeAll {$0.kind==entry.kind && $0.key==entry.key}
        settings.entries.append(entry)
        let excess=settings.entries.filter {$0.kind==entry.kind}.dropLast(100).map(\.id)
        settings.entries.removeAll {excess.contains($0.id)}
    }
    private func recordAudio(_ track:AudioTrack) {
        if let verse=track.verse {
            record(SavedEntry(kind:"verse_audio_history",key:track.qari.id+"|"+verse.reference.id,title:content.corpus.surahs[verse.surah-1].name,subtitle:"Ayat \(verse.ayah) · \(track.qari.name)"))
        }else {history(track.qari.id,track.surah)}
    }
    func playlist(_ qari:String,_ surah:Int) {
        let key="\(qari)|\(surah)"
        if settings.entries.contains(where:{$0.kind=="playlist" && $0.key==key}) {settings.entries.removeAll {$0.kind=="playlist" && $0.key==key}}
        else {settings.entries.append(SavedEntry(kind:"playlist",key:key,title:content.corpus.surahs[surah-1].name,subtitle:content.qaris.first {$0.id==qari}?.name ?? qari))}
    }
    func translated(_ id:String,_ en:String)->String {settings.language=="en" ? en:id}
}
struct ReaderDestination:Identifiable {var id=UUID();let reference:Reference,mode:String}
