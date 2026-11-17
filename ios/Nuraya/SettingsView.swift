import SwiftUI
import NurayaCore

struct SettingsView:View {
    @ObservedObject var state:AppState
    @State private var panel:String?
    @State private var query=""
    private var palette:Palette {Palette(dark:state.settings.dark)}
    private func tr(_ id:String,_ en:String)->String {state.translated(id,en)}
    var body:some View {
        PageContainer(dark:state.settings.dark){
            SettingsGroup(title:"Al-Qur’an",dark:state.settings.dark){
                menu(tr("Qari pilihan","Preferred reciter"),state.selectedQari.name,"headphones"){panel="qari"}
                separator
                menu(tr("Terjemahan","Translation"),state.settings.translation=="en" ? "English":state.settings.translation=="none" ? tr("Tanpa terjemahan","No translation"):"Indonesia","language"){panel="translation"}
                separator
                menu(tr("Ukuran tulisan Arab","Arabic text size"),"\(Int(state.settings.arabicSize)) · "+tr("lihat contoh tulisan","preview text"),"universal-access"){panel="arabic"}
                separator
                toggle(tr("Warna tajwid","Tajwid colors"),tr("Penanda warna hukum bacaan","Color cues for reading rules"),"palette",$state.settings.tajwid)
                separator
                toggle(tr("Ikuti lantunan ayat","Follow recitation"),tr("Halaman mengikuti ayat yang diputar","Follow the verse being recited"),"waveform-path",$state.settings.followAudio)
            }
            SettingsGroup(title:tr("Tampilan","Appearance"),dark:state.settings.dark){
                toggle(tr("Tampilan gelap","Dark appearance"),tr("Warna nyaman untuk malam hari","Comfortable colors for evening reading"),"moon",$state.settings.dark)
                separator
                menu(tr("Bahasa aplikasi","App language"),state.settings.language=="en" ? "English":"Indonesia","globe"){panel="language"}
            }
            SettingsGroup(title:tr("Pengingat","Reminders"),dark:state.settings.dark){
                NavigationLink{ReminderSettingsView(state:state)}label:{row(tr("Notifikasi & alarm","Notifications & alarms"),tr("Pengingat Quran, salat, dan suara azan","Quran reminders, prayer and adhan audio"),"bell")}
            }
            SettingsGroup(title:tr("Aplikasi","Application"),dark:state.settings.dark){
                NavigationLink(value:Route.profile){row(tr("Profil saya","My profile"),state.settings.name,"user")}
                separator
                menu(tr("Privasi & penyimpanan","Privacy & storage"),tr("Data pribadi disimpan di HP","Personal data stays on your phone"),"shield-check"){panel="privacy"}
                separator
                NavigationLink(value:Route.sources){row(tr("Sumber & atribusi","Sources & attribution"),tr("Sumber bacaan, audio, dan desain","Reading, audio and design sources"),"badge-check")}
                separator
                NavigationLink(value:Route.search){row(tr("Asisten pembelajaran","Learning companion"),tr("Cari ayat, makna, dan rujukan","Find verses, meaning and references"),"graduation-cap")}
                separator
                menu(tr("Tentang Nuraya","About Nuraya"),tr("Kenali aplikasi dan penyusunnya","About the app and its team"),"info"){panel="about"}
            }
            Text("Nuraya 1.7.1 · iOS").font(.footnote).foregroundStyle(palette.secondary).frame(maxWidth:.infinity)
        }.navigationTitle(tr("Pengaturan","Settings"))
            .sheet(isPresented:Binding(get:{panel != nil},set:{if !$0{panel=nil}})){
                NavigationStack{PageContainer(dark:state.settings.dark){panelContent}.toolbar{ToolbarItem(placement:.navigationBarTrailing){Button(tr("Tutup","Close")){panel=nil}}}}
            }
    }
    private var separator:some View {Divider().padding(.leading,58)}
    private func row(_ title:String,_ subtitle:String,_ icon:String)->some View {
        HStack(spacing:14){SettingsLabel(title:title,subtitle:subtitle,icon:icon,dark:state.settings.dark);AppIcon(name:"angle-right",size:20)}.padding(.vertical,12).frame(minHeight:76)
    }
    private func menu(_ title:String,_ subtitle:String,_ icon:String,action:@escaping ()->Void)->some View {Button(action:action){row(title,subtitle,icon)}.buttonStyle(.plain)}
    private func toggle(_ title:String,_ subtitle:String,_ icon:String,_ value:Binding<Bool>)->some View {
        Toggle(isOn:value){SettingsLabel(title:title,subtitle:subtitle,icon:icon,dark:state.settings.dark)}.padding(.vertical,12).frame(minHeight:76)
    }
    @ViewBuilder private var panelContent:some View {
        switch panel {
        case "qari":
            Text(tr("Qari pilihan","Preferred reciter")).font(.title2.bold())
            TextField(tr("Cari qari","Search reciters"),text:$query).textFieldStyle(.roundedBorder)
            ForEach(state.content.qaris.filter {query.isEmpty || Normalization.search($0.name).contains(Normalization.search(query))}){qari in
                Button{state.settings.qari=qari.id;panel=nil}label:{HStack(spacing:14){QariPhoto(content:state.content,qari:qari,size:60,rounded:30);Text(qari.name).font(.system(size:19));Spacer();if state.settings.qari==qari.id {AppIcon(name:"check-circle")}}.frame(minHeight:76)}
            }
        case "translation":
            Text(tr("Terjemahan Al-Qur’an","Qur’an translation")).font(.title2.bold())
            Picker(tr("Terjemahan","Translation"),selection:$state.settings.translation){Text("Indonesia").tag("id");Text("English").tag("en");Text(tr("Tanpa","None")).tag("none")}.pickerStyle(.segmented)
        case "arabic":
            Text(tr("Ukuran tulisan Arab","Arabic text size")).font(.title2.bold())
            Stepper("\(Int(state.settings.arabicSize))",value:$state.settings.arabicSize,in:22...54,step:2)
            ArabicText(text:OpeningPolicy.basmalah,size:state.settings.arabicSize)
        case "language":
            Text(tr("Bahasa aplikasi","App language")).font(.title2.bold())
            Picker(tr("Bahasa","Language"),selection:$state.settings.language){Text("Indonesia").tag("id");Text("English").tag("en")}.pickerStyle(.segmented)
        case "privacy":
            Text(tr("Privasi & penyimpanan","Privacy & storage")).font(.title2.bold())
            Text(tr("Nama, avatar, pilihan, penanda dan progres berada di iPhone ini. Nuraya tidak memakai akun cloud. Menghapus aplikasi dapat menghapus data pribadi dan unduhan.","Your name, avatar, preferences, bookmarks and progress stay on this iPhone. Nuraya has no cloud account. Removing the app may remove personal data and downloads."))
            Text(tr("AI lokal tidak menyimpan rekaman atau transkrip. Dikte Apple dapat memakai jaringan setelah kamu mengizinkannya. Streaming, unduhan dan nama daerah memerlukan koneksi.","Local AI does not store recordings or transcripts. Apple dictation can use online services with your permission. Streaming, downloads and place names need a connection."))
            Text(tr("Ruang unduhan","Download storage")+String(format:": %.1f MB",Double(state.downloads.size())/1_000_000))
            NavigationLink{LibraryView(state:state)}label:{Text(tr("Kelola koleksi & unduhan","Manage collection and downloads"))}
        case "about":
            AssetImage(content:state.content,path:"brand/logo.jpg",fit:true).frame(height:160)
            Text("Nuraya").font(.largeTitle.bold())
            Text(tr("Pendamping membaca Al-Qur’an, belajar, dan ibadah.","A companion for reading the Qur’an, learning and worship."))
            Text(tr("Kami penyusun aplikasi tidak mengambil keuntungan dari aplikasi ini.","We do not take a profit from this application."))
            Text(tr("Desain visual dan rancangan fitur: Naufal Irfan Ramadhan.","Visual design and feature planning: Naufal Irfan Ramadhan."))
            Text(tr("Acuan konten: Android 1.7.1. Versi iOS untuk pengujian pribadi.","Content baseline: Android 1.7.1. iOS version for personal testing.")).font(.footnote)
        default:EmptyView()
        }
    }
}
private struct SettingsGroup<Content:View>:View {
    let title:String,dark:Bool
    @ViewBuilder var content:()->Content
    var body:some View {VStack(alignment:.leading,spacing:12){Text(title).font(.system(size:20,weight:.semibold)).padding(.leading,4);VStack(spacing:0,content:content).padding(.horizontal,18).padding(.vertical,6).background(Palette(dark:dark).card,in:RoundedRectangle(cornerRadius:26))}.padding(.bottom,6)}
}
private struct SettingsLabel:View {
    let title:String,subtitle:String,icon:String,dark:Bool
    var body:some View {HStack(spacing:14){AppIcon(name:icon,size:24).frame(width:44,height:44).background(Palette(dark:dark).background,in:RoundedRectangle(cornerRadius:13));VStack(alignment:.leading,spacing:5){Text(title).font(.system(size:18,weight:.semibold));Text(subtitle).font(.system(size:16)).foregroundStyle(Palette(dark:dark).secondary).fixedSize(horizontal:false,vertical:true)}.frame(maxWidth:.infinity,alignment:.leading)}.multilineTextAlignment(.leading)}
}
