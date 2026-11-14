package com.nuraya.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nuraya.app.NurayaApplication
import com.nuraya.app.prayer.PrayerHero
import com.nuraya.app.profile.rememberProfile
import com.nuraya.app.storage.UserEntry
import com.nuraya.app.banners.BannerCarousel

@Composable
fun HomeScreen(app: NurayaApplication, language: String, entries: List<UserEntry>, onRead: (Int, Int) -> Unit, onNavigate: (String) -> Unit) {
    val profile=rememberProfile()
    val lastRead=entries.filter{it.type=="read_history"}.maxByOrNull{it.updatedAt}
    val menu=listOf(
        Triple("quran",Icons.Outlined.MenuBook,label(language,"Baca Al-Qur’an","Read the Qur’an")),
        Triple("recitation",Icons.Outlined.Mic,label(language,"AI bacaan Al-Qur’an","Qur’an recitation AI")),
        Triple("practice",Icons.Outlined.School,label(language,"Hafalan, tajwid & kuis","Memorization, tajwid & quizzes")),
        Triple("learn",Icons.Outlined.AutoStories,label(language,"Cari ayat & makna","Find verses & meaning")),
        Triple("murottal",Icons.Outlined.Headphones,label(language,"Dengarkan murottal","Listen to recitation")),
        Triple("library",Icons.Outlined.Bookmarks,label(language,"Koleksi saya","My collection")))
    LazyColumn(Modifier.testTag("home_menu"),contentPadding=PaddingValues(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        item { PrayerHero(language,onNavigate) }
        item { Column(Modifier.padding(horizontal=20.dp)){
            Text("Assalamualaikum,",style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text(profile.nickname,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        } }
        item { BannerCarousel(language,onNavigate,Modifier.padding(horizontal=20.dp)) }
        item { Text(label(language,"Temani langkah belajarmu","Make time for learning"),Modifier.padding(horizontal=20.dp),style=MaterialTheme.typography.titleLarge) }
        items(menu.chunked(2)){ row -> Row(Modifier.fillMaxWidth().padding(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){
            row.forEach{(key,icon,title)->HomeTile(key,icon,title,Modifier.weight(1f)){onNavigate(key)}}
        } }
        if(lastRead!=null)item {
            Card(onClick={val parts=lastRead.key.split(":");onRead(parts.getOrNull(0)?.toIntOrNull()?:1,parts.getOrNull(1)?.toIntOrNull()?:1)},modifier=Modifier.padding(horizontal=20.dp).fillMaxWidth().testTag("home_continue"),shape=RoundedCornerShape(22.dp)) {
                Row(Modifier.padding(20.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.BookmarkBorder,null);Column(Modifier.weight(1f).padding(horizontal=12.dp)){
                    Text(label(language,"Lanjutkan bacaan","Continue reading"),style=MaterialTheme.typography.titleMedium)
                    Text("${lastRead.title} · ${lastRead.subtitle}",style=MaterialTheme.typography.bodySmall)
                };Icon(Icons.AutoMirrored.Outlined.ArrowForward,null)}
            }
        }
        item { Row(Modifier.fillMaxWidth().padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically){
            Text(label(language,"Lantunan pilihan","Reciters to explore"),Modifier.weight(1f),style=MaterialTheme.typography.titleLarge)
            TextButton(onClick={onNavigate("murottal")}){Text(label(language,"Lihat semua","See all"))}
        } }
        item { LazyRow(contentPadding=PaddingValues(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(18.dp)){
            items(app.quran.qariChoices.take(12),key={it.id}){qari-> Column(Modifier.width(112.dp).clickable{onNavigate("qari:${qari.id}")}.padding(4.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){
                QariPortrait(qari.id,qari.name,language,Modifier.size(88.dp));Text(qari.name,maxLines=3,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.labelLarge,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
            }}
        } }
        item { TextButton(onClick={onNavigate("settings")},modifier=Modifier.fillMaxWidth().testTag("home_settings")){Icon(Icons.Outlined.Settings,null);Spacer(Modifier.width(8.dp));Text(label(language,"Pengaturan membaca","Reading settings"))} }
    }
}
@Composable private fun HomeTile(key:String,icon:ImageVector,title:String,modifier:Modifier=Modifier,onClick:()->Unit){
    Card(onClick=onClick,modifier=modifier.heightIn(min=142.dp).testTag("home_$key"),shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerLow)){
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
            Box(Modifier.size(44.dp).background(MaterialTheme.colorScheme.primaryContainer,RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center){Icon(icon,null,tint=MaterialTheme.colorScheme.primary)}
            Text(title,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.SemiBold)
        }
    }
}
@Composable fun SectionTitle(title:String){Text(title,style=MaterialTheme.typography.titleLarge)}
@Composable fun IconTile(icon:ImageVector){Box(Modifier.size(56.dp).background(MaterialTheme.colorScheme.primaryContainer,RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center){Icon(icon,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(28.dp))}}
