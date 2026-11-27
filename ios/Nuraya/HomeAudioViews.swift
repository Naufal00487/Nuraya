import SwiftUI
import NurayaCore
struct HomeView:View {
    @ObservedObject var state:AppState;let selectTab:(Int)->Void
    @State private var banner=0
    var body:some View {ScrollView{VStack(alignment:.leading,spacing:20){
        PrayerHeader(state:state).clipShape(RoundedRectangle(cornerRadius:26))
        HStack(spacing:12){NavigationLink(value:Route.search){searchCard("Cari Ayat")};NavigationLink(value:Route.hadith){searchCard("Cari Hadis")}}.padding(.horizontal,20)
        VStack(alignment:.leading,spacing:4){Text("Assalamualaikum,").font(.system(size:19));Text(state.settings.name).font(.system(size:24,weight:.bold))}.padding(.horizontal,20)
        TabView(selection:$banner){ForEach(0..<2){i in NavigationLink(value:i==0 ? Route.learning:Route.search){AssetImage(content:state.content,path:"brand/banners/\(i==0 ? "learn":"explore").png",fit:true).clipShape(RoundedRectangle(cornerRadius:18))}.tag(i)}}.tabViewStyle(.page(indexDisplayMode:.always)).frame(height:110).padding(.horizontal,20)
        Text("Temani langkah belajarmu").font(.system(size:22,weight:.semibold)).padding(.horizontal,20)
        LazyVGrid(columns:[GridItem(.flexible()),GridItem(.flexible())],spacing:12){
            Button{selectTab(1)}label:{tile("Baca Al-Qur’an","book-open-cover")}
            NavigationLink(value:Route.recitation){tile("AI bacaan Al-Qur’an","microphone")}
            NavigationLink(value:Route.learning){tile("Hafalan, tajwid & kuis","graduation-cap")}.accessibilityIdentifier("home_learning")
            NavigationLink(value:Route.search){tile("Cari ayat & makna","books")}
            Button{selectTab(2)}label:{tile("Dengarkan murottal","headphones")}
            NavigationLink(value:Route.library){tile("Koleksi saya","bookmark")}.accessibilityIdentifier("home_library")
        }.padding(.horizontal,20)
        if let last=state.settings.lastRead {Button{state.resumeReading()}label:{MenuRow(title:"Lanjutkan bacaan",subtitle:state.content.corpus.surahs[last.surah-1].name+" · Ayat \(last.ayah)",icon:"bookmark",dark:state.settings.dark)}.accessibilityIdentifier("home_resume").padding(.horizontal,20)}
        HStack{Text("Lantunan pilihan").font(.system(size:22,weight:.semibold));Spacer();Button("Lihat semua"){selectTab(2)}}.padding(.horizontal,20)
        ScrollView(.horizontal,showsIndicators:false){HStack(alignment:.top,spacing:18){ForEach(state.content.qaris.prefix(12)){q in NavigationLink(value:Route.qari(q.id)){VStack(spacing:8){QariPhoto(content:state.content,qari:q,size:88,rounded:44);Text(q.name).font(.system(size:16,weight:.semibold)).multilineTextAlignment(.center).lineLimit(3)}.frame(width:112)}}}.padding(.horizontal,20)}
        NavigationLink(value:Route.settings){Label{Text("Pengaturan membaca")}icon:{AppIcon(name:"settings")}}.frame(maxWidth:.infinity)
    }.padding(.bottom,24)}.foregroundStyle(Palette(dark:state.settings.dark).text).task{while !Task.isCancelled {try? await Task.sleep(nanoseconds:6_000_000_000);if !Task.isCancelled {withAnimation{banner=(banner+1)%2}}}}}
    func searchCard(_ name:String)->some View {HStack{Text(name).font(.system(size:18,weight:.semibold));Spacer();AppIcon(name:"search")}.padding(16).frame(maxWidth:.infinity,minHeight:60).background(Palette(dark:state.settings.dark).raised,in:RoundedRectangle(cornerRadius:20))}
    func tile(_ name:String,_ icon:String)->some View {VStack(alignment:.leading,spacing:14){AppIcon(name:icon).frame(width:44,height:44).background(Palette(dark:state.settings.dark).raised,in:RoundedRectangle(cornerRadius:14));Text(name).font(.system(size:18,weight:.semibold)).multilineTextAlignment(.leading).fixedSize(horizontal:false,vertical:true)}.padding(18).frame(maxWidth:.infinity,minHeight:146,alignment:.topLeading).background(Palette(dark:state.settings.dark).card,in:RoundedRectangle(cornerRadius:24))}
}
struct PrayerHeader:View {
    @ObservedObject var state:AppState
    var home=true
    var body:some View {
        TimelineView(.periodic(from:.now,by:1)){context in
            let date=context.date
            VStack(spacing:20){
                HStack(spacing:10){
                    NavigationLink(value:Route.location){
                        HStack(spacing:6){AppIcon(name:"marker",size:18);Text(state.settings.locationName.isEmpty ? "Pilih lokasi":state.settings.locationName).font(.system(size:17,weight:.semibold)).lineLimit(2);AppIcon(name:"angle-down",size:16)}
                    }
                    Spacer(minLength:4)
                    if home {
                        NavigationLink(value:Route.settings){AppIcon(name:"settings",size:24).frame(width:44,height:44)}.accessibilityLabel("Pengaturan").accessibilityIdentifier("home_settings")
                        NavigationLink(value:Route.profile){AssetImage(content:state.content,path:"brand/avatars/avatar_\(state.settings.avatar).png",fit:true).frame(width:42,height:42).clipShape(Circle())}
                    }else {
                        NavigationLink {ReminderSettingsView(state:state)}label:{AppIcon(name:"bell").frame(width:44,height:44).accessibilityLabel("Pengaturan azan")}
                    }
                }
                HStack(alignment:.top,spacing:18){
                    VStack(alignment:.leading,spacing:7){
                        Text(PrayerService.time(date,settings:state.settings)).font(.system(size:46,weight:.bold)).monospacedDigit()
                        if let next=PrayerService.next(date,settings:state.settings){
                            Text("Menuju "+next.name).font(.system(size:16))
                            Text(countdown(next.date.timeIntervalSince(date))).font(.system(size:22,weight:.semibold)).monospacedDigit()
                        }else {Text("Atur lokasi salat").font(.system(size:16))}
                        Text(PrayerService.hijri(date,settings:state.settings)).font(.system(size:15)).fixedSize(horizontal:false,vertical:true)
                    }.frame(maxWidth:.infinity,alignment:.leading)
                    VStack(alignment:.trailing,spacing:14){
                        NavigationLink(value:Route.qibla){HStack(spacing:6){AppIcon(name:"kaaba",size:21);Text("Arah Kiblat").font(.system(size:15,weight:.semibold))}.padding(12).background(Color(hex:0x25395B),in:Capsule())}
                        Text(formatted(date,"EEEE")).font(.system(size:19,weight:.semibold))
                        Text(formatted(date,"d MMMM yyyy")).font(.system(size:16)).multilineTextAlignment(.trailing)
                    }.frame(maxWidth:.infinity,alignment:.trailing)
                }.shadow(color:.black.opacity(0.45),radius:2,y:1)
                prayerStrip(date)
            }.foregroundStyle(.white).padding(20).background{
                if home {AssetImage(content:state.content,path:"brand/\(landscape(date)).jpg").clipped()}
                else {LinearGradient(colors:[Color.nuraya,Color(hex:0x17253B)],startPoint:.topLeading,endPoint:.bottomTrailing)}
            }.clipped()
        }
    }
    private func prayerStrip(_ date:Date)->some View {
        let moments=PrayerService.day(date,settings:state.settings)
        let next=PrayerService.next(date,settings:state.settings)?.id
        let labels=[("fajr","Subuh"),("dhuhr","Zuhur"),("asr","Asar"),("maghrib","Magrib"),("isha","Isya")]
        return HStack(spacing:2){ForEach(labels,id:\.0){id,name in
            VStack(spacing:6){Text(name).font(.system(size:14));Text(moments.first{$0.id==id}.map{PrayerService.time($0.date,settings:state.settings)} ?? "—:—").font(.system(size:16,weight:.bold)).monospacedDigit()}
                .frame(maxWidth:.infinity).padding(.vertical,11).background(next==id ? Color(hex:0x486CA2):Color.clear,in:RoundedRectangle(cornerRadius:12))
        }}.padding(5).background(Color(hex:0x17253B),in:RoundedRectangle(cornerRadius:20))
    }
    private func formatted(_ date:Date,_ pattern:String)->String {let f=DateFormatter();f.locale=Locale(identifier:state.settings.language=="en" ? "en_US":"id_ID");f.timeZone=TimeZone(identifier:state.settings.timeZone);f.dateFormat=pattern;return f.string(from:date)}
    private func landscape(_ date:Date)->String {var c=Calendar.current;c.timeZone=TimeZone(identifier:state.settings.timeZone) ?? .current;switch c.component(.hour,from:date){case 5...10:return "morning";case 11...14:return "day";case 15...17:return "evening";default:return "night"}}
    private func countdown(_ seconds:Double)->String {let n=max(0,Int(seconds));return String(format:"%02d:%02d:%02d",n/3600,(n%3600)/60,n%60)}
}
struct MurottalView:View {
    @ObservedObject var state:AppState;@State private var query=""
    var body:some View {PageContainer(dark:state.settings.dark){SectionArtwork(content:state.content,section:"murottal",title:"Murottal",subtitle:"Dengarkan ayat yang menenangkan hati.",dark:state.settings.dark);Text("Pilih qari").font(.system(size:26,weight:.semibold));Text("28 qari · dengarkan, simpan, dan ulangi surah pilihanmu.").font(.system(size:18));TextField("Cari nama qari",text:$query).textFieldStyle(.roundedBorder);ForEach(state.content.qaris.filter {query.isEmpty || Normalization.search($0.name).contains(Normalization.search(query))}){q in NavigationLink(value:Route.qari(q.id)){HStack(spacing:16){QariPhoto(content:state.content,qari:q,size:70,rounded:35);Text(q.name).font(.system(size:20,weight:.semibold));Spacer();AppIcon(name:"angle-right")}.padding(16).frame(maxWidth:.infinity,alignment:.leading).background(Palette(dark:state.settings.dark).card,in:RoundedRectangle(cornerRadius:22))}.accessibilityIdentifier("murottal_qari_"+q.id)}}}
}
struct QariSurahsView:View {
    @ObservedObject var state:AppState;let qari:ReciterSelection
    @State private var query=""
    var body:some View {PageContainer(dark:state.settings.dark){HStack(spacing:16){QariPhoto(content:state.content,qari:qari,size:96,rounded:24);Text(qari.name).font(.system(size:24,weight:.semibold))};TextField("Cari surah",text:$query).textFieldStyle(.roundedBorder);ForEach(state.content.corpus.surahs.filter {query.isEmpty || Normalization.search($0.name).contains(Normalization.search(query)) || String($0.id)==query}){s in Button {state.audio.chapter(qari,s.id);state.history(qari.id,s.id);state.fullPlayer=true}label:{MenuRow(title:"\(s.id). \(s.name)",subtitle:"\(s.ayahCount) ayat · Dengarkan",icon:"play",dark:state.settings.dark)}.accessibilityIdentifier("murottal_surah_\(s.id)")}}.navigationTitle("Surah murottal")}
}
struct MiniPlayer:View {
    @ObservedObject var state:AppState;@ObservedObject var audio:AudioService
    var body:some View {if let t=audio.track {HStack(spacing:12){Button{state.fullPlayer=true}label:{HStack(spacing:12){QariPhoto(content:state.content,qari:t.qari,size:48,rounded:12);VStack(alignment:.leading,spacing:4){Text(state.content.corpus.surahs[t.surah-1].name).font(.headline);Text(t.qari.name).font(.caption).lineLimit(1)};Spacer()}}.buttonStyle(.plain);Button{audio.toggle()}label:{AppIcon(name:audio.playing ? "pause":"play").frame(width:48,height:48)};Button{audio.stop()}label:{AppIcon(name:"cross",size:20).frame(width:44,height:48)}}.padding(.horizontal,16).padding(.vertical,8).background(Palette(dark:state.settings.dark).raised)}}
}
struct PlayerView:View {
    @ObservedObject var state:AppState
    @ObservedObject var audio:AudioService
    @Environment(\.dismiss) var dismiss
    @State private var saving=false
    private var palette:Palette {Palette(dark:state.settings.dark)}
    var body:some View {
        NavigationStack {
            ScrollView {
                VStack(spacing:24){
                    if let track=audio.track {
                        QariPhoto(content:state.content,qari:track.qari,size:min(300,UIScreen.main.bounds.width-96),rounded:28).padding(.top,12)
                        VStack(spacing:6){Text(state.content.corpus.surahs[track.surah-1].name).font(.system(size:24,weight:.semibold));Text(track.qari.name).font(.system(size:20)).foregroundStyle(palette.secondary)}
                        if let error=audio.error {Text(error).foregroundStyle(.red);PrimaryButton(title:"Coba lagi"){audio.retry()}}
                        VStack(spacing:8){
                            Slider(value:Binding(get:{min(audio.position,max(audio.duration,0.1))},set:{audio.seek($0)}),in:0...max(audio.duration,0.1)).disabled(audio.duration<=0).accessibilityLabel("Posisi audio")
                            HStack{Text(timeText(audio.position)).accessibilityIdentifier("player_elapsed");Spacer();Text(timeText(audio.duration))}.monospacedDigit().font(.system(size:15))
                        }.padding(.top,8)
                        playbackControls
                        collectionControls(track)
                        Button {readFromStart(track)}label:{HStack{AppIcon(name:"book-quran",size:20);Text("Baca & dengarkan dari awal").font(.system(size:16,weight:.semibold))}.frame(maxWidth:.infinity,minHeight:48).overlay(Capsule().stroke(palette.primary.opacity(0.45)))}.padding(.vertical,8)
                    }else {Text("Belum ada audio yang diputar.");PrimaryButton(title:"Kembali"){dismiss()}}
                }.padding(.horizontal,24).padding(.bottom,24).frame(maxWidth:600).frame(maxWidth:.infinity)
            }.foregroundStyle(palette.text).background(LinearGradient(colors:[palette.raised,palette.background],startPoint:.top,endPoint:.bottom).ignoresSafeArea())
            .navigationTitle("Murottal").navigationBarTitleDisplayMode(.inline)
            .toolbar{
                ToolbarItem(placement:.navigationBarLeading){Button{dismiss()}label:{AppIcon(name:"angle-down").frame(width:44,height:44)}.accessibilityLabel("Kecilkan pemutar")}
                ToolbarItem(placement:.navigationBarTrailing){Menu{
                    if let track=audio.track {NavigationLink("Daftar surah"){QariSurahsView(state:state,qari:track.qari)}}
                    NavigationLink("Koleksi saya"){LibraryView(state:state)}
                    Button("Hentikan audio",role:.destructive){audio.stop();dismiss()}
                }label:{AppIcon(name:"menu-dots-vertical").frame(width:44,height:44)}.accessibilityLabel("Menu pemutar")}
            }
        }
    }
    private var playbackControls:some View {
        HStack(spacing:12){
            Button{audio.repeatMode=(audio.repeatMode+1)%3}label:{AppIcon(name:"arrows-repeat-1").foregroundStyle(audio.repeatMode==0 ? palette.secondary:palette.primary).frame(width:44,height:56)}.accessibilityLabel(["Ulang mati","Ulang satu","Ulang semua"][audio.repeatMode])
            Spacer(minLength:0)
            Button{audio.previous()}label:{AppIcon(name:"step-backward",size:28).frame(width:44,height:56)}.accessibilityLabel("Surah sebelumnya")
            Button{audio.toggle()}label:{AppIcon(name:audio.playing ? "pause":"play",size:32).frame(width:76,height:76).foregroundStyle(.white).background(Color.nuraya,in:Circle())}.accessibilityLabel(audio.playing ? "Jeda":"Putar").accessibilityIdentifier("player_toggle")
            Button{audio.next(force:true)}label:{AppIcon(name:"step-forward",size:28).frame(width:44,height:56)}.accessibilityLabel("Surah berikutnya")
            Spacer(minLength:0)
            Menu{ForEach([Float(0.75),1,1.25,1.5,2],id:\.self){rate in Button("\(rate,specifier:"%.2g")×"){audio.setSpeed(rate)}}}label:{Text("\(audio.speed,specifier:"%.2g")×").font(.system(size:16,weight:.medium)).frame(width:44,height:56)}.accessibilityLabel("Kecepatan pemutaran")
        }
    }
    private func collectionControls(_ track:AudioTrack)->some View {
        let key=track.qari.id+"|\(track.surah)"
        return HStack(spacing:12){
            Button{state.favorite(track.qari.id,track.surah)}label:{actionLabel(state.settings.entries.contains(where:{$0.kind=="favorite" && $0.key==key}) ? "Difavoritkan":"Favorit","heart")}
            Spacer()
            Button{Task{saving=true;defer{saving=false};do{_=try await state.downloads.fetch(track.url)}catch{state.error=error.localizedDescription}}}label:{if saving {ProgressView().frame(width:80,height:64)}else{actionLabel(state.downloads.cached(track.url)==nil ? "Unduh":"Tersimpan","download")}}.disabled(saving)
            Spacer()
            Button{state.playlist(track.qari.id,track.surah)}label:{actionLabel(state.settings.entries.contains(where:{$0.kind=="playlist" && $0.key==key}) ? "Di koleksi":"Koleksi","list-music")}
        }.padding(.horizontal,12)
    }
    private func actionLabel(_ title:String,_ icon:String)->some View {VStack(spacing:8){AppIcon(name:icon,size:25);Text(title).font(.system(size:14,weight:.medium))}.frame(minWidth:72,minHeight:64)}
    private func readFromStart(_ track:AudioTrack){
        state.settings.qari=track.qari.id;dismiss()
        Task {try? await Task.sleep(nanoseconds:400_000_000);state.open(Reference(track.surah,1));audio.verses(qari:track.qari,start:Reference(track.surah,1),end:Reference(track.surah,state.content.corpus.surahs[track.surah-1].ayahCount))}
    }
}
