package com.nuraya.app.reminders

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nuraya.app.prayer.AdhanScheduler
import com.nuraya.app.ui.label

class PermissionPromptStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("nuraya_permission_prompts", Context.MODE_PRIVATE)
    fun notificationDue(now: Long = System.currentTimeMillis()): Boolean {
        val last = prefs.getLong("notifications_shown", 0)
        return last == 0L || now >= last && now-last >= 7*24*3600_000L
    }
    fun markNotificationsShown(now: Long = System.currentTimeMillis()) { prefs.edit().putLong("notifications_shown", now).commit() }
}

/** A compact, non-modal window. Only its rectangle receives touches; no screen scrim. */
@Composable
fun NoticePopup(onDismissRequest: () -> Unit, title: @Composable () -> Unit, text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit, dismissButton: @Composable () -> Unit = {}, modifier: Modifier = Modifier) {
    val maximumHeight = (LocalConfiguration.current.screenHeightDp * .62f).coerceAtMost(440f).dp
    val density = LocalDensity.current
    Popup(alignment = Alignment.BottomCenter, onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = false, dismissOnBackPress = false, dismissOnClickOutside = false)) {
        CompositionLocalProvider(LocalDensity provides density) {
        Surface(modifier.padding(horizontal=16.dp,vertical=12.dp).navigationBarsPadding().widthIn(max=520.dp).fillMaxWidth().heightIn(max=maximumHeight),
            shape = RoundedCornerShape(14.dp), color=MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation=3.dp, shadowElevation=8.dp) {
            Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
                ProvideTextStyle(MaterialTheme.typography.titleMedium) { title() }
                Box(Modifier.weight(1f,fill=false).verticalScroll(rememberScrollState())) { ProvideTextStyle(MaterialTheme.typography.bodyMedium) { text() } }
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.End, verticalAlignment=Alignment.CenterVertically) { dismissButton(); Spacer(Modifier.width(8.dp)); confirmButton() }
            }
        }
        }
    }
}

@Composable
fun NotificationPermissionNotice(language: String, allowedToShow: Boolean) {
    val context=LocalContext.current
    val store=remember(context){PermissionPromptStore(context)}
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    var revision by remember { mutableIntStateOf(0) }
    var showing by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf(false) }
    val launch=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        showing=false; dismissed=true; AdhanScheduler.reconcile(context); QuranReminderScheduler.schedule(context); revision++
    }
    DisposableEffect(lifecycle) {
        val observer=LifecycleEventObserver { _,event -> if(event==Lifecycle.Event.ON_RESUME) { revision++; if(store.notificationDue())dismissed=false; if(NotificationManagerCompat.from(context).areNotificationsEnabled())showing=false } }
        lifecycle.addObserver(observer);onDispose{lifecycle.removeObserver(observer)}
    }
    LaunchedEffect(allowedToShow,revision,dismissed) {
        if(!allowedToShow&&showing) { showing=false;dismissed=true }
        if(allowedToShow&&!dismissed&&!NotificationManagerCompat.from(context).areNotificationsEnabled()&&store.notificationDue()) {
            showing=true;store.markNotificationsShown()
        }
    }
    if(showing&&allowedToShow) NoticePopup(onDismissRequest={showing=false;dismissed=true},
        modifier=Modifier.testTag("notification_permission_notice"),
        title={Text(label(language,"Aktifkan notifikasi Nuraya","Enable Nuraya notifications"))},
        text={Text(label(language,"Dapatkan pengingat salat dan dua pesan Al-Qur’an setiap hari. Alarm suara azan tetap mati sampai kamu mengaktifkannya sendiri.","Get prayer reminders and two daily Quran messages. Adhan audio remains off until you enable it."))},
        confirmButton={TextButton(onClick={
            if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)launch.launch(Manifest.permission.POST_NOTIFICATIONS)
            else runCatching{context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName))}
        },modifier=Modifier.testTag("notification_permission_enable")){Text(label(language,"Aktifkan","Enable"))}},
        dismissButton={TextButton(onClick={showing=false;dismissed=true},modifier=Modifier.testTag("notification_permission_later")){Text(label(language,"Nanti","Later"))}})
}
