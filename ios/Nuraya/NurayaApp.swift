import SwiftUI
import CoreText
import NurayaCore
import UserNotifications

@main struct NurayaApp:App {
    @State private var state:AppState?
    @State private var loadError:String?
    var body:some Scene {WindowGroup {
        Group {
            if let state=state {RootView(state:state)}
            else {LaunchArtwork(loadError:loadError)}
        }.task {
            guard state==nil,loadError==nil else {return}
            let started=Date()
            do {
                guard let base=Bundle.main.resourceURL else {throw ContentError.invalidCorpus}
                let root=base.appendingPathComponent("Content")
                CTFontManagerRegisterFontsForURL(root.appendingPathComponent("amiri_quran.ttf") as CFURL,.process,nil)
                let content=try await Task.detached(priority:.userInitiated){try ContentStore(root:root)}.value
                let loaded=try AppState(content:content)
                // Match Android's 700 ms launch artwork, without delaying a slower load.
                var minimum=0.7
                #if DEBUG
                if ProcessInfo.processInfo.arguments.contains("--capture-launch"){minimum=3}
                #endif
                let remaining=minimum-Date().timeIntervalSince(started)
                if remaining>0 {try await Task.sleep(nanoseconds:UInt64(remaining*1_000_000_000))}
                state=loaded
                UNUserNotificationCenter.current().delegate=ReminderService.shared
            }catch {loadError="Data Nuraya belum dapat dibuka. \(error.localizedDescription)"}
        }
    }}
}

struct LaunchArtwork:View {
    var loadError:String?
    var body:some View {
        ZStack(alignment:.bottom){
            Color.white.ignoresSafeArea()
            Image("LaunchArtwork").resizable().scaledToFit()
                .frame(maxWidth:.infinity,maxHeight:.infinity)
                .accessibilityLabel("Dua maskot Nuraya")
                .accessibilityIdentifier("launch_artwork")
            if let error=loadError {
                Text(error).multilineTextAlignment(.center).padding(20)
                    .foregroundStyle(Color.nuraya).background(.white,in:RoundedRectangle(cornerRadius:18)).padding()
            }
        }.preferredColorScheme(.light)
    }
}
