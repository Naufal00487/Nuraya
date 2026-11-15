package com.nuraya.app.ui

import com.nuraya.app.ui.icons.AppIcons as Icons
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuraya.app.BuildConfig
import com.nuraya.app.NurayaApplication
import com.nuraya.app.storage.UserSettings

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(app:NurayaApplication,settings:UserSettings,perform:(suspend ()->Unit)->Unit,onLearn:()->Unit,onAbout:()->Unit = {}) {
    val language=settings.language
    var panel by remember { mutableStateOf<String?>(null) }
    var textSize by remember(settings.arabicSize) { mutableFloatStateOf(settings.arabicSize) }
    var qariQuery by remember { mutableStateOf("") }
    val qariName=app.playback.verseReciters.firstOrNull { it.id==settings.readerQariId }?.name ?: label(language,"Pilih qari","Choose reciter")
    LazyColumn(Modifier.fillMaxSize().testTag("settings_screen"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(26.dp)) {
        item { SettingsGroup(label(language,"Al-Qur’an","Qur’an")) {
            SettingsMenuRow(Icons.Outlined.Headphones,label(language,"Qari pilihan","Preferred reciter"),qariName,"settings_qari") { panel="qari" }
            SettingsSeparator()
            SettingsMenuRow(Icons.Outlined.Translate,label(language,"Terjemahan","Translation"),when(settings.translation){"en"->"English";"none"->label(language,"Tanpa terjemahan","No translation");else->"Indonesia"},"settings_translation") { panel="translation" }
            SettingsSeparator()
            SettingsMenuRow(Icons.Outlined.Accessibility,label(language,"Ukuran tulisan Arab","Arabic text size"),label(language,"${settings.arabicSize.toInt()} sp · lihat contoh tulisan","${settings.arabicSize.toInt()} sp · preview text"),"settings_arabic_size") { panel="arabic" }
            SettingsSeparator()
            SettingsSwitchRow(Icons.Outlined.Palette,label(language,"Warna tajwid","Tajwid colors"),label(language,"Penanda warna hukum bacaan","Color cues for reading rules"),settings.tajwidColors,"tajwid_colors_switch") { perform { app.settings.setTajwidColors(it) } }
            SettingsSeparator()
            SettingsSwitchRow(Icons.Outlined.GraphicEq,label(language,"Ikuti lantunan ayat","Follow recitation"),label(language,"Halaman mengikuti ayat yang diputar","Follow the verse being recited"),settings.followRecitation,"follow_recitation_switch") { perform { app.settings.setFollowRecitation(it) } }
        } }
        item { SettingsGroup(label(language,"Tampilan","Appearance")) {
            SettingsSwitchRow(Icons.Outlined.DarkMode,label(language,"Tampilan gelap","Dark appearance"),label(language,"Warna nyaman untuk malam hari","Comfortable colors for evening reading"),settings.darkMode,"dark_mode_switch") { perform { app.settings.setDarkMode(it) } }
            SettingsSeparator()
            SettingsMenuRow(Icons.Outlined.Globe,label(language,"Bahasa aplikasi","App language"),if(language=="en")"English" else "Indonesia","settings_language") { panel="language" }
        } }
        item { SettingsGroup(label(language,"Pengingat","Reminders")) {
            SettingsMenuRow(Icons.Outlined.Notifications,label(language,"Notifikasi & alarm","Notifications & alarms"),label(language,"Pengingat Quran, salat, dan suara azan","Quran reminders, prayer and adhan audio"),"settings_reminders") { panel="reminders" }
        } }
        item { SettingsGroup(label(language,"Aplikasi","Application")) {
            SettingsMenuRow(Icons.Outlined.Shield,label(language,"Privasi & penyimpanan","Privacy & storage"),label(language,"Data pribadi disimpan di HP","Personal data stays on your phone"),"settings_privacy") { panel="privacy" }
            SettingsSeparator()
            SettingsMenuRow(Icons.Outlined.Verified,label(language,"Sumber & atribusi","Sources & attribution"),label(language,"Sumber bacaan, audio, dan desain","Reading, audio and design sources"),"settings_sources") { panel="sources" }
            SettingsSeparator()
            SettingsMenuRow(Icons.Outlined.School,label(language,"Asisten pembelajaran","Learning companion"),label(language,"Cari ayat, makna, dan pelajaran tajwid","Find verses, meaning and tajwid lessons"),"settings_learning",onLearn)
            SettingsSeparator()
            SettingsMenuRow(Icons.Outlined.Info,label(language,"Tentang Nuraya","About Nuraya"),label(language,"Kenali aplikasi dan penyusunnya","About the app and its team"),"settings_about",onAbout)
        } }
        item { Text("Nuraya ${BuildConfig.VERSION_NAME}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.fillMaxWidth(),textAlign=androidx.compose.ui.text.style.TextAlign.Center) }
    }
    panel?.let { current ->
        ModalBottomSheet(onDismissRequest={panel=null},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
            Column(Modifier.fillMaxWidth().heightIn(max=680.dp).verticalScroll(rememberScrollState()).padding(20.dp).testTag("settings_panel_$current"),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text(when(current){"qari"->label(language,"Qari pilihan","Preferred reciter");"translation"->label(language,"Terjemahan Al-Qur’an","Qur’an translation");"arabic"->label(language,"Ukuran tulisan Arab","Arabic text size");"language"->label(language,"Bahasa aplikasi","App language");"privacy"->label(language,"Privasi & penyimpanan","Privacy & storage");"reminders"->label(language,"Notifikasi & alarm","Notifications & alarms");else->label(language,"Sumber & atribusi","Sources & attribution")},Modifier.weight(1f),style=MaterialTheme.typography.titleLarge)
                    IconButton(onClick={panel=null}) { Icon(Icons.Outlined.Close,label(language,"Tutup pengaturan","Close settings")) }
                }
                when(current) {
                    "reminders" -> com.nuraya.app.reminders.ReminderSettings(language)
                    "language" -> FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        FilterChip(language=="id",onClick={perform{app.settings.setLanguage("id")}},label={Text("Indonesia")},modifier=Modifier.testTag("language_id"))
                        FilterChip(language=="en",onClick={perform{app.settings.setLanguage("en")}},label={Text("English")},modifier=Modifier.testTag("language_en"))
                    }
                    "translation" -> {
                        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            listOf("id" to "Indonesia","en" to "English","none" to label(language,"Tanpa","None")).forEach { (key,title) -> FilterChip(settings.translation==key,onClick={perform{app.settings.setTranslation(key)}},label={Text(title)}) }
                        }
                        Text(if(settings.translation=="en")"Marmaduke Pickthall (1930)" else if(settings.translation=="id")"Kementerian Agama RI · QuranEnc v1.0.1" else label(language,"Hanya teks Arab","Arabic text only"),style=MaterialTheme.typography.bodySmall)
                    }
                    "arabic" -> {
                        Text(label(language,"Rasm Utsmani · font Amiri Quran","Uthmani script · Amiri Quran font"),style=MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick={textSize=(textSize-2f).coerceAtLeast(22f);perform{app.settings.setArabicSize(textSize)}},enabled=textSize>22f,modifier=Modifier.weight(1f)) { Text(label(language,"Kecilkan","Smaller")) }
                            Button(onClick={textSize=(textSize+2f).coerceAtMost(48f);perform{app.settings.setArabicSize(textSize)}},enabled=textSize<48f,modifier=Modifier.weight(1f)) { Text(label(language,"Besarkan","Larger")) }
                        }
                        Text("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",fontFamily=QuranArabicFont,fontSize=textSize.sp,lineHeight=(textSize*1.8f).sp,modifier=Modifier.fillMaxWidth(),textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                        Text("${textSize.toInt()} sp",style=MaterialTheme.typography.bodySmall)
                    }
                    "qari" -> {
                        Text(label(language,"Pilihan ini dipakai saat membaca Al-Qur’an dan berlatih hafalan.","This choice is used in the Quran reader and memorization practice."),style=MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(qariQuery,{qariQuery=it.take(80)},label={Text(label(language,"Cari nama qari","Search reciters"))},leadingIcon={Icon(Icons.Outlined.Search,null)},modifier=Modifier.fillMaxWidth(),singleLine=true)
                        app.playback.verseReciters.filter { it.name.contains(qariQuery,true) }.forEach { qari ->
                            Surface(onClick={perform{app.settings.setReaderQari(qari.id)}},shape=RoundedCornerShape(18.dp),color=if(qari.id==settings.readerQariId)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow) {
                                Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                                    QariPortrait(qari.id,qari.name,language,Modifier.size(44.dp))
                                    Text(qari.name,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium)
                                    RadioButton(selected=qari.id==settings.readerQariId,onClick=null)
                                }
                            }
                        }
                    }
                    "privacy" -> SettingsPrivacy(language)
                    "sources" -> SettingsSources(app,language)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable private fun SettingsPrivacy(language:String) {
    Column(verticalArrangement=Arrangement.spacedBy(14.dp)) {

            Spacer(Modifier.height(12.dp))
            Text(label(language,"Penanda, riwayat, pengaturan, dan unduhan disimpan di perangkat. Tidak ada akun atau sinkronisasi cloud yang aktif.","Bookmarks, history, settings, and downloads stay on this device. No account or cloud sync is active."),style=MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(10.dp))
            Text(label(language,"Izin lokasi dipakai saat Anda mengaktifkan lokasi HP untuk jadwal salat dan kiblat. Koordinat disimpan di perangkat; layanan Android dapat memakai internet untuk mencari nama kecamatan. Nuraya tidak mengirim lokasi ke server Nuraya atau melacaknya di latar belakang. Anda dapat memperbarui lokasi lewat Ibadah → Lokasi.","Location permission is used when you enable device location for prayer times and Qibla. Coordinates stay on this device; Android may use the internet to resolve the district name. Nuraya does not send locations to a Nuraya server or track them in the background. Refresh your location under Prayer → Place."),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Text(label(language,"Streaming murottal, pengunduhan gambar mushaf, dan pembaruan banner beranda menghubungkan perangkat ke penyedia konten. AI bacaan memproses suara di HP setelah model diunduh; Nuraya tidak menyimpan atau mengunggah rekaman maupun transkrip. Pilihan pengenal Android dapat memproses online setelah persetujuan Anda. Pencarian ayat dan makna mengambil rujukan dari data lokal. Pencarian suara bahasa Indonesia memakai layanan Android; jika pengenal offline tidak tersedia, audio dapat diproses oleh penyedia layanan suara sesuai pengaturan perangkat.","Recitation streaming, mushaf image downloads, and home banner updates connect your device to content providers. Recitation AI processes audio on your phone after the model download; Nuraya neither stores nor uploads recordings or transcripts. The optional Android recognizer may process online after your consent. Verse and meaning search retrieves local sources. Indonesian voice search uses Android speech services; without offline recognition, audio may be processed by the speech provider according to device settings."),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)

    }
}
@Composable private fun SettingsSources(app:NurayaApplication,language:String) {
    val context=LocalContext.current
    Column(verticalArrangement=Arrangement.spacedBy(14.dp)) {

                Spacer(Modifier.height(12.dp))
                Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surfaceContainerLow){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                    Text(app.quran.attribution,style=MaterialTheme.typography.bodySmall)
                    Text(app.quran.sourceNotice,style=MaterialTheme.typography.bodySmall)
                    Text(label(language,"Teks Arab: Tanzil Utsmani. Warna tajwid memakai anotasi sumber; arti per kata dan tafsir mencantumkan edisinya di pembaca. Penjelasan belajar dan hasil pengenalan suara perlu diperiksa bersama guru.","Arabic: Tanzil Uthmani. Tajwid colors use source annotations; word meanings and tafsir identify their editions in the reader. Study explanations and speech recognition results should be reviewed with a teacher."),style=MaterialTheme.typography.bodySmall)
                    app.study.sources.forEach { source -> Text(source, style = MaterialTheme.typography.bodySmall) }
                    Text("AI bacaan: Tarteel AI · whisper-base-ar-quran (Apache-2.0), OpenAI Whisper / whisper.cpp (MIT).", style = MaterialTheme.typography.bodySmall)
                    Text(label(language,"Foto qari: profil beratribusi dari Quran.com. Audio azan Makkah: proyek DBChoco/Muezzin (MIT). Ilustrasi lanskap dan logo: berkas desain Nuraya.","Reciter portraits: attributed Quran.com profiles. Mecca adhan audio: DBChoco/Muezzin (MIT). Landscapes and logo: Nuraya design files."),style=MaterialTheme.typography.bodySmall)
                    TextButton(onClick={runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://huggingface.co/tarteel-ai/whisper-base-ar-quran")))}}){Text(label(language,"Sumber model AI & lisensi","AI model source & license"))}
                    TextButton(onClick={runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://tanzil.net/docs/text_license")))}}){Text("Tanzil · ${label(language,"ketentuan sumber","source terms")}")}
                    Text("Audio surah: MP3Quran. Audio ayat: EveryAyah / Islamic Network. Rekaman bertiming: Quran.com / QuranicAudio. Hak rekaman tetap pada penyedia dan qari.",style=MaterialTheme.typography.bodySmall)
                    TextButton(onClick={runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.mp3quran.net/")))}}){Text("MP3Quran · sumber murottal")}
                    TextButton(onClick={runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://everyayah.com/")))}}){Text("EveryAyah · sumber audio ayat")}
                    TextButton(onClick={runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://alquran.cloud/about")))}}){Text("Islamic Network · ${label(language,"sumber audio","audio source")}")}
                    TextButton(onClick={runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/cpfair/quran-tajweed")))}}){Text("cpfair · ${label(language,"anotasi tajwid · CC BY 4.0","tajwid annotations · CC BY 4.0")}")}
                    Text(label(language,"Sorotan kata: Quran.com (Alafasy) dan QUD Technologies, Quranic Universal Audio v4.0.0 (qari dengan data waktu yang sesuai), CC BY 4.0. Waktu dipasangkan dengan rekaman sumbernya; ayat tanpa data cocok memakai sorotan per ayat. Hak rekaman tetap pada penyedia/qari.","Word timing: Quran.com (Alafasy) and QUD Technologies, Quranic Universal Audio v4.0.0 (reciters with matching timing data), CC BY 4.0. Timings retain their paired recordings; unmatched verses use verse highlighting. Audio rights remain with providers/reciters."),style=MaterialTheme.typography.bodySmall)
                    TextButton(onClick={runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/QUD-Technologies/quranic-universal-audio/releases/tag/v4.0.0")))}}){Text("QUD · ${label(language,"sumber waktu audio & lisensi","audio timing source & license")}")}
                    TextButton(onClick={runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://quran.com")))}}){Text("Quran.com · ${label(language,"arti per kata & waktu audio","word meanings & audio timing")}")}
                    TextButton(onClick={runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://equran.id/apidev/v2")))}}){Text("eQuran.id · ${label(language,"tafsir Kemenag","Kemenag commentary")}")}
                }}

        Text("Ikon UI oleh Flaticon · UIcons Regular Rounded. Digunakan dengan atribusi sesuai lisensi gratis Flaticon.",style=MaterialTheme.typography.bodySmall)
        TextButton(onClick={runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.flaticon.com/uicons")))}}) { Text("Flaticon · sumber ikon & lisensi") }
        Text("Hadis Arab & Indonesia: fawazahmed0/hadith-api · Unlicense. Nomor dan teks mengikuti edisi sumber. Sebagian entri belum memiliki terjemahan.",style=MaterialTheme.typography.bodySmall)
        TextButton(onClick={runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/fawazahmed0/hadith-api")))}}) { Text("Sumber hadis & lisensi") }
    }
}
