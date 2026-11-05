package com.nuraya.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(app:NurayaApplication,settings:UserSettings,perform:(suspend ()->Unit)->Unit,onLearn:()->Unit){
    val language=settings.language
    val context=LocalContext.current
    var textSize by remember(settings.arabicSize){mutableFloatStateOf(settings.arabicSize)}
    var sourcesVisible by remember{mutableStateOf(false)}
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
        item {
            Text(label(language,"Atur kenyamanan membaca","Reading preferences"),style=MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(label(language,"Mode tamu · tersimpan di perangkat ini","Guest mode · saved on this device"),color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            SectionTitle(label(language,"Bahasa antarmuka","Interface language"))
            FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                FilterChip(selected=language=="id",onClick={perform{app.settings.setLanguage("id")}},label={Text("Indonesia")},modifier=Modifier.testTag("language_id"))
                FilterChip(selected=language=="en",onClick={perform{app.settings.setLanguage("en")}},label={Text("English")},modifier=Modifier.testTag("language_en"))
            }
        }
        item {
            SectionTitle(label(language,"Terjemahan Al-Qur’an","Qur’an translation"))
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf("id" to "Indonesia","en" to "English","none" to label(language,"Tanpa","None")).forEach{(key,title)->FilterChip(selected=settings.translation==key,onClick={perform{app.settings.setTranslation(key)}},label={Text(title)})}
            }
            Text(if(settings.translation=="en")"Marmaduke Pickthall (1930)" else if(settings.translation=="id")"Kementerian Agama RI · QuranEnc v1.0.1" else label(language,"Hanya teks Arab","Arabic text only"),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            SectionTitle(label(language,"Ukuran tulisan Arab","Arabic text size"))
            Text(label(language,"Rasm Utsmani · font Amiri Quran · tersedia offline","Uthmani script · Amiri Quran font · available offline"),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick={textSize=(textSize-2f).coerceAtLeast(22f);perform{app.settings.setArabicSize(textSize)}}, enabled=textSize>22f, modifier=Modifier.weight(1f).heightIn(min=56.dp)) { Text(label(language,"A− Kecilkan","A− Smaller")) }
                Button(onClick={textSize=(textSize+2f).coerceAtMost(48f);perform{app.settings.setArabicSize(textSize)}}, enabled=textSize<48f, modifier=Modifier.weight(1f).heightIn(min=56.dp)) { Text(label(language,"A+ Besarkan","A+ Larger")) }
            }
            Spacer(Modifier.height(12.dp))
            Text("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", fontFamily=QuranArabicFont, fontSize=textSize.sp, lineHeight=(textSize*1.8f).sp, modifier=Modifier.fillMaxWidth(), textAlign=androidx.compose.ui.text.style.TextAlign.Center)
            Text("${textSize.toInt()} sp",style=MaterialTheme.typography.bodySmall)
        }
        item {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(label(language,"Warna tajwid","Tajwid colors"),style=MaterialTheme.typography.titleMedium)
                    Text(label(language,"Warna penanda hukum bacaan · aktif secara default","Reading-rule colors · enabled by default"),style=MaterialTheme.typography.bodySmall)
                }
                Switch(checked=settings.tajwidColors,onCheckedChange={perform{app.settings.setTajwidColors(it)}},modifier=Modifier.testTag("tajwid_colors_switch"))
            }
        }
        item {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(label(language,"Ikuti lantunan ayat","Follow recitation"),style=MaterialTheme.typography.titleMedium)
                    Text(label(language,"Gulir ke ayat yang sedang diputar dalam mode sinkron","Scroll to the current verse during synchronized playback"),style=MaterialTheme.typography.bodySmall)
                }
                Switch(checked=settings.followRecitation,onCheckedChange={perform{app.settings.setFollowRecitation(it)}})
            }
        }
        item {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)){Text(label(language,"Tampilan gelap","Dark appearance"),style=MaterialTheme.typography.titleMedium);Text(label(language,"Nyaman untuk membaca malam hari","Comfortable for evening reading"),style=MaterialTheme.typography.bodySmall)}
                Switch(checked=settings.darkMode,onCheckedChange={perform{app.settings.setDarkMode(it)}})
            }
        }
        item { HorizontalDivider() }
        item {
            SectionTitle(label(language,"Pernyataan nonkomersial","Noncommercial disclaimer"))
            Spacer(Modifier.height(12.dp))
            Text(label(language,
                "Kami, penyusun aplikasi Nuraya, tidak mengambil keuntungan finansial dari aplikasi ini. Nuraya disediakan gratis, tanpa iklan dari Nuraya, langganan, atau pembelian dalam aplikasi. Hak cipta teks, terjemahan, dan rekaman tetap milik sumber serta pemegang hak masing-masing.",
                "We, the creators of Nuraya, do not earn financial profit from this application. Nuraya is provided free of charge, without Nuraya ads, subscriptions, or in-app purchases. Copyright in the texts, translations, and recordings remains with their respective sources and rightsholders."),
                style=MaterialTheme.typography.bodyMedium,modifier=Modifier.testTag("noncommercial_disclaimer"))
        }
        item {
            SectionTitle(label(language,"Tentang ruang belajar ini","About this learning space"))
            Spacer(Modifier.height(12.dp))
            Text(label(language,"Nuraya gratis dan tidak memasang iklan sendiri. Penanda, riwayat, pengaturan, dan unduhan disimpan di perangkat. Tidak ada akun atau sinkronisasi cloud yang aktif.","Nuraya is free and does not place its own ads. Bookmarks, history, settings, and downloads stay on this device. No account or cloud sync is active."),style=MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(10.dp))
            Text(label(language,"Izin lokasi dipakai saat Anda mengaktifkan lokasi HP untuk jadwal salat dan kiblat. Nuraya menyimpan koordinat di perangkat tanpa mengunggah atau melacaknya di latar belakang. Anda dapat memperbarui lokasi lewat Ibadah → Lokasi.","Location permission is used when you enable device location for prayer times and Qibla. Nuraya stores coordinates on this device without uploading or tracking them in the background. Refresh your location under Prayer → Place."),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Text(label(language,"Streaming murottal dan pengunduhan gambar mushaf menghubungkan perangkat ke penyedia konten. AI bacaan memproses suara di HP setelah model diunduh; Nuraya tidak menyimpan atau mengunggah rekaman maupun transkrip. Pilihan pengenal Android dapat memproses online setelah persetujuan Anda. Teman Belajar mencari rujukan lokal; pertanyaan tidak dikirim ke server.","Recitation streaming and mushaf image downloads connect your device to content providers. Recitation AI processes audio on your phone after the model download; Nuraya neither stores nor uploads recordings or transcripts. The optional Android recognizer may process online after your consent. The learning companion searches local sources; questions are not sent to a server."),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            OutlinedButton(onClick={sourcesVisible=!sourcesVisible},modifier=Modifier.fillMaxWidth()) {Icon(Icons.Outlined.Verified,null);Spacer(Modifier.width(8.dp));Text(label(language,"Sumber & atribusi","Sources & attribution"))}
            if(sourcesVisible) {
                Spacer(Modifier.height(12.dp))
                Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surfaceContainerLow){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                    Text(app.quran.attribution,style=MaterialTheme.typography.bodySmall)
                    Text(app.quran.sourceNotice,style=MaterialTheme.typography.bodySmall)
                    Text(label(language,"Teks Arab: Tanzil Utsmani. Warna tajwid memakai anotasi sumber; arti per kata dan tafsir mencantumkan edisinya di pembaca. Penjelasan belajar dan hasil pengenalan suara perlu diperiksa bersama guru.","Arabic: Tanzil Uthmani. Tajwid colors use source annotations; word meanings and tafsir identify their editions in the reader. Study explanations and speech recognition results should be reviewed with a teacher."),style=MaterialTheme.typography.bodySmall)
                    app.study.sources.forEach { source -> Text(source, style = MaterialTheme.typography.bodySmall) }
                    Text("AI bacaan: Tarteel AI · whisper-base-ar-quran (Apache-2.0), OpenAI Whisper / whisper.cpp (MIT).", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick={runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://huggingface.co/tarteel-ai/whisper-base-ar-quran")))}}){Text(label(language,"Sumber model AI & lisensi","AI model source & license"))}
                    TextButton(onClick={runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://tanzil.net/docs/text_license")))}}){Text("Tanzil · ${label(language,"ketentuan sumber","source terms")}")}
                    TextButton(onClick={runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://alquran.cloud/about")))}}){Text("Islamic Network · ${label(language,"sumber audio","audio source")}")}
                    TextButton(onClick={runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/cpfair/quran-tajweed")))}}){Text("cpfair · ${label(language,"anotasi tajwid · CC BY 4.0","tajwid annotations · CC BY 4.0")}")}
                    TextButton(onClick={runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://quran.com")))}}){Text("Quran.com · ${label(language,"arti per kata & waktu audio","word meanings & audio timing")}")}
                    TextButton(onClick={runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://equran.id/apidev/v2")))}}){Text("eQuran.id · ${label(language,"tafsir Kemenag","Kemenag commentary")}")}
                }}
            }
            TextButton(onClick=onLearn,modifier=Modifier.fillMaxWidth()){Text(label(language,"Tentang asisten pembelajaran","About the learning companion"))}
        }
        item {Text("Nuraya ${BuildConfig.VERSION_NAME} · ${label(language,"Edisi pengembangan","Development edition")}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(12.dp))}
    }
}
