package com.nuraya.app.profile

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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
    fun read() = LocalProfile(preferences.getString("nickname", null)?.takeIf { it.isNotBlank() } ?: "Sahabat Nuraya", preferences.getInt("avatar", 0).coerceIn(0,4))
    fun save(nickname: String, avatar: Int) {
        require(nickname.trim().length in 1..30 && avatar in 0..4)
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
    val image = rememberAssetBitmap("brand/avatars.png")
    Box(modifier.clip(CircleShape).background(Color.White).border(2.dp,MaterialTheme.colorScheme.outlineVariant,CircleShape),contentAlignment=Alignment.Center) {
        if(image!=null) {
            val width=image.width/5
            Image(BitmapPainter(image,IntOffset(width*avatar.coerceIn(0,4),(image.height*.32f).toInt()),IntSize(width,(image.height*.42f).toInt())),
                "Avatar ${avatar+1}",Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
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
        item { Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally) {
            ProfileAvatar(avatar,Modifier.size(112.dp));Spacer(Modifier.height(12.dp))
            Text(profile.nickname,style=MaterialTheme.typography.headlineSmall)
            Text(label(language,"Ruang belajar milikmu","Your learning space"),color=MaterialTheme.colorScheme.onSurfaceVariant)
        } }
        item { OutlinedTextField(nickname,{nickname=it.take(30);saved=false},label={Text(label(language,"Nama panggilan","Nickname"))},singleLine=true,modifier=Modifier.fillMaxWidth().testTag("profile_nickname")) }
        item {
            Text(label(language,"Pilih avatarmu","Choose your avatar"),style=MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                repeat(5) { index -> Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.clickable{avatar=index;saved=false}.testTag("profile_avatar_$index").padding(4.dp)) {
                    ProfileAvatar(index,Modifier.size(64.dp));if(index==avatar)Icon(Icons.Outlined.CheckCircle,label(language,"Dipilih","Selected"),tint=MaterialTheme.colorScheme.primary)
                } }
            }
        }
        item { Button(onClick={ProfileStore(context).save(nickname,avatar);saved=true},enabled=nickname.isNotBlank(),modifier=Modifier.fillMaxWidth().testTag("profile_save")){Text(label(language,"Simpan profil","Save profile"))}
            if(saved)Text(label(language,"Profil tersimpan di HP ini","Profile saved on this phone"),color=MaterialTheme.colorScheme.primary)
        }
        item { HorizontalDivider() }
        items(4){i -> val target=listOf("library","progress","settings","about")[i]
            OutlinedCard(onClick={onNavigate(target)},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp)) {
                Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){
                    Icon(listOf(Icons.Outlined.Bookmarks,Icons.Outlined.School,Icons.Outlined.Settings,Icons.Outlined.Info)[i],null)
                    Text(listOf(label(language,"Koleksi & unduhan","Collection & downloads"),label(language,"Progres belajarku","My learning progress"),label(language,"Pengaturan","Settings"),label(language,"Tentang Nuraya","About Nuraya"))[i],Modifier.weight(1f).padding(start=12.dp))
                    Icon(Icons.Outlined.ChevronRight,null)
                }
            }
        }
        item { Text(label(language,"Nama, avatar, dan progres tersimpan di perangkat ini. Tidak ada akun atau peringkat online.","Your name, avatar, and progress stay on this device. No account or online rankings."),style=MaterialTheme.typography.bodySmall) }
    }
}
@Composable fun AboutScreen(language:String) {
    LazyColumn(contentPadding=PaddingValues(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item { BrandImage("logo",Modifier.fillMaxWidth().height(160.dp),ContentScale.Fit,"Logo Nuraya") }
        item { Text("Nuraya ${BuildConfig.VERSION_NAME}",style=MaterialTheme.typography.headlineMedium)
            Text(label(language,"Teman membaca, mendengar, dan mempelajari Al-Qur’an.","A companion for reading, listening, and learning the Qur’an.")) }
        item { Text(label(language,"Dirancang untuk perjalanan belajar yang bertahap: bacaan Utsmani, murottal, hafalan, tajwid, kuis, dan catatan progres pribadi.","Designed for a gradual learning journey: Uthmani text, recitation, memorization, tajwid, quizzes, and personal progress.")) }
        item { Text(label(language,"AI membantu membandingkan perkiraan kata bacaan, bukan pengganti bimbingan guru atau penilai tajwid/makhraj tervalidasi.","AI helps compare estimated spoken words; it does not replace a teacher or validated tajwid assessment.")) }
        item { Text(label(language,"Teks, terjemahan, rekaman, dan foto tetap mengikuti atribusi sumber serta hak pemiliknya. Rincian sumber tersedia di Pengaturan → Sumber & atribusi.","Texts, translations, recordings, and photos retain source attribution and their owners’ rights. See Settings → Sources & attribution."),style=MaterialTheme.typography.bodySmall) }
    }
}
