package com.nuraya.app.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nuraya.app.ui.label

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileOnboarding(language: String, onComplete: () -> Unit) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf("") }
    var avatar by rememberSaveable { mutableIntStateOf(0) }
    BackHandler { /* Complete the local profile before entering the application. */ }
    Surface(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        LazyColumn(Modifier.fillMaxSize().testTag("profile_onboarding"), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Spacer(Modifier.height(24.dp))
                    Text(label(language, "Selamat datang di Nuraya", "Welcome to Nuraya"), style = MaterialTheme.typography.headlineMedium)
                    Text(label(language, "Isi nama panggilan untuk memulai perjalanan belajarmu.", "Enter a nickname to start your learning journey."), style = MaterialTheme.typography.bodyLarge)
                }
            }
            item { OutlinedTextField(name, { name = it.take(30) }, label = { Text(label(language, "Nama panggilan wajib diisi", "Nickname required")) }, singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("onboarding_name")) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(label(language, "Pilih avatar", "Choose an avatar"), style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(5) { index ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { avatar = index }.padding(4.dp)) {
                                ProfileAvatar(index, Modifier.size(56.dp))
                                if (avatar == index) Text(label(language, "Dipilih", "Selected"), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            item { Button(onClick = { ProfileStore(context).save(name, avatar); onComplete() }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("onboarding_continue")) {
                Text(label(language, "Simpan & mulai", "Save & start"))
            } }
            item { Text(label(language, "Nama dan progres disimpan di HP ini. Tidak perlu membuat akun online.", "Your name and progress stay on this phone. No online account is needed."), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
