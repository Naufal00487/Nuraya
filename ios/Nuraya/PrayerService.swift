import Foundation
import CoreLocation
import UserNotifications
import Adhan
import NurayaCore
@MainActor final class LocationService:NSObject,ObservableObject,CLLocationManagerDelegate {
    private let manager=CLLocationManager()
    @Published var coordinate:CLLocationCoordinate2D?,name="",heading:Double?,accuracy:Double?,error:String?,busy=false
    override init() {super.init();manager.delegate=self;manager.desiredAccuracy=kCLLocationAccuracyHundredMeters;manager.headingFilter=1}
    func request() {
        error=nil;busy=true
        switch manager.authorizationStatus {
        case .notDetermined:manager.requestWhenInUseAuthorization()
        case .authorizedAlways,.authorizedWhenInUse:manager.requestLocation()
        default:busy=false;error="Izin lokasi belum aktif. Buka Pengaturan iPhone atau isi lokasi manual."
        }
    }
    func locationManagerDidChangeAuthorization(_ manager:CLLocationManager) {if busy && [.authorizedAlways,.authorizedWhenInUse].contains(manager.authorizationStatus) {manager.requestLocation()}else if manager.authorizationStatus == .denied {busy=false;error="Izin lokasi tidak diberikan."}}
    func locationManager(_ manager:CLLocationManager,didUpdateLocations locations:[CLLocation]) {
        guard let last=locations.last,abs(last.timestamp.timeIntervalSinceNow)<300,last.horizontalAccuracy>=0 else {busy=false;error="Koordinat terkini belum tersedia. Coba lagi.";return}
        coordinate=last.coordinate;busy=false
        CLGeocoder().reverseGeocodeLocation(last) {[weak self] places,_ in Task {@MainActor in self?.name=places?.first?.subAdministrativeArea ?? places?.first?.locality ?? "Lokasi HP"}}
    }
    func locationManager(_ manager:CLLocationManager,didFailWithError error:Error) {busy=false;self.error="Lokasi belum didapat: \(error.localizedDescription)"}
    func startCompass() {guard CLLocationManager.headingAvailable() else {error="Sensor kompas tidak tersedia. Gunakan arah numerik.";return};manager.startUpdatingHeading()}
    func stopCompass() {manager.stopUpdatingHeading()}
    func locationManager(_ manager:CLLocationManager,didUpdateHeading newHeading:CLHeading) {accuracy=newHeading.headingAccuracy;heading=newHeading.trueHeading>=0 ? newHeading.trueHeading:nil}
    func locationManagerShouldDisplayHeadingCalibration(_ manager:CLLocationManager)->Bool {true}
}
struct PrayerMoment:Identifiable {let id:String,name:String,date:Date}
enum PrayerService {
    static func day(_ date:Date,settings:AppSettings)->[PrayerMoment] {
        guard let lat=settings.latitude,let lon=settings.longitude,(-89...89).contains(lat),(-180...180).contains(lon) else {return []}
        var calendar=Calendar(identifier:.gregorian);calendar.timeZone=TimeZone(identifier:settings.timeZone) ?? .current
        let components=calendar.dateComponents([.year,.month,.day],from:date)
        var params=CalculationMethod.other.params;params.fajrAngle=20;params.ishaAngle=18
        params.madhab = .shafi;params.highLatitudeRule = .twilightAngle
        let correction=settings.prayerCorrection
        params.adjustments.fajr=correction;params.adjustments.dhuhr=correction;params.adjustments.asr=correction;params.adjustments.maghrib=correction;params.adjustments.isha=correction
        guard let p=PrayerTimes(coordinates:Coordinates(latitude:lat,longitude:lon),date:components,calculationParameters:params) else {return []}
        return [PrayerMoment(id:"fajr",name:"Subuh",date:p.fajr),PrayerMoment(id:"sunrise",name:"Terbit",date:p.sunrise),PrayerMoment(id:"dhuhr",name:"Zuhur",date:p.dhuhr),PrayerMoment(id:"asr",name:"Asar",date:p.asr),PrayerMoment(id:"maghrib",name:"Magrib",date:p.maghrib),PrayerMoment(id:"isha",name:"Isya",date:p.isha)]
    }
    static func next(_ now:Date,settings:AppSettings)->PrayerMoment? {
        let today=day(now,settings:settings).first {$0.id != "sunrise" && $0.date>now}
        return today ?? day(now.addingTimeInterval(86400),settings:settings).first {$0.id != "sunrise" && $0.date>now}
    }
    static func bearing(settings:AppSettings)->Double? {guard let lat=settings.latitude,let lon=settings.longitude else {return nil};return Qibla(coordinates:Coordinates(latitude:lat,longitude:lon)).direction}
    static func time(_ date:Date,settings:AppSettings)->String {let f=DateFormatter();f.locale=Locale(identifier:"id_ID");f.timeZone=TimeZone(identifier:settings.timeZone);f.dateFormat="HH:mm";return f.string(from:date)}
    static func hijri(_ date:Date,settings:AppSettings)->String {
        let f=DateFormatter();f.calendar=Calendar(identifier:.islamicUmmAlQura);f.locale=Locale(identifier:settings.language=="en" ? "en_US":"id_ID");f.timeZone=TimeZone(identifier:settings.timeZone);f.dateFormat="d MMMM yyyy 'H'";return f.string(from:date.addingTimeInterval(Double(settings.hijriCorrection)*86400))
    }
}
@MainActor final class ReminderService:NSObject,UNUserNotificationCenterDelegate {
    static let shared=ReminderService()
    private var generation=0
    func request() async throws -> Bool {try await UNUserNotificationCenter.current().requestAuthorization(options:[.alert,.sound,.badge])}
    func schedule(state:AppState) async throws {
        generation+=1;let token=generation,center=UNUserNotificationCenter.current(),settings=state.settings
        guard await center.notificationSettings().authorizationStatus == .authorized else {return}
        var calendar=Calendar(identifier:.gregorian);calendar.timeZone=TimeZone(identifier:settings.timeZone) ?? .current
        struct Quote:Decodable {let id:Int,text:String,ref:String}
        let quotes=try state.content.decode("notifications/quran.json",[Quote].self)
        var requests=[(Date,UNNotificationRequest)]();let now=Date()
        func add(_ id:String,_ title:String,_ body:String,_ date:Date,sound:Bool=false,info:[String:String]=[:]) {
            guard date>now else {return}
            let c=UNMutableNotificationContent();c.title=title;c.body=body;c.userInfo=info;c.sound=sound ? UNNotificationSound(named:UNNotificationSoundName("adhan_short.caf")):.default
            let dc=calendar.dateComponents([.year,.month,.day,.hour,.minute,.second],from:date)
            let trigger=UNCalendarNotificationTrigger(dateMatching:dc,repeats:false)
            requests.append((date,UNNotificationRequest(identifier:"nuraya."+id,content:c,trigger:trigger)))
        }
        for offset in 0..<6 {
            guard let day=calendar.date(byAdding:.day,value:offset,to:now) else {continue}
            let tag=calendar.dateComponents([.year,.month,.day],from:day);let dayID="\(tag.year!)-\(tag.month!)-\(tag.day!)"
            if settings.quranReminders,!quotes.isEmpty {
                // Stable per date across relaunches; two independent slots never closer than 3 h.
                let seed=UInt64(tag.year!*10000+tag.month!*100+tag.day!)
                let first=480+Int((seed &* 1103515245 &+ 12345)%301)
                let second=first+180+Int((seed &* 1664525 &+ 1013904223)%UInt64(1201-first-180))
                for (i,minute) in [first,second].enumerated() {
                    let q=quotes[(Int(seed)+i*17)%quotes.count]
                    if let date=calendar.date(bySettingHour:minute/60,minute:minute%60,second:0,of:day) {add("quran.\(dayID).\(i)","Waktunya dekat dengan Al-Qur’an",q.text+"\n"+q.ref,date,info:["destination":"quran"])}
                }
            }
            if settings.prayerReminders {
                for p in PrayerService.day(day,settings:settings) where settings.enabledPrayers.contains(p.id) {
                    add("prayer.\(dayID).\(p.id).before","\(settings.prayerLead) menit lagi \(p.name)","Yuk bersiap wudu dan salat tepat waktu.",p.date.addingTimeInterval(-Double(settings.prayerLead)*60),info:["destination":"prayer"])
                    add("prayer.\(dayID).\(p.id).at","Waktu \(p.name) telah tiba","Semoga Allah memudahkan ibadahmu.",p.date,sound:settings.adhanSound,info:["destination":"prayer"])
                }
            }
        }
        guard token==generation else {return}
        let old=await center.pendingNotificationRequests();center.removePendingNotificationRequests(withIdentifiers:old.filter {$0.identifier.hasPrefix("nuraya.")}.map(\.identifier))
        // iOS caps pending local requests. Refill when the app opens; do not claim indefinite alarms.
        for (_,request) in requests.sorted(by:{$0.0<$1.0}).prefix(60) {guard token==generation else {return};try await center.add(request)}
    }
    func userNotificationCenter(_ center:UNUserNotificationCenter,willPresent notification:UNNotification) async -> UNNotificationPresentationOptions {[.banner,.sound]}
}
