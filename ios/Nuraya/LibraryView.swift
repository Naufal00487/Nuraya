import SwiftUI
import NurayaCore

private enum CollectionCategory:String,CaseIterable,Identifiable {
    case bookmark,favorite,playlist,download,history
    var id:String {rawValue}
    var title:String {switch self {case .bookmark:return "Penanda";case .favorite:return "Favorit";case .playlist:return "Daftar putar";case .download:return "Unduhan";case .history:return "Riwayat"}}
    var icon:String {switch self {case .bookmark:return "bookmark";case .favorite:return "heart";case .playlist:return "list-music";case .download:return "download";case .history:return "clock"}}
    var guidance:String {switch self {
        case .bookmark:return "Ketuk ikon penanda pada ayat saat membaca Al-Qur’an."
        case .favorite:return "Tandai surah favorit melalui tombol Favorit di pemutar murottal."
        case .playlist:return "Tambahkan surah melalui tombol Koleksi di pemutar murottal."
        case .download:return "Unduh surah dari pemutar Murottal untuk didengarkan tanpa internet."
        case .history:return "Bacaan dan audio yang diputar akan tercatat di sini."
    }}
    func includes(_ entry:SavedEntry)->Bool {self == .history ? entry.kind.hasSuffix("history"):entry.kind==rawValue}
}

struct LibraryView:View {
    @ObservedObject var state:AppState
    @ObservedObject var downloads:DownloadStore
    @State private var category=CollectionCategory.bookmark
    @State private var clearHistory=false
    @State private var deletingDownload:OfflineRow?
    private var palette:Palette {Palette(dark:state.settings.dark)}
    init(state:AppState){self.state=state;downloads=state.downloads}
    private var entries:[SavedEntry] {state.settings.entries.filter(category.includes).sorted {$0.date>$1.date}}
    var body:some View {
        PageContainer(dark:state.settings.dark){
            Text("Koleksi dan unduhan Anda").font(.system(size:27,weight:.semibold))
            LazyVGrid(columns:[GridItem(.adaptive(minimum:104),spacing:8)],spacing:8){
                ForEach(CollectionCategory.allCases){item in
                    Button{category=item}label:{Text(item.title).font(.system(size:16,weight:.semibold))
                        .frame(maxWidth:.infinity,minHeight:48)
                        .foregroundStyle(category==item ? palette.background:palette.primary)
                        .background(category==item ? palette.primary:palette.card,in:Capsule())
                        .overlay(Capsule().stroke(palette.primary.opacity(0.25)))
                    }.buttonStyle(.plain).accessibilityAddTraits(category==item ? .isSelected:[])
                        .accessibilityIdentifier("library_filter_"+item.id)
                }
            }
            if category == .download {offlineSection}
            else {
                if entries.isEmpty {emptyState}
                else {
                    HStack{
                        Text("\(entries.count) \(category.title.lowercased())").font(.headline)
                        Spacer()
                        if category == .history {Button("Hapus riwayat",role:.destructive){clearHistory=true}.frame(minHeight:48)}
                    }
                    if category == .playlist {PrimaryButton(title:"Putar koleksi",icon:"play"){state.audio.collection(Array(entries.reversed()));state.fullPlayer=true}}
                    ForEach(entries){entry in savedRow(entry)}
                }
            }
        }.navigationTitle("Koleksi saya")
        .confirmationDialog("Hapus riwayat membaca dan mendengarkan?",isPresented:$clearHistory,titleVisibility:.visible){
            Button("Hapus riwayat",role:.destructive){state.settings.entries.removeAll {$0.kind.hasSuffix("history")}}
        }message:{Text("Penanda, favorit, daftar putar, dan unduhan tetap tersimpan.")}
        .alert("Hapus unduhan?",isPresented:Binding(get:{deletingDownload != nil},set:{if !$0 {deletingDownload=nil}})){
            Button("Batal",role:.cancel){deletingDownload=nil}
            Button("Hapus",role:.destructive){if let row=deletingDownload {do{try downloads.remove(row.url)}catch{state.error=error.localizedDescription}};deletingDownload=nil}
        }message:{Text("Audio akan dihapus dari iPhone ini. Kamu dapat mengunduhnya lagi.")}
    }
    private var emptyState:some View {
        VStack(spacing:16){
            AppIcon(name:category.icon,size:38).foregroundStyle(palette.primary)
                .frame(width:80,height:80).background(palette.raised,in:Circle())
            Text(category == .download ? "Belum ada unduhan":"Ruang untuk pilihan Anda")
                .font(.system(size:22,weight:.semibold))
            Text(category.guidance).font(.system(size:18)).foregroundStyle(palette.secondary)
        }.multilineTextAlignment(.center).padding(28).frame(maxWidth:.infinity)
            .background(palette.card,in:RoundedRectangle(cornerRadius:22))
            .accessibilityIdentifier("library_empty_"+category.id)
    }
    private func savedRow(_ entry:SavedEntry)->some View {
        HStack(spacing:8){
            Button{open(entry)}label:{
                HStack(spacing:14){
                    AppIcon(name:entry.kind=="bookmark" || entry.kind=="read_history" ? "book-open-cover":"headphones",size:25)
                        .frame(width:46,height:46).background(palette.raised,in:RoundedRectangle(cornerRadius:14))
                    VStack(alignment:.leading,spacing:6){
                        Text(entry.title).font(.system(size:20,weight:.semibold))
                        Text(entry.subtitle).font(.system(size:16)).foregroundStyle(palette.secondary)
                        if let mode=entry.reading?.mode,let title=state.content.modes.first(where:{$0.id==mode})?.titleId {
                            Text(title).font(.system(size:14)).foregroundStyle(palette.secondary)
                        }
                        if category == .history {Text(entry.date,style:.date).font(.system(size:14)).foregroundStyle(palette.secondary)}
                    }.fixedSize(horizontal:false,vertical:true)
                    Spacer(minLength:0)
                }.padding(.vertical,16).padding(.leading,16).frame(maxWidth:.infinity,alignment:.leading).contentShape(Rectangle())
            }.buttonStyle(.plain).accessibilityIdentifier("library_entry_"+entry.kind+"_"+entry.key)
            Button(role:.destructive){state.settings.entries.removeAll {$0.id==entry.id}}label:{
                AppIcon(name:"trash",size:21).frame(width:48,height:52)
            }.accessibilityLabel("Hapus "+entry.title)
        }.foregroundStyle(palette.text).background(palette.card,in:RoundedRectangle(cornerRadius:18))
    }
    private var offlineSection:some View {
        let rows=offline
        return VStack(alignment:.leading,spacing:16){
            Text("\(rows.count) audio offline").font(.title2.bold())
            Text("Tersimpan di aplikasi. Menghapus aplikasi juga menghapus unduhannya.").font(.callout).foregroundStyle(palette.secondary)
            if rows.isEmpty {emptyState}
            ForEach(rows){row in
                VStack(alignment:.leading,spacing:14){
                    Text(state.content.corpus.surahs[row.surah-1].name).font(.system(size:21,weight:.semibold))
                    Text(row.qari.name).font(.system(size:17)).foregroundStyle(palette.secondary)
                    Label("Siap didengarkan offline",systemImage:"checkmark.circle").font(.callout)
                    HStack(spacing:16){
                        Button{state.audio.chapter(row.qari,row.surah);state.fullPlayer=true}label:{Label("Putar",systemImage:"play.fill").frame(minWidth:96,minHeight:48)}.buttonStyle(.borderedProminent)
                        Button(role:.destructive){deletingDownload=row}label:{Label("Hapus",systemImage:"trash").frame(minWidth:96,minHeight:48)}.buttonStyle(.bordered)
                    }
                }.padding(18).frame(maxWidth:.infinity,alignment:.leading).background(palette.card,in:RoundedRectangle(cornerRadius:18))
            }
            Text("Ruang unduhan: \(Double(downloads.size())/1_000_000,specifier:"%.1f") MB").font(.footnote)
            Text("Halaman mushaf dan model AI tersimpan terpisah dari daftar audio surah.").font(.footnote).foregroundStyle(palette.secondary)
        }
    }
    private struct OfflineRow:Identifiable {let id:String,qari:ReciterSelection,surah:Int,url:URL}
    private var offline:[OfflineRow]{state.content.qaris.flatMap {q in
        guard let r=state.content.chapterReciters.first(where:{$0.id==q.chapterId}) else{return [OfflineRow]()}
        return (1...114).compactMap {s in guard let u=r.url(s),downloads.cached(u) != nil else{return nil};return OfflineRow(id:q.id+"|\(s)",qari:q,surah:s,url:u)}
    }}
    private func reference(_ value:String)->Reference? {let p=value.split(separator:":");guard p.count==2,let s=Int(p[0]),let a=Int(p[1]),state.content.corpus.verse(Reference(s,a)) != nil else{return nil};return Reference(s,a)}
    private func open(_ entry:SavedEntry){
        if entry.kind=="bookmark" || entry.kind=="read_history" {
            if let ref=reference(entry.key){state.open(ref,mode:entry.reading?.mode,juz:entry.reading?.juz)}
            else {state.error="Rujukan ayat pada koleksi ini tidak tersedia."}
            return
        }
        let parts=entry.key.split(separator:"|").map(String.init)
        guard parts.count==2,let qari=state.content.qaris.first(where:{$0.id==parts[0]}) else {state.error="Qari pada koleksi ini tidak tersedia.";return}
        if entry.kind=="verse_audio_history",let ref=reference(parts[1]) {state.settings.qari=qari.id;state.open(ref);state.audio.verses(qari:qari,start:ref,end:ref)}
        else if let surah=Int(parts[1]),(1...114).contains(surah){state.audio.chapter(qari,surah);state.fullPlayer=true}
        else {state.error="Rujukan audio pada koleksi ini tidak tersedia."}
    }
}
