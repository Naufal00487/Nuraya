import SwiftUI
import NurayaCore
struct RecitationView:View {
    @ObservedObject var state:AppState
    @StateObject private var local:QuranRecitationService
    @ObservedObject var speech:SpeechService
    @Environment(\.scenePhase) var scenePhase
    @State private var surah=112;@State private var ayah=1;@State private var mode=0;@State private var allowNetwork=false;@State private var resultRecorded=false
    init(state:AppState){self.state=state;_local=StateObject(wrappedValue:QuranRecitationService(state:state));speech=state.speech}
    var verse:Verse? {state.content.corpus.verse(Reference(surah,ayah))}
    var text:String {mode==0 ? local.transcript:speech.text}
    var comparison:Comparison? {guard let verse=verse,!text.isEmpty,!local.micOnly else{return nil};return RecitationMatcher.compare(expected:verse.text,recognized:text,final:mode==0 ? local.final:!speech.recording)}
    var body:some View {PageContainer(dark:state.settings.dark){Text("AI bacaan Al-Qur’an").font(.system(size:28,weight:.bold));Text("Dengarkan perkiraan kata dari bacaanmu, lalu periksa bersama teks ayat.");Picker("Mesin",selection:$mode){Text("AI lokal").tag(0);Text("Pengenal iPhone").tag(1)}.pickerStyle(.segmented).disabled(local.recording || local.processing || speech.recording)
        Picker("Surah",selection:$surah){ForEach(state.content.corpus.surahs){s in Text(s.name).tag(s.id)}}.disabled(local.recording || local.processing || speech.recording);Stepper("Ayat \(ayah)",value:$ayah,in:1...state.content.corpus.surahs[surah-1].ayahCount).disabled(local.recording || local.processing || speech.recording)
        if let verse=verse{ArabicText(text:verse.text,size:state.settings.arabicSize);Text(verse.indonesian).font(.system(size:18));Button("Dengarkan contoh"){state.audio.verses(qari:state.selectedQari,start:verse.reference,end:verse.reference)}}
        if mode==0 {if !local.ready{Text("Model bacaan ±59,7 MB diunduh sekali. Setelah siap, suara diproses lokal di iPhone.").font(.callout);PrimaryButton(title:local.downloading ? "Mengunduh dan memeriksa model…":"Unduh model AI",icon:"download"){Task{await local.download()}}.disabled(local.downloading)}
            HStack{Text(local.recording ? "Mendengarkan…":"Mikrofon");Spacer();Text(timeText(local.seconds)).monospacedDigit()};ProgressView(value:min(1,local.level*12)).tint(.blue)
            if local.processing{ProgressView("Memproses perkiraan bacaan…")}
            PrimaryButton(title:local.recording ? "Selesai membaca":"Mulai membaca",icon:"microphone"){if local.recording{local.finish()}else{resultRecorded=false;Task{await local.start()}}}.disabled(!local.ready || (local.processing && !local.recording) || local.downloading)
            Button("Cek mikrofon 5 detik tanpa model"){Task{await local.start(microphoneOnly:true)}}.disabled(local.recording || local.processing)
            if local.recording || local.processing{Button("Batalkan",role:.destructive){local.cancel()}}
            if let error=local.error{Text(error).foregroundStyle(.red)}
        }else {Toggle("Izinkan layanan suara Apple jika model lokal Arab belum tersedia",isOn:$allowNetwork);Text("Pengenal iPhone bergantung dukungan bahasa Arab di perangkat. Bila daring dipilih, suara diproses oleh layanan Apple.").font(.footnote);PrimaryButton(title:speech.recording ? "Selesai membaca":"Mulai membaca",icon:"microphone"){state.audio.stop();if speech.recording{speech.finish()}else{Task{await speech.start(locale:"ar-SA",allowNetwork:allowNetwork)}}};if let error=speech.error{Text(error).foregroundStyle(.red)}}
        if !text.isEmpty{Text(local.micOnly ? "Hasil cek mikrofon":"Perkiraan yang terdengar").font(.title3.bold());if local.micOnly{Text(text)}else{ArabicText(text:text,size:28)}}
        if let c=comparison{Text("Kata cocok: \(c.matched)/\(c.words.count)").font(.headline);comparisonText(c).font(.custom("AmiriQuran-Regular",size:32)).lineSpacing(12).multilineTextAlignment(.trailing).frame(maxWidth:.infinity,alignment:.trailing).environment(\.layoutDirection,.rightToLeft);Text(c.final ? "Hijau: cocok. Oranye: berbeda. Abu-abu: belum terdengar.":"Hasil sementara. Bagian yang belum selesai belum dinilai.").font(.footnote);if !c.extras.isEmpty{Text("Kata tambahan yang terdengar: "+c.extras.joined(separator:" ")).font(.callout)}}
        Text("Hasil ini membandingkan perkiraan kata. AI belum menilai tajwid, makhraj, harakat, atau panjang mad secara tervalidasi. Suara dan transkrip lokal tidak disimpan.").font(.footnote).foregroundStyle(.secondary)
    }.navigationTitle("AI bacaan").onAppear{local.inspect()}.onChange(of:surah){_ in ayah=1}.onChange(of:scenePhase){p in if p == .background{local.cancel();speech.stop()}}.onDisappear{local.cancel();speech.stop();state.audio.stopReader()}}
    func comparisonText(_ c:Comparison)->Text {c.words.reduce(Text("")){part,w in part+Text(w.text+" ").foregroundColor(w.state == .matched ? .green:w.state == .different ? .orange:.secondary)}}
}
