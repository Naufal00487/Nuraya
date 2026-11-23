import SwiftUI
import CoreLocation
import NurayaCore
struct PrayerView:View {
    @ObservedObject var state:AppState
    var body:some View {PageContainer(dark:state.settings.dark){PrayerHeader(state:state,home:false).clipShape(RoundedRectangle(cornerRadius:26));NavigationLink(value:Route.location){MenuRow(title:"Lokasi salat",subtitle:state.settings.locationName.isEmpty ? "Gunakan koordinat iPhone":state.settings.locationName,icon:"marker",dark:state.settings.dark)};NavigationLink(value:Route.qibla){MenuRow(title:"Arah kiblat",subtitle:"Lihat kompas dan arah dari lokasi tersimpan",icon:"kaaba",dark:state.settings.dark)};Text("Jadwal hari ini").font(.title2.bold());ForEach(PrayerService.day(Date(),settings:state.settings)){p in HStack{Text(p.name);Spacer();Text(PrayerService.time(p.date,settings:state.settings)).monospacedDigit().bold()}.font(.system(size:20)).padding(18).background(Palette(dark:state.settings.dark).card,in:RoundedRectangle(cornerRadius:16))};Stepper("Koreksi Hijriah: \(state.settings.hijriCorrection) hari",value:$state.settings.hijriCorrection,in:-2...2);Stepper("Koreksi salat: \(state.settings.prayerCorrection) menit",value:$state.settings.prayerCorrection,in:-30...30);Text("Hijriah memakai kalender Umm al-Qura dan tanggal sipil. Cocokkan tanggal serta jadwal salat dengan rujukan setempat.").font(.footnote).foregroundStyle(.secondary);NavigationLink{ReminderSettingsView(state:state)}label:{MenuRow(title:"Notifikasi & suara azan",icon:"bell",dark:state.settings.dark)}}}
}
struct LocationView:View {
    @ObservedObject var state:AppState;@ObservedObject var location:LocationService
    @State private var name="";@State private var latitude="";@State private var longitude="";@State private var zone=TimeZone.current.identifier
    var valid:Bool {guard let a=Double(latitude),let b=Double(longitude) else{return false};return (-89...89).contains(a) && (-180...180).contains(b) && !name.trimmingCharacters(in:.whitespaces).isEmpty && TimeZone(identifier:zone) != nil}
    var body:some View {PageContainer(dark:state.settings.dark){Text("Lokasi untuk salat & kiblat").font(.title2.bold());Text("Gunakan koordinat iPhone agar tidak perlu mengisi angka lokasi.");PrimaryButton(title:location.busy ? "Mencari lokasi…":"Gunakan lokasi HP",icon:"marker"){location.request()}.disabled(location.busy);if let e=location.error{Text(e).foregroundStyle(.red)};Text("Pilihan manual").font(.headline);TextField("Nama kota/kabupaten",text:$name).textFieldStyle(.roundedBorder);TextField("Lintang, contoh −6.18",text:$latitude).keyboardType(.numbersAndPunctuation).textFieldStyle(.roundedBorder);TextField("Bujur, contoh 106.63",text:$longitude).keyboardType(.numbersAndPunctuation).textFieldStyle(.roundedBorder);Picker("Zona waktu",selection:$zone){ForEach(Array(Set([TimeZone.current.identifier,"Asia/Jakarta","Asia/Makassar","Asia/Jayapura",state.settings.timeZone])).sorted(),id:\.self){z in Text(z).tag(z)}};PrimaryButton(title:"Simpan lokasi"){save()}.disabled(!valid);Text("Tidak ada pelacakan lokasi di latar belakang. Nama daerah dapat memakai layanan geocoding perangkat.").font(.footnote)}.navigationTitle("Lokasi").onAppear{name=state.settings.locationName;latitude=state.settings.latitude.map {String($0)} ?? "";longitude=state.settings.longitude.map {String($0)} ?? "";zone=state.settings.timeZone}.onChange(of:location.coordinate?.latitude){_ in if let c=location.coordinate {latitude=String(format:"%.6f",c.latitude);longitude=String(format:"%.6f",c.longitude);name=location.name.isEmpty ? "Lokasi HP":location.name;zone=TimeZone.current.identifier}}.onChange(of:location.name){n in if !n.isEmpty{ name=n;if latitude==String(format:"%.6f",location.coordinate?.latitude ?? 1000) {state.settings.locationName=n}}}}
    func save(){guard valid else{return};state.settings.locationName=name;state.settings.latitude=Double(latitude);state.settings.longitude=Double(longitude);state.settings.timeZone=zone;Task{try? await ReminderService.shared.schedule(state:state)}}
}
struct QiblaView:View {
    @ObservedObject var state:AppState
    @ObservedObject var location:LocationService
    private var bearing:Double? {PrayerService.bearing(settings:state.settings)}
    var body:some View {
        PageContainer(dark:true){
            Text("Arah kiblat").font(.system(size:30,weight:.bold))
            Text(state.settings.locationName.isEmpty ? "Pilih lokasi terlebih dahulu":state.settings.locationName).font(.title3)
            CompassDial(heading:location.heading,bearing:bearing).frame(height:350)
            directionDescription
            if let accuracy=location.accuracy,accuracy>25 || accuracy<0 {
                Text("Kalibrasikan kompas dengan gerakan angka delapan, lalu jauhkan dari magnet/logam.").foregroundStyle(.orange)
            }
            if let error=location.error {Text(error)}
            Text("Letakkan iPhone mendatar. Arah sensor dapat dipengaruhi benda logam dan lingkungan.").font(.callout)
            NavigationLink(value:Route.location){Text("Ubah lokasi")}
        }.navigationTitle("Kiblat").onAppear{location.startCompass()}.onDisappear{location.stopCompass()}
    }
    @ViewBuilder private var directionDescription:some View {
        if let bearing=bearing {
            Text("\(bearing,specifier:"%.1f")° dari utara sejati").font(.title2.bold())
            if location.heading==nil {Text("Arah numerik tersedia; kompas menunggu sensor dan lokasi.")}
        }
    }
}
private struct CompassDial:View {
    let heading:Double?,bearing:Double?
    private let gold=Color(hex:0xF1C057)
    var body:some View {
        GeometryReader {geometry in
            let size=min(geometry.size.width,340)
            ZStack {
                Circle().fill(Color(hex:0x263955)).overlay(Circle().stroke(Color(hex:0x536D99),lineWidth:7))
                markings
                if let bearing=bearing,let heading=heading {
                    VStack(spacing:0){
                        AppIcon(name:"kaaba",size:28).foregroundStyle(gold).frame(width:48,height:48).background(Color(hex:0x1A2841),in:Circle())
                        Path{path in path.move(to:CGPoint(x:8,y:0));path.addLine(to:CGPoint(x:0,y:96));path.addLine(to:CGPoint(x:16,y:96));path.closeSubpath()}.fill(gold).frame(width:16,height:96)
                        Spacer().frame(height:120)
                    }.frame(height:264).rotationEffect(.degrees(bearing-heading))
                }
                Circle().fill(gold).frame(width:16,height:16)
            }.frame(width:size,height:size).frame(maxWidth:.infinity)
        }
    }
    private var markings:some View {
        Canvas {context,size in
            let radius=min(size.width,size.height)/2
            for tick in 0..<72 {
                let angle=(Double(tick)*5-(heading ?? 0))*Double.pi/180
                let outside=radius-17,inside=outside-CGFloat(tick%6==0 ? 17:8)
                var path=Path();path.move(to:point(angle,inside,radius));path.addLine(to:point(angle,outside,radius))
                context.stroke(path,with:.color(.white.opacity(0.7)),lineWidth:tick%6==0 ? 2:1)
            }
            for (index,title) in ["U","T","S","B"].enumerated() {
                let angle=(Double(index)*90-(heading ?? 0))*Double.pi/180
                context.draw(Text(title).font(.title3.bold()).foregroundColor(.white),at:point(angle,radius-50,radius))
            }
        }
    }
    private func point(_ angle:Double,_ distance:CGFloat,_ center:CGFloat)->CGPoint {CGPoint(x:center+CGFloat(sin(angle))*distance,y:center-CGFloat(cos(angle))*distance)}
}
struct ReminderSettingsView:View {
    @ObservedObject var state:AppState;@State private var status="";@State private var saving=false
    var body:some View {Form{Section("Al-Qur’an"){Toggle("Pengingat Quran",isOn:$state.settings.quranReminders);Text("Dua kali per hari, isi dan waktu acak 08.00–20.00 dengan jeda minimal tiga jam.")};Section("Salat"){Toggle("Pengingat salat",isOn:$state.settings.prayerReminders);Picker("Sebelum salat",selection:$state.settings.prayerLead){ForEach([5,10,15,30],id:\.self){Text("\($0) menit").tag($0)}};ForEach([("fajr","Subuh"),("dhuhr","Zuhur"),("asr","Asar"),("maghrib","Magrib"),("isha","Isya")],id:\.0){id,name in Toggle(name,isOn:Binding(get:{state.settings.enabledPrayers.contains(id)},set:{if $0 {if !state.settings.enabledPrayers.contains(id){state.settings.enabledPrayers.append(id)}}else{state.settings.enabledPrayers.removeAll {$0==id}}}))};Toggle("Cuplikan azan saat waktunya",isOn:$state.settings.adhanSound);Text("iOS membatasi suara notifikasi hingga kurang dari 30 detik ketika aplikasi tertutup. Pengingat tunduk pada izin, mode Fokus, dan suara iPhone.")};Section{Button(saving ? "Menyimpan…":"Izinkan & simpan pengingat"){Task{saving=true;defer{saving=false};do {guard try await ReminderService.shared.request() else {status="Izin notifikasi belum diberikan. Buka Pengaturan iPhone.";return};try await ReminderService.shared.schedule(state:state);status="Pengingat tersimpan. Buka Nuraya secara berkala untuk memperbarui jadwal beberapa hari berikutnya."}catch{status=error.localizedDescription}}}.disabled(saving);Text(status);Text("Jadwal diisi ulang saat Nuraya dibuka karena iOS membatasi jumlah notifikasi yang dapat dijadwalkan sekaligus.").font(.footnote)}}.navigationTitle("Notifikasi & azan")}
}
struct ProfileView:View {
    @ObservedObject var state:AppState;@State private var name="";@State private var saved=false
    var body:some View {PageContainer(dark:state.settings.dark){AssetImage(content:state.content,path:"brand/avatars/avatar_\(state.settings.avatar).png",fit:true).frame(height:130);Text("Profil saya").font(.title.bold());TextField("Nama panggilan",text:$name).textFieldStyle(.roundedBorder);AvatarPicker(state:state,selected:$state.settings.avatar);PrimaryButton(title:"Simpan profil"){state.settings.name=String(name.trimmingCharacters(in:.whitespacesAndNewlines).prefix(60));saved=true}.disabled(name.trimmingCharacters(in:.whitespacesAndNewlines).isEmpty);if saved{Text("Profil tersimpan.")};NavigationLink(value:Route.library){MenuRow(title:"Koleksi saya",icon:"bookmark",dark:state.settings.dark)};Text("Profil pribadi disimpan di iPhone ini.").font(.footnote)}.onAppear{name=state.settings.name}.navigationTitle("Profil")}
}
struct SourcesView:View {
    @ObservedObject var state:AppState
    var body:some View {PageContainer(dark:state.settings.dark){Text("Sumber & Atribusi").font(.title.bold());ForEach([("Teks Arab Utsmani","Tanzil Project · teks dipertahankan verbatim","https://tanzil.net/docs/Text_License"),("Terjemahan Indonesia","Kemenag RI melalui QuranEnc","https://quranenc.com/id/browse/indonesian_affairs"),("Arti kata & timing","Quran.com; sumber rekaman dipasangkan dengan data waktu","https://quran.com"),("Tafsir & Latin","eQuran.id · Kemenag RI","https://equran.id/apidev/v2"),("Gambar mushaf","agusibrahim/quran-image; indeks dan hash sesuai sumber Nuraya","https://github.com/agusibrahim/quran-image"),("Audio qari","MP3Quran, EveryAyah, Islamic Network, QuranicAudio dan QUD","https://www.mp3quran.net"),("Hadis","fawazahmed0/hadith-api","https://github.com/fawazahmed0/hadith-api"),("AI bacaan","Tarteel / Whisper / whisper.cpp","https://huggingface.co/tarteel-ai/whisper-base-ar-quran"),("Ikon antarmuka","Flaticon UIcons, path yang sama dengan Android","https://www.flaticon.com/uicons"),("Perhitungan salat","Adhan Swift · Batoul Apps","https://github.com/batoulapps/adhan-swift")],id:\.0){title,detail,url in VStack(alignment:.leading,spacing:6){Text(title).font(.headline);Text(detail);if let link=URL(string:url){Link("Kunjungi sumber",destination:link)}}.padding(.vertical,8)};Text("Font Arab: Amiri Quran, SIL Open Font License. Foto qari: Quran.com dan dokumen penyusun sesuai pemetaan identitas. Desain visual dan rancangan fitur: Naufal Irfan Ramadhan.");ForEach(licensePaths,id:\.self){path in NavigationLink(path){ScrollView{Text((try? String(contentsOf:state.content.root.appendingPathComponent("licenses/"+path),encoding:.utf8)) ?? "Dokumen lisensi tidak tersedia.").font(.system(size:14)).padding()}.navigationTitle(path)}}}.navigationTitle("Sumber")}
    var licensePaths:[String]{((try? FileManager.default.contentsOfDirectory(atPath:state.content.root.appendingPathComponent("licenses").path)) ?? []).sorted()}
}
