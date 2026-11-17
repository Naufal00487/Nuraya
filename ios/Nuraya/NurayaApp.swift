import SwiftUI
import CoreText
@main struct NurayaApp:App {
    @State private var state:AppState?;@State private var loadError:String?
    var body:some Scene {WindowGroup {Group {
        if let state=state {RootView(state:state)}
        else {VStack(spacing:24){if let base=Bundle.main.resourceURL,let image=UIImage(contentsOfFile:base.appendingPathComponent("Content/brand/logo.jpg").path){Image(uiImage:image).resizable().scaledToFit().frame(width:130,height:130).clipShape(RoundedRectangle(cornerRadius:28))};Text("Nuraya").font(.largeTitle.bold());if let error=loadError{Text(error).multilineTextAlignment(.center)}else {ProgressView("Menyiapkan bacaan…")}}.padding(28).frame(maxWidth:.infinity,maxHeight:.infinity).background(Color(hex:0xF6F8FC))}
    }.task {
        guard state==nil,loadError==nil else {return}
        do {
            guard let base=Bundle.main.resourceURL else {throw ContentError.invalidCorpus}
            let root=base.appendingPathComponent("Content")
            CTFontManagerRegisterFontsForURL(root.appendingPathComponent("amiri_quran.ttf") as CFURL,.process,nil)
            let content=try await Task.detached(priority:.userInitiated){try ContentStore(root:root)}.value
            state=try AppState(content:content);UNUserNotificationCenter.current().delegate=ReminderService.shared
        }catch {loadError="Data Nuraya belum dapat dibuka. \(error.localizedDescription)"}
    }}}
}
import NurayaCore
import UserNotifications
