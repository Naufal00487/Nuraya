import SwiftUI
import NurayaCore
struct HomeView:View {
    @ObservedObject var state:AppState;let selectTab:(Int)->Void
    @State private var banner=0
    var body:some View {ScrollView{VStack(alignment:.leading,spacing:20){
        PrayerHeader(state:state)
        HStack(spacing:12){NavigationLink(value:Route.search){searchCard("Cari Ayat")};NavigationLink(value:Route.hadith){searchCard("Cari Hadis")}}.padding(.horizontal,20)
        VStack(alignment:.leading,spacing:4){Text("Assalamualaikum,").font(.system(size:19));Text(state.settings.name).font(.system(size:24,weight:.bold))}.padding(.horizontal,20)
        TabView(selection:$banner){ForEach(0..<2){i in NavigationLink(value:i==0 ? Route.learning:Route.search){AssetImage(content:state.content,path:"brand/banners/\(i==0 ? "learn":"explore").png",fit:true).clipShape(RoundedRectangle(cornerRadius:18))}.tag(i)}}.tabViewStyle(.page(indexDisplayMode:.always)).frame(height:150).padding(.horizontal,20)
        Text("Temani langkah belajarmu").font(.system(size:22,weight:.semibold)).padding(.horizontal,20)
        LazyVGrid(columns:[GridItem(.flexible()),GridItem(.flexible())],spacing:12){
            Button{selectTab(1)}label:{tile("Baca Al-Qur’an","book-open-cover")}
            NavigationLink(value:Route.recitation){tile("AI bacaan Al-Qur’an","microphone")}
            NavigationLink(value:Route.learning){tile("Hafalan, tajwid & kuis","graduation-cap")}
            NavigationLink(value:Route.search){tile("Cari ayat & makna","books")}
            Button{selectTab(2)}label:{tile("Dengarkan murottal","headphones")}
            NavigationLink(value:Route.library){tile("Koleksi saya","bookmark")}
        }.padding(.horizontal,20)
        if let last=state.settings.lastRead {Button{state.open(last)}label:{MenuRow(title:"Lanjutkan bacaan",subtitle:state.content.corpus.surahs[last.surah-1].name+" · Ayat \(last.ayah)",icon:"bookmark",dark:state.settings.dark)}.padding(.horizontal,20)}
        HStack{Text("Lantunan pilihan").font(.system(size:22,weight:.semibold));Spacer();Button("Lihat semua"){selectTab(2)}}.padding(.horizontal,20)
        ScrollView(.horizontal,showsIndicators:false){HStack(alignment:.top,spacing:18){ForEach(state.content.qaris.prefix(12)){q in NavigationLink(value:Route.qari(q.id)){VStack(spacing:8){QariPhoto(content:state.content,qari:q,size:88,rounded:44);Text(q.name).font(.system(size:16,weight:.semibold)).multilineTextAlignment(.center).lineLimit(3)}.frame(width:112)}}}.padding(.horizontal,20)}
        NavigationLink(value:Route.settings){Label{Text("Pengaturan membaca")}icon:{AppIcon(name:"settings")}}.frame(maxWidth:.infinity)
    }.padding(.bottom,24)}.foregroundStyle(Palette(dark:state.settings.dark).text).task{while !Task.isCancelled {try? await Task.sleep(nanoseconds:6_000_000_000);if !Task.isCancelled {withAnimation{banner=(banner+1)%2}}}}}
    func searchCard(_ name:String)->some View {HStack{Text(name).font(.system(size:18,weight:.semibold));Spacer();AppIcon(name:"search")}.padding(16).frame(maxWidth:.infinity,minHeight:60).background(Palette(dark:state.settings.dark).raised,in:RoundedRectangle(cornerRadius:20))}
    func tile(_ name:String,_ icon:String)->some View {VStack(alignment:.leading,spacing:14){AppIcon(name:icon).frame(width:44,height:44).background(Palette(dark:state.settings.dark).raised,in:RoundedRectangle(cornerRadius:14));Text(name).font(.system(size:18,weight:.semibold)).multilineTextAlignment(.leading).fixedSize(horizontal:false,vertical:true)}.padding(18).frame(maxWidth:.infinity,minHeight:146,alignment:.topLeading).background(Palette(dark:state.settings.dark).card,in:RoundedRectangle(cornerRadius:24))}
}
struct PrayerHeader:View {
    @ObservedObject var state:AppState
    var body:some View {TimelineView(.periodic(from:.now,by:1)){context in
        let date=context.date,hour=Calendar.current.component(.hour,from:date),landscape=hour>=5 && hour<=10 ? "morning":hour<=14 && hour>=11 ? "day":hour>=15 && hour<=17 ? "evening":"night"
        VStack(spacing:18){HStack(alignment:.top,spacing:18){VStack(alignment:.leading,spacing:8){Text(PrayerService.time(date,settings:state.settings)).font(.system(size:42,weight:.bold)).monospacedDigit();if let next=PrayerService.next(date,settings:state.settings){Text(next.name+" − "+timeText(next.date.timeIntervalSince(date))).font(.system(size:16)).monospacedDigit()}else {Text("Atur lokasi salat").font(.system(size:16))};Text(PrayerService.hijri(date,settings:state.settings)).font(.system(size:16))}.frame(maxWidth:.infinity,alignment:.leading)
            Rectangle().fill(.white.opacity(0.65)).frame(width:1)
            VStack(alignment:.leading,spacing:14){HStack{NavigationLink(value:Route.qibla){HStack{AppIcon(name:"kaaba");Text("Arah Kiblat").font(.system(size:16,weight:.semibold))}};Spacer();NavigationLink(value:Route.profile){AppIcon(name:"user")}};NavigationLink(value:Route.location){Text(state.settings.locationName.isEmpty ? "Gunakan lokasi HP":state.settings.locationName).font(.system(size:16)).lineLimit(2)};Text(date.formatted(.dateTime.day().month(.wide).year().locale(Locale(identifier:"id_ID")))).font(.system(size:16))}.frame(maxWidth:.infinity,alignment:.leading)}.fixedSize(horizontal:false,vertical:true).foregroundStyle(.white).shadow(color:.black.opacity(0.65),radius:2,y:1)
            let moments=PrayerService.day(date,settings:state.settings).filter {$0.id != "sunrise"}
            if !moments.isEmpty {HStack(spacing:3){ForEach(moments){p in VStack(spacing:5){Text(p.name).font(.system(size:14));Text(PrayerService.time(p.date,settings:state.settings)).font(.system(size:16,weight:.bold))}.frame(maxWidth:.infinity).padding(.vertical,10).background(PrayerService.next(date,settings:state.settings)?.id==p.id ? Color.nuraya:Color.clear,in:RoundedRectangle(cornerRadius:12))}}.padding(6).foregroundStyle(.white).background(Color(hex:0x19263E),in:RoundedRectangle(cornerRadius:20))}
        }.padding(20).background{AssetImage(content:state.content,path:"brand/\(landscape).jpg").clipped()}.clipped()
    }}
}
struct MurottalView:View {
    @ObservedObject var state:AppState;@State private var query=""
    var body:some View {PageContainer(dark:state.settings.dark){AssetImage(content:state.content,path:"brand/murottal_\(state.settings.dark ? "dark":"light").jpg",fit:true).frame(height:160).clipShape(RoundedRectangle(cornerRadius:22));Text("Pilih qari").font(.system(size:26,weight:.semibold));Text("28 qari · dengarkan, simpan, dan ulangi surah pilihanmu.").font(.system(size:18));TextField("Cari nama qari",text:$query).textFieldStyle(.roundedBorder);ForEach(state.content.qaris.filter {query.isEmpty || Normalization.search($0.name).contains(Normalization.search(query))}){q in NavigationLink(value:Route.qari(q.id)){HStack(spacing:16){QariPhoto(content:state.content,qari:q,size:70,rounded:35);Text(q.name).font(.system(size:20,weight:.semibold));Spacer();AppIcon(name:"angle-right")}.padding(16).frame(maxWidth:.infinity,alignment:.leading).background(Palette(dark:state.settings.dark).card,in:RoundedRectangle(cornerRadius:22))}}}}
}
struct QariSurahsView:View {
    @ObservedObject var state:AppState;let qari:ReciterSelection
    @State private var query=""
    var body:some View {PageContainer(dark:state.settings.dark){HStack(spacing:16){QariPhoto(content:state.content,qari:qari,size:96,rounded:24);Text(qari.name).font(.system(size:24,weight:.semibold))};TextField("Cari surah",text:$query).textFieldStyle(.roundedBorder);ForEach(state.content.corpus.surahs.filter {query.isEmpty || Normalization.search($0.name).contains(Normalization.search(query)) || String($0.id)==query}){s in Button {state.audio.chapter(qari,s.id);state.history(qari.id,s.id);state.fullPlayer=true}label:{MenuRow(title:"\(s.id). \(s.name)",subtitle:"\(s.ayahCount) ayat · Dengarkan",icon:"play",dark:state.settings.dark)}}}.navigationTitle("Surah murottal")}
}
struct MiniPlayer:View {
    @ObservedObject var state:AppState;@ObservedObject var audio:AudioService
    var body:some View {if let t=audio.track {HStack(spacing:12){Button{state.fullPlayer=true}{HStack(spacing:12){QariPhoto(content:state.content,qari:t.qari,size:48,rounded:12);VStack(alignment:.leading,spacing:4){Text(state.content.corpus.surahs[t.surah-1].name).font(.headline);Text(t.qari.name).font(.caption).lineLimit(1)};Spacer()}}.buttonStyle(.plain);Button{audio.toggle()}label:{AppIcon(name:audio.playing ? "pause":"play").frame(width:48,height:48)};Button{audio.stop()}label:{AppIcon(name:"cross",size:20).frame(width:44,height:48)}}.padding(.horizontal,16).padding(.vertical,8).background(Palette(dark:state.settings.dark).raised)}}
}
struct PlayerView:View {
    @ObservedObject var state:AppState;@ObservedObject var audio:AudioService
    @Environment(\.dismiss) var dismiss
    @State private var saving=false
    var body:some View {NavigationStack{PageContainer(dark:state.settings.dark){if let t=audio.track {
        QariPhoto(content:state.content,qari:t.qari,size:250,rounded:28).frame(maxWidth:.infinity)
        Text(state.content.corpus.surahs[t.surah-1].name).font(.system(size:30,weight:.bold)).frame(maxWidth:.infinity)
        Text(t.qari.name).font(.system(size:20)).frame(maxWidth:.infinity)
        if let error=audio.error {Text(error).foregroundStyle(.red);PrimaryButton(title:"Coba lagi"){audio.retry()}}
        Slider(value:Binding(get:{min(audio.position,max(audio.duration,0.1))},set:{audio.seek($0)}),in:0...max(audio.duration,0.1)).disabled(audio.duration<=0)
        HStack{Text(timeText(audio.position));Spacer();Text(timeText(audio.duration))}.monospacedDigit().font(.system(size:16))
        HStack(spacing:30){Button{audio.previous()}label:{AppIcon(name:"step-backward",size:30).frame(width:56,height:56)};Button{audio.toggle()}label:{AppIcon(name:audio.playing ? "pause":"play",size:30).frame(width:76,height:76).foregroundStyle(.white).background(Color.nuraya,in:Circle())};Button{audio.next(force:true)}label:{AppIcon(name:"step-forward",size:30).frame(width:56,height:56)}}.frame(maxWidth:.infinity)
        HStack{Menu{ForEach([Float(0.75),1,1.25,1.5,2],id:\.self){r in Button("\(r,specifier:"%.2g")×"){audio.setSpeed(r)}}}label:{VStack{AppIcon(name:"tachometer-fast");Text("\(audio.speed,specifier:"%.2g")×").font(.caption)}};Spacer();Button{audio.repeatMode=(audio.repeatMode+1)%3}label:{VStack{AppIcon(name:"arrows-repeat-1");Text(["Ulang mati","Ulang satu","Ulang semua"][audio.repeatMode]).font(.caption)}};Spacer();Button{state.favorite(t.qari.id,t.surah)}label:{VStack{AppIcon(name:"heart");Text("Favorit").font(.caption)}};Spacer();Button{Task{saving=true;defer{saving=false};do{_=try await state.downloads.fetch(t.url)}catch{state.error=error.localizedDescription}}}label:{VStack{if saving{ProgressView()}else{AppIcon(name:"download")};Text(state.downloads.cached(t.url)==nil ? "Unduh":"Tersimpan").font(.caption)}}.disabled(saving)}.frame(minHeight:60)
        PrimaryButton(title:"Baca & dengarkan dari awal",icon:"book-open-cover"){let q=t.qari;state.settings.qari=q.id;dismiss();state.open(Reference(t.surah,1));audio.verses(qari:q,start:Reference(t.surah,1),end:Reference(t.surah,state.content.corpus.surahs[t.surah-1].ayahCount))}
        NavigationLink {QariSurahsView(state:state,qari:t.qari)}label:{MenuRow(title:"Daftar surah",icon:"list-music",dark:state.settings.dark)}
    }else {Text("Belum ada audio yang diputar.");PrimaryButton(title:"Kembali"){dismiss()}}}.navigationTitle("Murottal").navigationBarTitleDisplayMode(.inline).toolbar{ToolbarItem(placement:.navigationBarLeading){Button{dismiss()}label:{AppIcon(name:"angle-down").frame(width:44,height:44).accessibilityLabel("Kecilkan pemutar")}};ToolbarItem(placement:.navigationBarTrailing){Menu{Button("Hentikan audio",role:.destructive){audio.stop();dismiss()};Button("Koleksi saya"){dismiss()}}label:{AppIcon(name:"menu-dots-vertical").frame(width:44,height:44)}}}}}
}
