import SwiftUI
import NurayaCore
struct QuranModesView:View {
    @ObservedObject var state:AppState;@State private var query=""
    var body:some View {PageContainer(dark:state.settings.dark){SectionArtwork(content:state.content,section:"quran",title:"Al-Qur’an",subtitle:"Baca, pahami, dan dekatkan hati.",dark:state.settings.dark);Text("Pilih cara membaca").font(.system(size:26,weight:.semibold));NavigationLink(value:Route.search){MenuRow(title:"Cari ayat · makna & suara",icon:"search",dark:state.settings.dark)};TextField("Cari cara membaca",text:$query).textFieldStyle(.roundedBorder);ForEach(state.content.modes.filter {query.isEmpty || Normalization.search($0.titleId).contains(Normalization.search(query))}){mode in NavigationLink(value:Route.readingMode(mode.id))label:{HStack(spacing:16){AssetImage(content:state.content,path:"brand/covers/\(mode.id=="madinah_tajwid" ? "madinah":mode.id).png",fit:true).frame(width:84,height:120);VStack(alignment:.leading,spacing:8){Text(state.translated(mode.titleId,mode.titleEn)).font(.system(size:22,weight:.semibold));Text(state.translated(mode.descriptionId,mode.descriptionEn)).font(.system(size:16)).foregroundStyle(.secondary);Text("Buka bacaan").font(.system(size:16,weight:.semibold))}}.padding(18).frame(maxWidth:.infinity,alignment:.leading).background(Palette(dark:state.settings.dark).card,in:RoundedRectangle(cornerRadius:20))}.accessibilityIdentifier("mode_"+mode.id)}}}
}
struct SurahList:View {
    @ObservedObject var state:AppState;let mode:String;@State private var query="";@State private var juz=false
    var body:some View {PageContainer(dark:state.settings.dark){TextField("Cari surah atau nomor",text:$query).textFieldStyle(.roundedBorder);Picker("Daftar",selection:$juz){Text("Surah").tag(false);Text("Juz").tag(true)}.pickerStyle(.segmented)
        if juz {ForEach(1...30,id:\.self){j in if let v=state.content.corpus.verses.first(where:{$0.juz==j}) {Button{state.open(v.reference,mode:mode,juz:j)}label:{MenuRow(title:"Juz \(j)",subtitle:state.content.corpus.surahs[v.surah-1].name+" · Ayat \(v.ayah)",icon:"book-open-cover",dark:state.settings.dark)}}}}
        else {ForEach(state.content.corpus.surahs.filter {query.isEmpty || String($0.id)==query || Normalization.search($0.name).contains(Normalization.search(query))}){s in Button{state.open(Reference(s.id,1),mode:mode)}label:{HStack(spacing:14){Text(String(s.id)).font(.system(size:18,weight:.semibold)).frame(width:44,height:44).background(Palette(dark:state.settings.dark).raised,in:RoundedRectangle(cornerRadius:14));VStack(alignment:.leading,spacing:6){Text(s.name).font(.system(size:20,weight:.semibold));Text("\(s.ayahCount) ayat · \(s.revelation=="meccan" ? "Makkiyah":"Madaniyah")").font(.system(size:16)).foregroundStyle(.secondary)};Spacer();Text(s.arabicName).font(.custom("AmiriQuran-Regular",size:23))}.padding(16).background(Palette(dark:state.settings.dark).card,in:RoundedRectangle(cornerRadius:18))}.accessibilityIdentifier("surah_\(s.id)")}}
    }.navigationTitle(state.content.modes.first {$0.id==mode}?.titleId ?? "Al-Qur’an")}
}
struct ThemesView:View {
    @ObservedObject var state:AppState
    var body:some View {PageContainer(dark:state.settings.dark){Text("Pilihan rujukan per tema. Buka ayat untuk membaca konteks dan tafsirnya.");ForEach(state.content.themes){theme in NavigationLink {PageContainer(dark:state.settings.dark){ForEach(theme.references, id:\.self){r in if r.count==2,let v=state.content.corpus.verse(Reference(r[0],r[1])) {Button{state.open(v.reference,mode:"thematic")}label:{MenuRow(title:state.content.corpus.surahs[v.surah-1].name+" · "+v.reference.id,subtitle:v.indonesian,icon:"book-open-cover",dark:state.settings.dark)}}}}.navigationTitle(theme.title)}label:{MenuRow(title:theme.title,subtitle:"\(theme.references.count) rujukan ayat",icon:"books",dark:state.settings.dark)}};Text("Indeks pilihan Nuraya, bukan tafsir baru atau daftar seluruh ayat tentang tema ini.").font(.footnote).foregroundStyle(.secondary)}.navigationTitle("Terjemah tematik")}
}
struct ReaderView:View {
    @ObservedObject var state:AppState;let target:ReaderDestination;@ObservedObject var audio:AudioService
    @Environment(\.dismiss) var dismiss
    @State private var page=1;@State private var reference=Reference(1,1);@State private var controls=false;@State private var image:UIImage?;@State private var error:String?;@State private var loading=false;@State private var zoom=1.0;@State private var hidden=false;@State private var landscape=false;@State private var tikrar=0
    @State private var readingJuz:Int?;@State private var readingMenu=false
    @State private var panel=false;@State private var jump=false;@State private var audioSettings=false;@State private var actions=false;@State private var share=false;@State private var info=false;@State private var downloadingEdition=false;@State private var downloadCount=0
    init(state:AppState,target:ReaderDestination,audio:AudioService){
        self.state=state;self.target=target;self.audio=audio
        _reference=State(initialValue:target.reference)
        _readingJuz=State(initialValue:target.juz)
        let edition=state.content.modes.first(where:{$0.id==target.mode})?.edition
        let initial=edition.flatMap {try? state.content.index($0).page(for:target.reference)} ?? 1
        _page=State(initialValue:initial)
    }
    var sequence:ReadingSequence {ReadingSequence(corpus:state.content.corpus,reference:reference,juz:readingJuz)}
    private func recordPosition(){state.recordReading(reference,mode:target.mode,juz:readingJuz)}
    var mode:ReadingMode? {state.content.modes.first {$0.id==target.mode}}
    var edition:MushafEdition? {mode?.edition}
    var body:some View {Group {if let edition=edition {book(edition)}else {textReader}}
        .sheet(isPresented:$readingMenu){textReadingMenu}
        .sheet(isPresented:$panel){NavigationStack{StudyPanel(state:state,reference:reference,audio:audio).toolbar{ToolbarItem(placement:.navigationBarTrailing){Button("Tutup"){panel=false}}}}}
        .sheet(isPresented:$jump){JumpSheet(state:state,edition:edition,current:reference,onSelect:{r,j in reference=r;readingJuz=j;recordPosition();if let edition=edition {do {page=try state.content.index(edition).page(for:r)}catch{self.error="Rujukan tidak ditemukan."}};jump=false},onPage:{n in page=n;updateReference();jump=false})}
        .sheet(isPresented:$audioSettings){AudioRangeSheet(state:state,start:reference,onPlay:{a,b,e,p in audio.verses(qari:state.selectedQari,start:a,end:b,each:e,passes:p);audioSettings=false;controls=true})}
        .confirmationDialog("Ayat \(reference.id)",isPresented:$actions,titleVisibility:.visible){Button("Putar ayat ini"){audio.verses(qari:state.selectedQari,start:reference,end:reference)};Button("Arti, per kata & tafsir"){panel=true};Button("Tandai / hapus penanda"){state.mark(reference)};Button("Bagikan ayat"){share=true};Button("Salin ayat"){if let v=state.content.corpus.verse(reference){UIPasteboard.general.string=v.text+"\n"+v.indonesian+"\nQS "+reference.id}}}
        .sheet(isPresented:$share){if let verse=state.content.corpus.verse(reference){ShareSheet(text:verse.text+"\n"+verse.indonesian+"\nQS "+reference.id)}}
        .sheet(isPresented:$info){NavigationStack{PageContainer(dark:false){Text("Tajwid & sumber").font(.title.bold());Text("Gambar halaman mengikuti edisi sumber yang dipatok Nuraya. Warna asli cetak tidak selalu memakai legenda panel teks yang sama.");Text("Sumber: agusibrahim/quran-image. Terjemahan panel: Kemenag RI melalui QuranEnc. Arti per kata: Quran.com. Latin: eQuran.id.");if target.mode=="waqaf",let signs=try? state.content.decode("waqaf_guide.json",[WaqafItem].self){ForEach(signs){s in Text(s.mark+" — "+s.name).font(.title2);Text(s.explanation)}};Text("Halaman: \(page)/604. Ketuk halaman untuk menampilkan kontrol; geser kanan untuk lanjut; tekan lama ayat pada edisi berpeta.")}.toolbar{Button("Tutup"){info=false}}}}
        .onAppear{recordPosition()}
        .onDisappear{downloadingEdition=false;audio.stopReader();rotate(false)}
        .onChange(of:audio.track?.id){_ in guard state.settings.followAudio,audio.context == .reader,let r=audio.track?.verse?.reference else{return};if edition==nil && !sequence.references.contains(r){return};reference=r;recordPosition();if let edition=edition,let n=try? state.content.index(edition).page(for:r){page=n}}
    }
    func book(_ edition:MushafEdition)->some View {ZStack{
        Color.mushafPaper.ignoresSafeArea()
        if let image=image {MushafCanvas(image:image,regions:state.content.regions(edition,page),selected:audio.context == .reader ? audio.track?.verse?.reference:nil,zoom:zoom,tap:{withAnimation{controls.toggle()}},doubleTap:{landscape.toggle();rotate(landscape)},select:{r in reference=r;actions=true},turn:{turn($0)}).accessibilityIdentifier("mushaf_surface").saturation(state.settings.tajwid ? 1:0).ignoresSafeArea(edges:.bottom)}
        if hidden {Color.mushafPaper;Button("Tampilkan kembali halaman"){hidden=false}.font(.title3).padding()}
        if loading {ProgressView("Memuat halaman…").padding(20).background(Color.mushafPaper,in:RoundedRectangle(cornerRadius:18))}
        if let error=error {VStack(spacing:18){Text(error);Button("Coba lagi"){Task{await loadPage(edition)}};Button("Kembali"){dismiss()}}.padding(24).background(Color.mushafPaper,in:RoundedRectangle(cornerRadius:18))}
        if controls {VStack{HStack(spacing:10){Button{dismiss()}label:{AppIcon(name:"arrow-left").frame(width:44,height:44).accessibilityHidden(false).accessibilityLabel("Tutup Quran")};VStack(alignment:.leading){Text(mode?.titleId ?? edition.title).font(.system(size:15,weight:.semibold));Text("Halaman \(page) / 604").font(.system(size:13))};Spacer();Button{panel=true}label:{AppIcon(name:"language").frame(width:40,height:44)};Button{jump=true}label:{AppIcon(name:"search").frame(width:40,height:44)};readerMenu}.padding(.horizontal,8).background(Color(hex:0xF4ECD7));Spacer();if downloadingEdition {Text("Mengunduh edisi: \(downloadCount)/604").padding(12).background(Color(hex:0xF4ECD7));Button("Hentikan unduhan"){downloadingEdition=false}.padding(8).background(Color(hex:0xF4ECD7))};HStack(spacing:12){Button{info=true}label:{AppIcon(name:"info").frame(width:44,height:48)};Button{state.mark(reference)}label:{AppIcon(name:"bookmark").frame(width:44,height:48)};Button{audioSettings=true}label:{HStack{AppIcon(name:"settings-sliders");Text(state.selectedQari.name).font(.system(size:13)).lineLimit(2);Spacer();AppIcon(name:"angle-down",size:18)}};if audio.context == .reader,audio.track != nil {Button{audio.toggle()}label:{AppIcon(name:audio.playing ? "pause":"play").frame(width:44,height:48)}}}.padding(.horizontal,10).background(Color(hex:0xF4ECD7))}.foregroundStyle(Color(hex:0x625331))}
    }.statusBarHidden(!controls).task(id:page){await loadPage(edition)}}
    var readerMenu:some View {Menu{Button("Halaman berikutnya"){turn(1)};Button("Halaman sebelumnya"){turn(-1)};Button("Lanskap / potret"){landscape.toggle();rotate(landscape)};Button("Perbesar halaman"){zoom=min(3,zoom+0.5)};Button("Ukuran halaman semula"){zoom=1};Button("Layar baca penuh"){controls=false};Button("Simpan halaman offline"){if let edition=edition {Task{await loadPage(edition)}}};Button("Unduh 604 halaman edisi ini"){downloadEdition()};if target.mode=="hafalan_8"{Button("Uji hafalan tanpa melihat"){hidden.toggle()}};if target.mode=="tikrar" {Button("Catat pengulangan: \(tikrar)"){tikrar+=1};Button("Atur pengulangan audio"){audioSettings=true}};Button("Info tajwid & sumber"){info=true}}label:{AppIcon(name:"menu-dots-vertical").frame(width:40,height:44)}}
    var textReader:some View {
        let palette=Palette(dark:state.settings.dark)
        return ScrollView {
            VStack(spacing:24){
                VStack(spacing:8){
                    Text(state.content.corpus.surahs[reference.surah-1].name).font(.system(size:24,weight:.semibold))
                    Text(mode?.titleId ?? "Quran per ayat").font(.system(size:17)).foregroundStyle(palette.primary)
                    Text(reference.id).font(.system(size:16)).accessibilityIdentifier("reader_reference")
                    if let juz=readingJuz {Text("Juz \(juz)").font(.callout)}
                }.frame(maxWidth:.infinity)
                if let verse=state.content.corpus.verse(reference){
                    if let opening=verse.opening {ArabicText(text:opening,size:state.settings.arabicSize).padding(.vertical,12)}
                    ArabicText(text:verse.text,size:state.settings.arabicSize,study:state.content.study(reference),color:state.settings.tajwid,highlight:audio.context == .reader && audio.track?.verse?.reference==reference ? audio.word:nil)
                        .accessibilityIdentifier("reader_arabic")
                    if target.mode=="thematic" || state.settings.translation != "none" {
                        Text(target.mode != "thematic" && state.settings.translation=="en" ? verse.english:verse.indonesian)
                            .font(.system(size:19)).frame(maxWidth:.infinity,alignment:.leading).textSelection(.enabled)
                    }
                    Button("Arti per kata, Latin & tafsir"){panel=true}.font(.system(size:17)).frame(minHeight:48)
                }
            }.padding(24).frame(maxWidth:740).frame(maxWidth:.infinity)
        }.id(reference).background(palette.background.ignoresSafeArea()).foregroundStyle(palette.text)
            .simultaneousGesture(DragGesture(minimumDistance:55).onEnded {value in
                if abs(value.translation.width)>abs(value.translation.height)*1.5 {turnVerse(value.translation.width>0 ? 1:-1)}
            })
            .safeAreaInset(edge:.bottom){
                Button{readingMenu=true}label:{Label("Menu bacaan",systemImage:"slider.horizontal.3").font(.system(size:17,weight:.semibold)).padding(.horizontal,22).frame(minHeight:52)}
                    .buttonStyle(.bordered).tint(palette.primary).padding(.bottom,8).accessibilityIdentifier("reader_menu")
            }
    }
    private var textReadingMenu:some View {
        NavigationStack {
            PageContainer(dark:state.settings.dark){
                PrimaryButton(title:"Kembali ke daftar surah",icon:"arrow-left"){readingMenu=false;dismiss()}.accessibilityIdentifier("reader_close")
                HStack(spacing:12){
                    Button("Ayat sebelumnya"){turnVerse(-1)}.disabled(sequence.neighbor(of:reference,offset:-1)==nil).frame(maxWidth:.infinity,minHeight:52).accessibilityIdentifier("reader_previous")
                    Button("Ayat berikutnya"){turnVerse(1)}.disabled(sequence.neighbor(of:reference,offset:1)==nil).frame(maxWidth:.infinity,minHeight:52).accessibilityIdentifier("reader_next")
                }.buttonStyle(.bordered)
                Text("Ayat \(reference.id)").font(.headline)
                PrimaryButton(title:audio.context == .reader && audio.playing ? "Jeda bacaan":"Putar ayat",icon:audio.context == .reader && audio.playing ? "pause":"play"){
                    if audio.context == .reader && audio.playing {audio.pause()}
                    else {audio.verses(qari:state.selectedQari,start:reference,end:reference)}
                }
                Button{readingMenu=false;DispatchQueue.main.asyncAfter(deadline:.now()+0.4){audioSettings=true}}label:{MenuRow(title:"Qari & pengulangan",subtitle:state.selectedQari.name,icon:"headphones",dark:state.settings.dark)}
                Button{readingMenu=false;DispatchQueue.main.asyncAfter(deadline:.now()+0.4){jump=true}}label:{MenuRow(title:"Pindah surah atau ayat",icon:"search",dark:state.settings.dark)}
                Button{state.mark(reference)}label:{MenuRow(title:state.settings.entries.contains(where:{$0.kind=="bookmark" && $0.key==reference.id}) ? "Hapus penanda":"Tandai ayat",icon:"bookmark",dark:state.settings.dark)}.accessibilityIdentifier("reader_bookmark")
                Button("Bagikan ayat"){readingMenu=false;DispatchQueue.main.asyncAfter(deadline:.now()+0.4){share=true}}.frame(minHeight:48)
            }.navigationTitle("Menu bacaan").navigationBarTitleDisplayMode(.inline)
                .toolbar{ToolbarItem(placement:.navigationBarTrailing){Button("Tutup"){readingMenu=false}.accessibilityIdentifier("reader_menu_close")}}
        }
    }
    private func turnVerse(_ offset:Int){
        guard let next=sequence.neighbor(of:reference,offset:offset) else {return}
        reference=next;recordPosition()
    }
    func turn(_ delta:Int){guard (1...604).contains(page+delta) else{return};page+=delta;zoom=1;updateReference()}
    func updateReference(){if let edition=edition,let index=try? state.content.index(edition),let r=index.references(on:page).first {reference=r;recordPosition()}}
    func loadPage(_ edition:MushafEdition) async {loading=true;error=nil;image=nil;let n=page;do{let i=try await state.downloads.page(edition,n);if page==n{image=i}}catch{if page==n{self.error="Halaman belum terbuka: \(error.localizedDescription)"}};if page==n{loading=false}}
    func downloadEdition(){guard let edition=edition else{return};downloadingEdition=true;Task{for n in 1...604{guard downloadingEdition else{return};do{_=try await state.downloads.page(edition,n);downloadCount=n}catch{self.error="Unduhan berhenti pada halaman \(n). Halaman yang selesai tetap tersimpan.";downloadingEdition=false;return}};downloadingEdition=false}}
    func rotate(_ landscape:Bool){guard let scene=UIApplication.shared.connectedScenes.first as? UIWindowScene else{return};scene.requestGeometryUpdate(.iOS(interfaceOrientations:landscape ? .landscapeRight:.portrait))}
}
struct StudyPanel:View {
    @ObservedObject var state:AppState;let reference:Reference;@ObservedObject var audio:AudioService
    @State private var tab=0
    var body:some View {PageContainer(dark:state.settings.dark){if let v=state.content.corpus.verse(reference){Text(state.content.corpus.surahs[v.surah-1].name+" · "+reference.id).font(.title2.bold());ArabicText(text:v.text,size:state.settings.arabicSize,study:state.content.study(reference),color:state.settings.tajwid,highlight:audio.track?.verse?.reference==reference ? audio.word:nil);Picker("Bantuan baca",selection:$tab){Text("Arti").tag(0);Text("Per kata").tag(1);Text("Tafsir").tag(2);Text("Latin").tag(3)}.pickerStyle(.segmented)
        if tab==0 {Text(v.indonesian).font(.system(size:19)).textSelection(.enabled);if let notes=v.indonesianFootnotes{Text(notes).font(.footnote)};Text("Kementerian Agama RI · QuranEnc").font(.footnote).foregroundStyle(.secondary)}
        if tab==1,let study=state.content.study(reference){ForEach(study.words){w in HStack{Text(w.indonesian ?? w.english ?? "Arti belum tersedia");Spacer();Text(w.arabic).font(.custom("AmiriQuran-Regular",size:29))}.padding(14).background(Palette(dark:state.settings.dark).card,in:RoundedRectangle(cornerRadius:14))};Text("Arti kata: Quran.com").font(.footnote)}
        if tab==2 {Text(state.content.study(reference)?.tafsir ?? "Tafsir belum tersedia untuk rujukan ini.").font(.system(size:18)).textSelection(.enabled);Text(state.content.study(reference)?.tafsirSource ?? "Kemenag RI · eQuran.id").font(.footnote)}
        if tab==3 {Text(state.content.latin(reference) ?? "Latin belum tersedia.").font(.system(size:20));Text("Transliterasi eQuran.id. Latin membantu belajar; pelafalan tetap dipelajari melalui bacaan Arab dan guru.").font(.footnote)}
    }}.navigationTitle("Arti & bantuan baca")}
}
struct JumpSheet:View {
    @ObservedObject var state:AppState
    let edition:MushafEdition?,current:Reference,onSelect:(Reference,Int?)->Void,onPage:(Int)->Void
    @Environment(\.dismiss) private var dismiss
    @State private var choice=0;@State private var number="";@State private var query=""
    @State private var surah=1;@State private var ayah="1"
    var validReference:Reference? {
        guard let number=Int(ayah),state.content.corpus.verse(Reference(surah,number)) != nil else {return nil}
        return Reference(surah,number)
    }
    var body:some View {NavigationStack {
        PageContainer(dark:state.settings.dark){
            Picker("Tujuan",selection:$choice){if edition != nil{Text("Halaman").tag(0)};Text("Surah").tag(1);Text("Juz").tag(2);Text("Ayat").tag(3)}.pickerStyle(.segmented)
            if choice==0,edition != nil {
                TextField("Nomor 1–604",text:$number).keyboardType(.numberPad).textFieldStyle(.roundedBorder)
                PrimaryButton(title:"Buka halaman"){if let n=Int(number),(1...604).contains(n){onPage(n)}}.disabled(!(Int(number).map {(1...604).contains($0)} ?? false))
            }else if choice==2 {
                ForEach(1...30,id:\.self){j in Button("Juz \(j)"){if let v=state.content.corpus.verses.first(where:{$0.juz==j}){onSelect(v.reference,edition==nil ? j:nil)}}.frame(minHeight:48)}
            }else if choice==3 {
                Picker("Surah",selection:$surah){ForEach(state.content.corpus.surahs){s in Text("\(s.id). \(s.name)").tag(s.id)}}
                Text("Nomor ayat 1–\(state.content.corpus.surahs[surah-1].ayahCount)")
                TextField("Nomor ayat",text:$ayah).keyboardType(.numberPad).textFieldStyle(.roundedBorder)
                PrimaryButton(title:"Buka ayat"){if let ref=validReference {onSelect(ref,nil)}}.disabled(validReference==nil)
            }else {
                TextField("Cari surah",text:$query).textFieldStyle(.roundedBorder)
                ForEach(state.content.corpus.surahs.filter{query.isEmpty || Normalization.search($0.name).contains(Normalization.search(query)) || String($0.id)==query}){s in
                    Button{onSelect(Reference(s.id,1),nil)}label:{MenuRow(title:"\(s.id). \(s.name)",icon:"book-open-cover",dark:state.settings.dark)}
                }
            }
        }.navigationTitle("Buka bacaan")
            .toolbar{ToolbarItem(placement:.navigationBarTrailing){Button("Tutup"){dismiss()}}}
            .onAppear{if edition==nil{choice=1};surah=current.surah;ayah=String(current.ayah)}
    }}
}
struct AudioRangeSheet:View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var state:AppState;let start:Reference,onPlay:(Reference,Reference,Int,Int)->Void
    @State private var startSurah=1;@State private var startAyah=1;@State private var endSurah=1;@State private var endAyah=1;@State private var each=1;@State private var passes=1
    var valid:Bool {startSurah<endSurah || (startSurah==endSurah && startAyah<=endAyah)}
    var body:some View {NavigationStack{Form{Section("Qari"){Picker("Suara",selection:$state.settings.qari){ForEach(state.content.qaris){q in Text(q.name).tag(q.id)}}};Section("Awal"){surahPicker("Surah",selection:$startSurah);Stepper("Ayat \(startAyah)",value:$startAyah,in:1...state.content.corpus.surahs[startSurah-1].ayahCount)};Section("Akhir"){surahPicker("Surah",selection:$endSurah);Stepper("Ayat \(endAyah)",value:$endAyah,in:1...state.content.corpus.surahs[endSurah-1].ayahCount)};Section("Pengulangan"){Stepper("Ulang setiap ayat: \(each)×",value:$each,in:1...20);Stepper("Putaran rentang: \(passes)×",value:$passes,in:1...20);Text("Pengulangan per ayat dan seluruh rentang berbeda. Maksimal 20 pengulangan gabungan.")};Button("Mulai mendengarkan"){onPlay(Reference(startSurah,startAyah),Reference(endSurah,endAyah),each,passes)}.disabled(!valid || each*passes>20)}.navigationTitle("Audio bacaan").toolbar{ToolbarItem(placement:.navigationBarTrailing){Button("Tutup"){dismiss()}}}.onAppear{startSurah=start.surah;endSurah=start.surah;startAyah=start.ayah;endAyah=state.content.corpus.surahs[start.surah-1].ayahCount}.onChange(of:startSurah){_ in startAyah=min(startAyah,state.content.corpus.surahs[startSurah-1].ayahCount)}.onChange(of:endSurah){_ in endAyah=min(endAyah,state.content.corpus.surahs[endSurah-1].ayahCount)}}}
    func surahPicker(_ label:String,selection:Binding<Int>)->some View {Picker(label,selection:selection){ForEach(state.content.corpus.surahs){Text("\($0.id). \($0.name)").tag($0.id)}}}
}
