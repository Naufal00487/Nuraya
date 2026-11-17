import SwiftUI
import NurayaCore
enum Route:Hashable {case settings,profile,search,hadith,learning,recitation,library,qari(String),qibla,location,sources}
struct RootView:View {
    @ObservedObject var state:AppState
    @ObservedObject private var reminders=ReminderService.shared
    @Environment(\.scenePhase) var scenePhase
    @State private var tab=0
    @State private var path=[Route]()
    @State private var locationSetup=false
    private var palette:Palette {Palette(dark:state.settings.dark)}
    var body:some View {
        Group {
            if state.settings.name.trimmingCharacters(in:.whitespacesAndNewlines).isEmpty {ProfileSetup(state:state)}
            else {
                VStack(spacing:0){
                    NavigationStack(path:$path){
                        mainPage.background(palette.background)
                            .navigationTitle(["Nuraya","Al-Qur’an","Murottal",state.translated("Ibadah","Prayer")][tab])
                            .navigationBarTitleDisplayMode(.inline)
                            .toolbar(tab==0 && path.isEmpty ? .hidden:.visible,for:.navigationBar)
                            .toolbar{ToolbarItem(placement:.navigationBarTrailing){NavigationLink(value:Route.settings){AppIcon(name:"settings-sliders").frame(width:44,height:44)}.accessibilityLabel("Pengaturan")}}
                            .navigationDestination(for:Route.self){destination($0)}
                    }
                    MiniPlayer(state:state,audio:state.audio)
                    tabBar
                }.background(palette.background)
            }
        }.tint(palette.primary).preferredColorScheme(state.settings.dark ? .dark:.light)
            .environment(\.locale,Locale(identifier:state.settings.language))
            .task(id:state.settings.name){
                guard !state.settings.name.trimmingCharacters(in:.whitespacesAndNewlines).isEmpty,state.settings.latitude==nil else{return}
                #if DEBUG
                if ProcessInfo.processInfo.arguments.contains("--ui-testing"){return}
                #endif
                locationSetup=true
            }
            .sheet(isPresented:$locationSetup,onDismiss:{Task{if (try? await ReminderService.shared.request())==true {try? await ReminderService.shared.schedule(state:state)}}}){
                NavigationStack{LocationView(state:state,location:state.location)}.interactiveDismissDisabled()
                    .onAppear{state.location.request()}
                    .onChange(of:state.settings.latitude){_ in if state.settings.latitude != nil && state.settings.longitude != nil {locationSetup=false}}
            }
            .fullScreenCover(item:$state.reader,onDismiss:{state.audio.stopReader()}){target in ReaderView(state:state,target:target,audio:state.audio)}
            .fullScreenCover(isPresented:$state.fullPlayer){PlayerView(state:state,audio:state.audio)}
            .alert("Nuraya",isPresented:Binding(get:{state.error != nil},set:{if !$0 {state.error=nil}})){Button("Tutup",role:.cancel){state.error=nil}}message:{Text(state.error ?? "")}
            .onChange(of:reminders.pendingDestination){_ in routeNotification()}
            .onAppear{routeNotification()}
            .onChange(of:scenePhase){phase in
                if phase != .active {state.audio.stopReader();if phase == .background {state.speech.stop()}}
                else {Task{try? await ReminderService.shared.schedule(state:state)}}
            }
    }
    @ViewBuilder private var mainPage:some View {
        switch tab {
        case 1:QuranModesView(state:state)
        case 2:MurottalView(state:state)
        case 3:PrayerView(state:state)
        default:HomeView(state:state,selectTab:{tab=$0})
        }
    }
    private var tabBar:some View {
        HStack(spacing:0){ForEach(0..<4){index in
            Button{path=[];tab=index}label:{
                VStack(spacing:6){AppIcon(name:["home","book-quran","headphones","compass-alt"][index],size:26);Text([state.translated("Beranda","Home"),"Al-Qur’an","Murottal",state.translated("Ibadah","Prayer")][index]).font(.system(size:14,weight:tab==index ? .semibold:.regular))}
                    .foregroundStyle(tab==index ? palette.primary:palette.secondary).frame(maxWidth:.infinity,minHeight:62)
            }.buttonStyle(.plain).accessibilityIdentifier("tab_\(index)")
        }}.padding(.horizontal,10).padding(.top,6).background(.ultraThinMaterial)
    }
    @ViewBuilder private func destination(_ route:Route)->some View {
        switch route {
        case .settings:SettingsView(state:state)
        case .profile:ProfileView(state:state)
        case .search:SearchView(state:state)
        case .hadith:HadithView(state:state)
        case .learning:LearningView(state:state)
        case .recitation:RecitationView(state:state)
        case .library:LibraryView(state:state)
        case .qari(let id):QariSurahsView(state:state,qari:state.content.qaris.first {$0.id==id} ?? state.selectedQari)
        case .qibla:QiblaView(state:state,location:state.location)
        case .location:LocationView(state:state,location:state.location)
        case .sources:SourcesView(state:state)
        }
    }
    private func routeNotification(){guard let route=reminders.pendingDestination else{return};state.reader=nil;state.fullPlayer=false;path=[];tab=route=="prayer" ? 3:1;reminders.pendingDestination=nil}
}
struct ProfileSetup:View {
    @ObservedObject var state:AppState;@State private var name="";@State private var avatar=0
    var body:some View {PageContainer(dark:state.settings.dark){AssetImage(content:state.content,path:"brand/logo.jpg",fit:true).frame(height:150).clipShape(RoundedRectangle(cornerRadius:24));Text("Selamat datang di Nuraya").font(.system(size:30,weight:.bold));Text("Lengkapi profil pribadimu untuk mulai membaca dan belajar.").font(.system(size:19));AvatarPicker(state:state,selected:$avatar);TextField("Nama panggilan",text:$name).textContentType(.nickname).padding(18).background(Palette(dark:state.settings.dark).raised,in:RoundedRectangle(cornerRadius:16));PrimaryButton(title:"Mulai menggunakan Nuraya",icon:"arrow-right"){state.settings.avatar=avatar;state.settings.name=String(name.trimmingCharacters(in:.whitespacesAndNewlines).prefix(60))}.disabled(name.trimmingCharacters(in:.whitespacesAndNewlines).isEmpty);Text("Profil disimpan di iPhone ini. Tidak memerlukan akun online.").font(.callout).foregroundStyle(.secondary)}}
}
struct AvatarPicker:View {
    @ObservedObject var state:AppState;@Binding var selected:Int
    var body:some View {HStack(spacing:18){ForEach(0..<3){i in Button{selected=i}label:{AssetImage(content:state.content,path:"brand/avatars/avatar_\(i).png",fit:true).frame(width:78,height:78).clipShape(Circle()).overlay(Circle().stroke(selected==i ? Color.nuraya:Color.clear,lineWidth:3))}.accessibilityLabel("Avatar \(i+1)")}}.frame(maxWidth:.infinity)}
}
