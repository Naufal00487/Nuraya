package com.nuraya.app.profile

import com.nuraya.app.ui.icons.AppIcons as Icons
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.nuraya.app.BuildConfig
import com.nuraya.app.ui.*

data class LocalProfile(val nickname: String = "Sahabat Nuraya", val avatar: Int = 0)
class ProfileStore(context: Context) {
    val preferences = context.applicationContext.getSharedPreferences("nuraya_profile", Context.MODE_PRIVATE)
    fun isComplete(): Boolean = preferences.getString("nickname", null)?.trim()?.length in 1..30
    fun read() = LocalProfile(preferences.getString("nickname", null)?.takeIf { it.isNotBlank() } ?: "Sahabat Nuraya", preferences.getInt("avatar", 0).coerceIn(0,2))
    fun save(nickname: String, avatar: Int) {
        require(nickname.trim().length in 1..30 && avatar in 0..2)
        preferences.edit().putString("nickname", nickname.trim()).putInt("avatar", avatar).apply()
    }
}
@Composable fun rememberProfile(): LocalProfile {
    val context = LocalContext.current
    val store = remember(context) { ProfileStore(context) }
    var profile by remember { mutableStateOf(store.read()) }
    DisposableEffect(store) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> profile = store.read() }
        store.preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { store.preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return profile
}
@Composable fun ProfileAvatar(avatar: Int, modifier: Modifier = Modifier) {
    val image = rememberAssetBitmap("brand/avatars/avatar_${avatar.coerceIn(0,2)}.png")
    Box(modifier.clip(CircleShape).background(Color.White).border(2.dp,MaterialTheme.colorScheme.outlineVariant,CircleShape),contentAlignment=Alignment.Center) {
        if(image!=null) {
            Image(image, "Avatar ${avatar.coerceIn(0,2)+1}", Modifier.fillMaxSize(), contentScale=ContentScale.Fit)
        } else Icon(Icons.Outlined.Person,null,tint=MaterialTheme.colorScheme.primary)
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable fun ProfileScreen(language:String,onNavigate:(String)->Unit) {
    val context=LocalContext.current
    val profile=rememberProfile()
    var nickname by rememberSaveable { mutableStateOf(profile.nickname) }
    var avatar by rememberSaveable { mutableIntStateOf(profile.avatar) }
    var saved by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().testTag("profile_screen"),contentPadding=PaddingValues(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        item { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) { Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally) {
            ProfileAvatar(avatar,Modifier.size(112.dp));Spacer(Modifier.height(12.dp))
            Text(profile.nickname,style=MaterialTheme.typography.headlineSmall)
            Text(label(language,"Ruang belajar milikmu","Your learning space"),color=MaterialTheme.colorScheme.onSurfaceVariant)
        } } }
        item { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedTextField(nickname,{nickname=it.take(30);saved=false},label={Text(label(language,"Nama panggilan","Nickname"))},singleLine=true,modifier=Modifier.fillMaxWidth().testTag("profile_nickname")) } }
        item { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(label(language,"Pilih avatarmu","Choose your avatar"),style=MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                repeat(3) { index -> Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.clickable{avatar=index;saved=false}.testTag("profile_avatar_$index").padding(4.dp)) {
                    ProfileAvatar(index,Modifier.size(64.dp));if(index==avatar)Icon(Icons.Outlined.CheckCircle,label(language,"Dipilih","Selected"),tint=MaterialTheme.colorScheme.primary)
                } }
            }
        } }
        item { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) { Button(onClick={ProfileStore(context).save(nickname,avatar);saved=true},enabled=nickname.isNotBlank(),modifier=Modifier.fillMaxWidth().testTag("profile_save")){Text(label(language,"Simpan profil","Save profile"))}
            if(saved)Text(label(language,"Profil tersimpan di HP ini","Profile saved on this phone"),color=MaterialTheme.colorScheme.primary)
        } }
        item { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) { HorizontalDivider() } }
        item { SettingsGroup(label(language,"Ruang pribadi","Your space")) {
            val destinations = listOf("library", "progress", "settings", "about")
            val titles = listOf(label(language,"Koleksi & unduhan","Collection & downloads"), label(language,"Progres belajarku","My learning progress"), label(language,"Pengaturan","Settings"), label(language,"Tentang Nuraya","About Nuraya"))
            val descriptions = listOf(label(language,"Bacaan dan audio yang disimpan","Your saved readings and audio"), label(language,"Lihat catatan hafalan dan latihan","Review memorization and practice"), label(language,"Atur bacaan, qari, dan tampilan","Reading, reciter and appearance preferences"), label(language,"Aplikasi dan penyusunnya","About the app and its team"))
            val icons = listOf(Icons.Outlined.Bookmarks, Icons.Outlined.School, Icons.Outlined.Settings, Icons.Outlined.Info)
            destinations.forEachIndexed { index, destination ->
                if (index > 0) SettingsSeparator()
                SettingsMenuRow(icons[index], titles[index], descriptions[index], "profile_$destination") { onNavigate(destination) }
            }
        } }
        item { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(label(language,"Nama, avatar, dan progres tersimpan di perangkat ini. Tidak ada akun atau peringkat online.","Your name, avatar, and progress stay on this device. No account or online rankings."),style=MaterialTheme.typography.bodySmall) } }
    }
}
@Composable fun AboutScreen(language:String) {
    LazyColumn(Modifier.testTag("about_screen"),contentPadding=PaddingValues(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) { BrandImage("logo",Modifier.fillMaxWidth().height(160.dp),ContentScale.Fit,"Logo Nuraya") } }
        item { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Nuraya ${BuildConfig.VERSION_NAME}",style=MaterialTheme.typography.headlineMedium)
            Text(label(language,"Teman membaca, mendengar, dan mempelajari Al-Qur’an.","A companion for reading, listening, and learning the Qur’an.")) } }
        item { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(label(language,"Dirancang untuk perjalanan belajar yang bertahap: bacaan Utsmani, murottal, hafalan, tajwid, kuis, dan catatan progres pribadi.","Designed for a gradual learning journey: Uthmani text, recitation, memorization, tajwid, quizzes, and personal progress.")) } }
        item { Text(label(language,
            "Nuraya dikembangkan oleh tim penyusun sebagai tugas mata kuliah di bawah arahan dosen pengampu. Seluruh proses perancangan dan pengembangan dilakukan secara sukarela, tanpa menerima imbalan finansial dalam bentuk apa pun dari pihak mana pun. Aplikasi ini dibuat untuk kepentingan akademik dan pembelajaran.",
            "Nuraya was developed by the team as a course assignment under a lecturer’s guidance. All design and development work was voluntary, without financial compensation from any party. The application serves academic and learning purposes."), Modifier.testTag("about_academic")) }
        item { Text(label(language,"Sumber data dan materi","Data and learning sources"),style=MaterialTheme.typography.titleLarge) }
        item { Text(label(language,
            "Teks Arab Utsmani bersumber dari Tanzil, dengan font Amiri Quran. Terjemahan Indonesia berasal dari Kementerian Agama RI melalui QuranEnc; tafsir Kemenag dan transliterasi melalui eQuran.id. Arti per kata dan sebagian data waktu audio bersumber dari Quran.com. Gambar mushaf berasal dari repositori publik agusibrahim/quran-image dengan indeks dan checksum tiap edisi.",
            "Uthmani Arabic: Tanzil, rendered with Amiri Quran. Indonesian translation: the Indonesian Ministry of Religious Affairs via QuranEnc; commentary and transliteration via eQuran.id. Word meanings and some audio timings: Quran.com. Mushaf images: agusibrahim/quran-image, with per-edition indexes and checksums."), Modifier.testTag("about_sources")) }
        item { Text(label(language,
            "Murottal bersumber dari MP3Quran, EveryAyah, Islamic Network dan QuranicAudio; sinkronisasi tambahan dari QUD Technologies. Foto qari beratribusi Quran.com. Data hadis berasal dari fawazahmed0/hadith-api, dan audio azan dari DBChoco/Muezzin. AI bacaan memakai model Tarteel AI dengan Whisper/whisper.cpp. Ikon UI berasal dari Flaticon; ilustrasi, banner, avatar dan template pengingat disediakan tim penyusun. Rincian atribusi dan lisensi tersedia di Pengaturan → Sumber & atribusi.",
            "Recitations: MP3Quran, EveryAyah, Islamic Network and QuranicAudio; additional timings: QUD Technologies. Reciter photos: Quran.com. Hadith: fawazahmed0/hadith-api; adhan audio: DBChoco/Muezzin. Recitation AI: Tarteel AI with Whisper/whisper.cpp. UI icons: Flaticon. Illustrations, banners, avatars and reminder templates were supplied by the team. Detailed credits and licenses are available in Settings → Sources & attribution.")) }
        item { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(label(language,"AI membantu membandingkan perkiraan kata bacaan, bukan pengganti bimbingan guru atau penilai tajwid/makhraj tervalidasi.","AI helps compare estimated spoken words; it does not replace a teacher or validated tajwid assessment.")) } }
        item { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(label(language,"Teks, terjemahan, rekaman, dan foto tetap mengikuti atribusi sumber serta hak pemiliknya. Rincian sumber tersedia di Pengaturan → Sumber & atribusi.","Texts, translations, recordings, and photos retain source attribution and their owners’ rights. See Settings → Sources & attribution."),style=MaterialTheme.typography.bodySmall) } }
    }
}
