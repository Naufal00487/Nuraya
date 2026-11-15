package com.nuraya.app.reminders

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nuraya.app.prayer.*
import com.nuraya.app.ui.label

@Composable private fun reminderRevision():Int {
    val context=LocalContext.current;val lifecycle=LocalLifecycleOwner.current.lifecycle
    var revision by remember{mutableIntStateOf(0)}
    DisposableEffect(context,lifecycle) {
        val stores=listOf(AdhanScheduler.preferences(context),QuranReminderScheduler.preferences(context))
        val listener=SharedPreferences.OnSharedPreferenceChangeListener{_,_->revision++}
        stores.forEach{it.registerOnSharedPreferenceChangeListener(listener)}
        val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_RESUME)revision++}
        lifecycle.addObserver(observer)
        onDispose{stores.forEach{it.unregisterOnSharedPreferenceChangeListener(listener)};lifecycle.removeObserver(observer)}
    }
    return revision
}
@Composable fun ReminderSettings(language:String) {
    val context=LocalContext.current;val revision=reminderRevision()
    var permissionRevision by remember { mutableIntStateOf(0) }
    val allowed=remember(revision,permissionRevision){NotificationManagerCompat.from(context).areNotificationsEnabled()}
    val quran=remember(revision){QuranReminderScheduler.enabled(context)}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        AdhanScheduler.reconcile(context);QuranReminderScheduler.schedule(context);permissionRevision++
    }
    Column(verticalArrangement=Arrangement.spacedBy(16.dp),modifier=Modifier.testTag("reminder_settings")) {
        Text(label(language,if(allowed)"Izin notifikasi aktif" else "Izin notifikasi belum aktif",if(allowed)"Notifications are allowed" else "Notifications are not allowed"),style=MaterialTheme.typography.titleMedium)
        TextButton(onClick={
            if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else runCatching{context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName))}
        }){Text(label(language,"Atur izin notifikasi","Manage notification permission"))}
        Row(verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(label(language,"Pengingat Al-Qur’an","Quran reminders"),style=MaterialTheme.typography.titleMedium)
                Text(label(language,"Dua pesan acak antara 08.00–20.00, berjeda minimal tiga jam.","Two random messages between 08:00–20:00, at least three hours apart."),style=MaterialTheme.typography.bodyMedium)
            }
            Switch(quran,onCheckedChange={QuranReminderScheduler.setEnabled(context,it)},modifier=Modifier.testTag("quran_reminder_toggle"))
        }
        HorizontalDivider()
        Text(label(language,"Notifikasi salat","Prayer notifications"),style=MaterialTheme.typography.titleMedium)
        val ids=listOf("fajr","dhuhr","asr","maghrib","isha")
        val names=if(language=="en")listOf("Fajr","Dhuhr","Asr","Maghrib","Isha") else listOf("Subuh","Zuhur","Asar","Magrib","Isya")
        ids.forEachIndexed { index,id -> Row(verticalAlignment=Alignment.CenterVertically) { Text(names[index],Modifier.weight(1f));PrayerReminderSwitch(id,names[index],language) } }
        PrayerTimingControls(language)
        Text(label(language,"Pengingat dihitung dan disimpan di HP. Pengiriman dapat terlambat jika Android membatasi aktivitas latar belakang.","Reminders are calculated and stored on your phone. Android background restrictions can delay delivery."),style=MaterialTheme.typography.bodySmall)
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable fun PrayerTimingControls(language:String) {
    val context=LocalContext.current;val revision=reminderRevision()
    val minutes=remember(revision){AdhanScheduler.beforeMinutes(context)}
    val sound=remember(revision){AdhanScheduler.enabled(context)}
    var permission by remember{mutableStateOf(false)}
    Column(verticalArrangement=Arrangement.spacedBy(12.dp),modifier=Modifier.testTag("prayer_timing_controls")) {
        Text(label(language,"Ingatkan sebelum salat","Remind me before prayer"),style=MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf(5,10,15,30).forEach { value -> FilterChip(minutes==value,onClick={AdhanScheduler.setBeforeMinutes(context,value)},label={Text("$value ${label(language,"menit","min")}")},modifier=Modifier.testTag("reminder_minutes_$value")) }
        }
        Row(verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(label(language,"Alarm suara azan","Adhan audio alarm"),style=MaterialTheme.typography.titleMedium)
                Text(label(language,"Opsional. Notifikasi salat tetap bekerja saat alarm dimatikan.","Optional. Prayer notifications still work when the alarm is off."),style=MaterialTheme.typography.bodyMedium)
            }
            Switch(sound,onCheckedChange={if(it)permission=true else AdhanScheduler.setEnabled(context,false)},modifier=Modifier.testTag("adhan_alarm_toggle"))
        }
    }
    if(permission)ReminderPermissionDialog(language,{permission=false},alarm=true){AdhanScheduler.setEnabled(context,true);permission=false}
}
