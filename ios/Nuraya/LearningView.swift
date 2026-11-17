import SwiftUI
import NurayaCore

struct LearningView:View {
    @ObservedObject var state:AppState
    @State private var tab=0
    @State private var level=0
    @State private var lesson:Lesson?
    @State private var replacing:Lesson?
    @State private var sessionOpen=false
    @State private var freePractice=false
    private var palette:Palette {Palette(dark:state.settings.dark)}
    private var lessons:[Lesson] {state.content.lessons.filter {$0.level==level && $0.kind==(tab==1 ? "memorize":"tajwid")}}
    private var today:String {let f=DateFormatter();f.locale=Locale(identifier:"en_US_POSIX");f.dateFormat="yyyy-MM-dd";return f.string(from:Date())}
    var body:some View {
        PageContainer(dark:state.settings.dark){
            tabs
            HStack(spacing:12){chip("\(state.settings.learning.xp) XP","trophy");chip("\(state.settings.learning.streak(today:today)) hari beruntun","calendar")}
            if tab==3 {progress}
            else if tab==2 {practice}
            else {path}
        }.navigationTitle("Belajar")
            .sheet(item:$lesson){selected in
                NavigationStack{LessonDetailView(state:state,lesson:selected,onStart:{requestStart(selected)}).toolbar{Button("Tutup"){lesson=nil}}}
            }
            .confirmationDialog("Ganti sesi latihan yang belum selesai?",isPresented:Binding(get:{replacing != nil},set:{if !$0{replacing=nil}}),titleVisibility:.visible){
                Button("Ganti sesi"){if let selected=replacing {replacing=nil;start(selected)}}
                Button("Lanjutkan sesi tersimpan"){replacing=nil;lesson=nil;presentSession()}
                Button("Batal",role:.cancel){replacing=nil}
            }message:{Text("Progres pelajaran yang sudah selesai tetap tersimpan. Jawaban sesi yang sedang berjalan akan diganti.")}
            .fullScreenCover(isPresented:$sessionOpen){LessonSessionView(state:state)}
            .fullScreenCover(isPresented:$freePractice){FreeMemorizationView(state:state)}
            .onDisappear{state.audio.stopReader()}
    }
    private var tabs:some View {
        HStack(spacing:0){ForEach(0..<4){index in
            Button{tab=index}label:{VStack(spacing:12){Text(["Belajar","Hafalan","Latihan","Progres"][index]).font(.system(size:17,weight:.semibold));Rectangle().fill(tab==index ? palette.primary:palette.raised).frame(height:3)}.frame(maxWidth:.infinity,minHeight:44)}
        }}
    }
    private func chip(_ title:String,_ icon:String)->some View {HStack(spacing:6){AppIcon(name:icon,size:19);Text(title).font(.system(size:14,weight:.medium))}.padding(.horizontal,12).padding(.vertical,9).background(palette.raised,in:RoundedRectangle(cornerRadius:14))}
    private var path:some View {
        VStack(alignment:.leading,spacing:18){
            Text(tab==1 ? "Akrab dengan surah pendek":"Tajwid, selangkah demi selangkah").font(.system(size:24,weight:.semibold))
            Text("Pilih materi yang kamu inginkan. Semua level terbuka.").font(.system(size:19)).foregroundStyle(palette.secondary)
            HStack(spacing:8){ForEach(0..<3){index in Button{level=index}label:{Text(["Dasar","Menengah","Lanjutan"][index]).font(.system(size:16,weight:.medium)).padding(.horizontal,16).frame(minHeight:50).background(level==index ? palette.raised:Color.clear,in:RoundedRectangle(cornerRadius:10)).overlay(RoundedRectangle(cornerRadius:10).stroke(palette.primary.opacity(level==index ? 0:0.5)))}}}
            if state.settings.learning.session != nil {PrimaryButton(title:"Lanjutkan sesi tersimpan",icon:"play"){sessionOpen=true}}
            HStack(spacing:16){AppIcon(name:tab==1 ? "book-open-cover":"graduation-cap",size:32);VStack(alignment:.leading,spacing:4){Text(["Kenali pola bacaan","Hubungkan dan terapkan","Perkuat pemahaman"][level]).font(.system(size:20,weight:.semibold));Text("\(lessons.count) pelajaran singkat · bebas dipilih").font(.system(size:16))}}.foregroundStyle(.white).padding(20).frame(maxWidth:.infinity,alignment:.leading).background(Color.nuraya,in:RoundedRectangle(cornerRadius:24))
            VStack(spacing:0){ForEach(Array(lessons.enumerated()),id:\.element.id){index,item in
                if index>0 {connector.padding(.leading,index%2==0 ? 45:65)}
                Button{lesson=item}label:{lessonRow(item)}.padding(.leading,index%2==0 ? 0:20)
            }}
            if tab==1 {Button{freePractice=true}label:{MenuRow(title:"Hafalan rentang bebas",subtitle:"Pilih ayat, dengarkan, lalu periksa dari ingatan",icon:"arrows-repeat-1",dark:state.settings.dark)}}
        }
    }
    private var connector:some View {Path{path in path.move(to:CGPoint(x:0,y:0));path.addCurve(to:CGPoint(x:18,y:38),control1:CGPoint(x:0,y:20),control2:CGPoint(x:18,y:18))}.stroke(palette.raised,style:StrokeStyle(lineWidth:5,lineCap:.round)).frame(width:25,height:38).frame(maxWidth:.infinity,alignment:.leading)}
    private func lessonRow(_ item:Lesson)->some View {
        let completed=state.settings.learning.records[item.id] != nil
        let steps=item.kind=="tajwid" ? 5:state.content.corpus.surahs[item.surah-1].ayahCount+3
        return HStack(spacing:16){
            AppIcon(name:completed ? "check-circle":item.kind=="tajwid" ? "graduation-cap":"book-open-cover",size:25).frame(width:56,height:56).background(palette.raised,in:Circle())
            VStack(alignment:.leading,spacing:5){Text(item.title).font(.system(size:18,weight:.semibold));Text(item.summary).font(.system(size:16)).foregroundStyle(palette.secondary);Text("\(steps) langkah · mulai kapan saja").font(.system(size:14,weight:.medium))}.frame(maxWidth:.infinity,alignment:.leading)
        }.padding(18).multilineTextAlignment(.leading).background(palette.card,in:RoundedRectangle(cornerRadius:24)).overlay(RoundedRectangle(cornerRadius:24).stroke(palette.primary.opacity(0.14)))
    }
    private var practice:some View {
        VStack(alignment:.leading,spacing:18){
            Text("Latihan sesuai kebutuhan").font(.title2.bold())
            Button{lesson=state.content.lessons.filter {$0.kind=="tajwid"}.randomElement()}label:{MenuRow(title:"Acak materi tajwid",subtitle:"Satu materi, lima langkah singkat",icon:"compass-alt",dark:state.settings.dark)}
            Button{freePractice=true}label:{MenuRow(title:"Hafalan rentang bebas",subtitle:"Pilih sendiri surah, ayat, dan pengulangan",icon:"book-open-cover",dark:state.settings.dark)}
            NavigationLink(value:Route.recitation){MenuRow(title:"AI bacaan Al-Qur’an",subtitle:"Periksa perkiraan kata yang terdengar",icon:"microphone",dark:state.settings.dark)}
        }
    }
    private var progress:some View {
        VStack(alignment:.leading,spacing:18){
            Text("Progres pribadi").font(.title2.bold())
            Text("\(state.settings.learning.records.count) pelajaran telah dicoba").font(.title3)
            Toggle("Target harian pribadi",isOn:$state.settings.learning.goalEnabled)
            if state.settings.learning.goalEnabled {Stepper("\(state.settings.learning.goal) pelajaran per hari",value:$state.settings.learning.goal,in:1...3);Text("Hari ini: \(state.settings.learning.daily[today] ?? 0) pelajaran")}
            Text("Lencana pribadi").font(.title2.bold())
            if state.settings.learning.badges.isEmpty {Text("Lencana muncul dari kegiatan belajarmu. Tidak ada kompetisi.")}
            ForEach(state.settings.learning.badges,id:\.self){badge in HStack{AppIcon(name:"badge-check");Text(badge)}}
            Text("Hari aktif: \(state.settings.learning.daily.count)")
            Text("\(state.settings.learning.reviews?.count ?? 0) ayat pernah dinilai mandiri")
            Text("XP mencatat aktivitas. Penilaian hafalan mandiri bukan pengukuran suara otomatis.").font(.footnote).foregroundStyle(palette.secondary)
        }
    }
    private func requestStart(_ selected:Lesson){
        if let active=state.settings.learning.session {
            if active.lessonID==selected.id {lesson=nil;presentSession()}else {replacing=selected}
        }else {start(selected)}
    }
    private func start(_ selected:Lesson){state.audio.stopReader();state.settings.learning.session=LessonSession(lesson:selected,course:state.content.lessons,corpus:state.content.corpus);lesson=nil;presentSession()}
    private func presentSession(){Task{try? await Task.sleep(nanoseconds:400_000_000);sessionOpen=true}}
}

private struct LessonDetailView:View {
    @ObservedObject var state:AppState
    let lesson:Lesson,onStart:()->Void
    var body:some View {
        PageContainer(dark:state.settings.dark){
            Text(lesson.title).font(.title.bold())
            Text(lesson.explanation).font(.system(size:19))
            if let verse=state.content.corpus.verse(Reference(lesson.surah,lesson.ayah)){
                ArabicText(text:verse.text,size:state.settings.arabicSize,study:state.content.study(verse.reference),color:state.settings.tajwid)
                Button("Dengarkan contoh qari"){state.audio.verses(qari:state.selectedQari,start:verse.reference,end:verse.reference)}
            }
            Text("Rujukan: "+lesson.sourceTitle).font(.footnote)
            if let url=URL(string:lesson.sourceUrl){Link("Buka sumber",destination:url)}
            PrimaryButton(title:"Mulai latihan",icon:"play",action:onStart)
        }.onDisappear{state.audio.stopReader()}
    }
}

struct FreeMemorizationView:View {
    @ObservedObject var state:AppState
    @Environment(\.dismiss) var dismiss
    @State private var start=Reference(112,1)
    @State private var end=Reference(112,4)
    @State private var cursor=0
    @State private var reveal=false
    @State private var choose=false
    @State private var rated=false
    @State private var each=1
    @State private var passes=1
    private var verses:[Verse] {(try? state.content.corpus.range(start,end,each:1,passes:1)) ?? []}
    var body:some View {
        NavigationStack{
            PageContainer(dark:state.settings.dark){
                Text("\(start.id) – \(end.id)").font(.title2.bold())
                Button("Ubah rentang & qari"){choose=true}
                PrimaryButton(title:"Dengarkan rentang",icon:"play"){state.audio.verses(qari:state.selectedQari,start:start,end:end,each:each,passes:passes)}
                if verses.indices.contains(cursor){versePractice(verses[cursor])}
                HStack{Button("Sebelumnya"){move(-1)}.disabled(cursor==0);Spacer();Text("\(cursor+1) / \(verses.count)");Spacer();Button("Berikutnya"){move(1)}.disabled(cursor+1>=verses.count)}
                Text("Catatan ini penilaian sendiri. Gunakan teks Arab dan bimbingan guru untuk memeriksa bacaan.").font(.footnote).foregroundStyle(.secondary)
            }.navigationTitle("Hafalan rentang bebas").toolbar{Button("Selesai"){dismiss()}}
        }.sheet(isPresented:$choose){AudioRangeSheet(state:state,start:start,onPlay:{a,b,e,p in start=a;end=b;each=e;passes=p;cursor=0;reveal=false;rated=false;choose=false;state.audio.verses(qari:state.selectedQari,start:a,end:b,each:e,passes:p)})}
            .onDisappear{state.audio.stopReader()}
    }
    private func versePractice(_ verse:Verse)->some View {
        VStack(alignment:.leading,spacing:18){
            Text(state.content.corpus.surahs[verse.surah-1].name+" · Ayat \(verse.ayah)").font(.headline)
            if reveal {ArabicText(text:verse.text,size:state.settings.arabicSize,study:state.content.study(verse.reference),color:state.settings.tajwid);Text(verse.indonesian)}
            else {Text("Coba baca ayat ini dari ingatan.").font(.title3).frame(maxWidth:.infinity,minHeight:150).background(Palette(dark:state.settings.dark).card,in:RoundedRectangle(cornerRadius:22))}
            PrimaryButton(title:reveal ? "Sembunyikan ayat":"Tampilkan ayat",icon:reveal ? "eye-crossed":"eye"){reveal.toggle()}
            Button("Dengarkan ayat ini"){state.audio.verses(qari:state.selectedQari,start:verse.reference,end:verse.reference)}
            if reveal {ForEach(["again","help","fluent"],id:\.self){rating in
                Button(["again":"Perlu mengulang","help":"Masih dibantu","fluent":"Lancar menurut saya"][rating]!){rate(verse.reference,rating)}.frame(maxWidth:.infinity,minHeight:48).disabled(rated)
            }}
            if rated {Text("Catatan tersimpan.").foregroundStyle(.secondary)}
        }
    }
    private func move(_ offset:Int){guard verses.indices.contains(cursor+offset) else{return};cursor+=offset;reveal=false;rated=false}
    private func rate(_ reference:Reference,_ rating:String){guard !rated else{return};let f=DateFormatter();f.locale=Locale(identifier:"en_US_POSIX");f.dateFormat="yyyy-MM-dd";state.settings.learning.review(reference,rating:rating,date:f.string(from:Date()));rated=true}
}
