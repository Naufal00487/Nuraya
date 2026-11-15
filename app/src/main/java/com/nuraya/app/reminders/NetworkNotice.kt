package com.nuraya.app.reminders

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuraya.app.ui.label
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object NetworkNotice {
    private val mutable=MutableStateFlow<Long?>(null)
    val events=mutable.asStateFlow()
    private var lastShown:Long?=null
    fun connected(context:Context):Boolean=runCatching{
        val manager=context.getSystemService(ConnectivityManager::class.java)
        val network=manager.activeNetwork?:return@runCatching false
        val capabilities=manager.getNetworkCapabilities(network)?:return@runCatching false
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)&&capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }.getOrDefault(false)
    @Synchronized fun warnIfOffline(context:Context) {
        if(connected(context))return
        val now=SystemClock.elapsedRealtime()
        if(lastShown!=null&&now-lastShown!! in 0..14_999)return
        lastShown=now;mutable.value=now
    }
    fun dismiss(value:Long){if(mutable.value==value)mutable.value=null}
}
@Composable fun OfflineNotice(language:String,allowedToShow:Boolean=true) {
    val context=LocalContext.current
    val event by NetworkNotice.events.collectAsStateWithLifecycle()
    LaunchedEffect(event){event?.let{delay(8_000);NetworkNotice.dismiss(it)}}
    event?.let { value -> if(allowedToShow && android.os.SystemClock.elapsedRealtime()-value in 0..8_000) NoticePopup(onDismissRequest={NetworkNotice.dismiss(value)},modifier=Modifier.testTag("offline_notice"),
        title={Text(label(language,"Belum terhubung ke internet","No internet connection"))},
        text={Text(label(language,"Fitur ini membutuhkan koneksi. Hubungkan Wi-Fi atau data seluler, lalu coba kembali.","This feature needs a connection. Connect to Wi-Fi or mobile data, then retry."))},
        confirmButton={TextButton(onClick={runCatching{context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))}}){Text(label(language,"Hubungkan","Connect"))}},
        dismissButton={TextButton(onClick={NetworkNotice.dismiss(value)}){Text(label(language,"Tutup","Close"))}}) }
}
