package com.nuraya.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nuraya.app.NurayaApplication
import com.nuraya.app.prayer.PrayerDashboardCard
import com.nuraya.app.storage.UserEntry

@Composable
fun HomeScreen(app: NurayaApplication, language: String, entries: List<UserEntry>, onRead: (Int, Int) -> Unit, onNavigate: (String) -> Unit) {
    val lastRead = entries.filter { it.type == "read_history" }.maxByOrNull { it.updatedAt }
    LazyColumn(Modifier.testTag("home_menu"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text(label(language, "Assalamualaikum", "Assalamualaikum"), style = MaterialTheme.typography.titleLarge)
            Text(label(language, "Apa yang ingin Anda lakukan?", "What would you like to do?"), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { PrayerDashboardCard(language) { onNavigate("prayer") } }
        item {
            HomeAction("recitation", Icons.Outlined.Mic, label(language,"AI bacaan Al-Qur’an","Qur’an recitation AI"),
                label(language,"Baca dengan suara, lalu periksa kata","Read aloud and check the words"), primary = true) { onNavigate("recitation") }
        }
        item {
            HomeAction("quran", Icons.Outlined.MenuBook, label(language,"Baca Al-Qur’an","Read the Qur’an"),
                label(language,"Pilih cara membaca dan belajar","Choose a reading and learning mode")) { onNavigate("quran") }
        }
        if (lastRead != null) item {
            HomeAction("continue", Icons.Outlined.BookmarkBorder, label(language,"Lanjutkan bacaan","Continue reading"),
                "${lastRead.title} · ${lastRead.subtitle}") {
                val parts = lastRead.key.split(":")
                onRead(parts.getOrNull(0)?.toIntOrNull() ?: 1, parts.getOrNull(1)?.toIntOrNull() ?: 1)
            }
        }
        item {
            HomeAction("murottal", Icons.Outlined.Headphones, label(language,"Dengarkan murottal","Listen to recitation"),
                label(language,"Pilih qari, putar atau unduh surah","Choose a reciter, play or download a surah")) { onNavigate("murottal") }
        }
        item {
            HomeAction("practice", Icons.Outlined.School, label(language,"Hafalan, tajwid & kuis","Memorization, tajwid & quizzes"),
                label(language,"Latihan dan lihat perkembangan Anda","Practise and review your progress")) { onNavigate("practice") }
        }
        item {
            HomeAction("learn", Icons.Outlined.AutoStories, label(language,"Teman belajar","Learning companion"),
                label(language,"Cari ayat, terjemahan dan tafsir","Find verses, translations and tafsir")) { onNavigate("learn") }
        }
        item {
            HomeAction("library", Icons.Outlined.Bookmarks, label(language,"Koleksi saya","My collection"),
                label(language,"Penanda, audio tersimpan dan riwayat","Bookmarks, saved audio and history")) { onNavigate("library") }
        }
        item {
            HomeAction("settings", Icons.Outlined.Settings, label(language,"Pengaturan","Settings"),
                label(language,"Ukuran tulisan, warna dan bahasa","Text size, colors and language")) { onNavigate("settings") }
        }
        item {
            Text(label(language,"Gratis dan tanpa iklan. Penyusun tidak mengambil keuntungan finansial dari aplikasi ini.","Free and without ads. The creators do not earn financial profit from this application."),
                Modifier.fillMaxWidth().padding(vertical = 12.dp), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun HomeAction(key: String, icon: ImageVector, title: String, detail: String, primary: Boolean = false, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp).testTag("home_$key"),
        shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(
            containerColor = if (primary) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(24.dp))
        }
    }
}

@Composable fun SectionTitle(title: String) { Text(title, style = MaterialTheme.typography.titleLarge) }
@Composable fun IconTile(icon: ImageVector) {
    Box(Modifier.size(56.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
    }
}
