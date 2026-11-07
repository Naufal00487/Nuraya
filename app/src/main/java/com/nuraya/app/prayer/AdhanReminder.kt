package com.nuraya.app.prayer

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.media.*
import android.os.*
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nuraya.app.MainActivity
import com.nuraya.app.R
import com.nuraya.app.ui.label
import java.time.Instant

object AdhanScheduler {
    const val ACTION="com.nuraya.app.ADHAN"
    private const val ID=4201
    fun enabled(context:Context)=context.getSharedPreferences("nuraya_adhan",Context.MODE_PRIVATE).getBoolean("enabled",false)
    fun exactAllowed(context:Context)=Build.VERSION.SDK_INT<31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    fun permissionsAllowed(context:Context)=exactAllowed(context)&&NotificationManagerCompat.from(context).areNotificationsEnabled()
    /** Recheck after returning from Android settings, reboot, or an application update. */
    fun reconcile(context:Context){
        if(enabled(context)&&!permissionsAllowed(context))setEnabled(context,false) else schedule(context)
    }
    fun setEnabled(context:Context,value:Boolean){
        context.getSharedPreferences("nuraya_adhan",Context.MODE_PRIVATE).edit().putBoolean("enabled",value).apply()
        if(!value)context.stopService(Intent(context,AdhanService::class.java))
        schedule(context)
    }
    fun schedule(context:Context){
        val alarms=context.getSystemService(AlarmManager::class.java)
        val intent=Intent(context,AdhanReceiver::class.java).setAction(ACTION)
        val previous=PendingIntent.getBroadcast(context,ID,intent,PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if(previous!=null)alarms.cancel(previous)
        if(!enabled(context)||!permissionsAllowed(context))return
        val prefs=PrayerPreferenceStore(context).read()
        val location=prefs.location?:return
        val next=PrayerCalculator.next(Instant.now(),location,prefs.minuteAdjustment)?:return
        intent.putExtra("at",next.instant.toEpochMilli()).putExtra("name",next.nameId)
        val alarm=PendingIntent.getBroadcast(context,ID,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try{alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.instant.toEpochMilli(),alarm)}catch(_:SecurityException){/* System permission revoked: remain silent. */}
    }
}
class AdhanReceiver:BroadcastReceiver(){
    override fun onReceive(context:Context,intent:Intent){
        if(intent.action!=AdhanScheduler.ACTION){AdhanScheduler.reconcile(context);return}
        val at=intent.getLongExtra("at",0)
        val shouldPlay=AdhanScheduler.enabled(context)&&AdhanScheduler.permissionsAllowed(context)&&PrayerPreferenceStore(context).read().location!=null&&kotlin.math.abs(System.currentTimeMillis()-at)<120_000
        AdhanScheduler.schedule(context)
        if(!shouldPlay||!NotificationManagerCompat.from(context).areNotificationsEnabled())return
        try{ContextCompat.startForegroundService(context,Intent(context,AdhanService::class.java).putExtra("name",intent.getStringExtra("name")))}catch(_:RuntimeException){/* Respect platform background/permission restrictions. */}
    }
}
class AdhanService:Service(){
    private var player:MediaPlayer?=null
    private var focus:AudioFocusRequest?=null
    override fun onBind(intent:Intent?)=null
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{
        if(intent?.action=="stop"||!AdhanScheduler.enabled(this)){stopSelf();return START_NOT_STICKY}
        val manager=getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("adhan_playback","Azan salat",NotificationManager.IMPORTANCE_LOW).apply{setSound(null,null)})
        val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE)
        val stop=PendingIntent.getService(this,1,Intent(this,AdhanService::class.java).setAction("stop"),PendingIntent.FLAG_IMMUTABLE)
        val notification=NotificationCompat.Builder(this,"adhan_playback").setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Waktu ${intent?.getStringExtra("name")?:"salat"}").setContentText("Azan · Nuraya").setContentIntent(open)
            .setOngoing(true).addAction(0,"Hentikan azan",stop).build()
        startForeground(4202,notification)
        player?.release()
        focus?.let{getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it)}
        val attributes=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
        val audio=getSystemService(AudioManager::class.java)
        focus=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(attributes).setOnAudioFocusChangeListener{change->if(change<0)stopSelf()}.build()
        if(audio.requestAudioFocus(focus!!)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){stopSelf();return START_NOT_STICKY}
        try{
            player=MediaPlayer().apply{
                setAudioAttributes(attributes);setWakeMode(this@AdhanService,PowerManager.PARTIAL_WAKE_LOCK)
                resources.openRawResourceFd(R.raw.adhan_mecca).use{setDataSource(it.fileDescriptor,it.startOffset,it.length)}
                setOnCompletionListener{stopSelf()};setOnErrorListener{_,_,_->stopSelf();true};prepare();start()
            }
        }catch(_:Exception){stopSelf()}
        return START_NOT_STICKY
    }
    override fun onDestroy(){player?.release();player=null;focus?.let{getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it)};stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy()}
}
@Composable fun AdhanToggle(language:String){
    val context=LocalContext.current
    var enabled by remember{mutableStateOf(AdhanScheduler.enabled(context))}
    var dialog by remember{mutableStateOf(false)}
    var message by remember{mutableStateOf("")}
    val preferences=remember{context.getSharedPreferences("nuraya_adhan",Context.MODE_PRIVATE)}
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle,context){
        val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_RESUME)AdhanScheduler.reconcile(context)}
        lifecycle.addObserver(observer)
        if(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))AdhanScheduler.reconcile(context)
        onDispose{lifecycle.removeObserver(observer)}
    }
    DisposableEffect(preferences){val listener=SharedPreferences.OnSharedPreferenceChangeListener{_,_->enabled=AdhanScheduler.enabled(context)};preferences.registerOnSharedPreferenceChangeListener(listener);onDispose{preferences.unregisterOnSharedPreferenceChangeListener(listener)}}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->message=if(granted)label(language,"Izin notifikasi aktif. Tekan Aktifkan untuk melanjutkan.","Notifications enabled. Tap Enable to continue.") else label(language,"Izin notifikasi diperlukan untuk pengingat azan.","Notification permission is needed for adhan reminders.")}
    FilledTonalIconButton(onClick={if(enabled){AdhanScheduler.setEnabled(context,false);enabled=false}else dialog=true},modifier=Modifier.size(56.dp).testTag("adhan_toggle"),colors=IconButtonDefaults.filledTonalIconButtonColors(containerColor=Color(0xFFF4E6B9),contentColor=Color(0xFF5E4922))){
        Icon(if(enabled)Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff,label(language,if(enabled)"Nonaktifkan azan" else "Aktifkan azan",if(enabled)"Disable adhan" else "Enable adhan"))
    }
    if(dialog)AlertDialog(onDismissRequest={dialog=false},title={Text(label(language,"Pengingat azan","Adhan reminders"))},text={Text(message.ifBlank{label(language,"Putar azan pada lima waktu salat sesuai lokasi tersimpan. Mengikuti volume alarm HP. Izin notifikasi dan alarm tepat waktu diperlukan; pengingat awalnya nonaktif.","Play adhan at the five prayer times for your saved location, using alarm volume. Notification and exact alarm permissions are needed; reminders are off by default.")})},confirmButton={TextButton(onClick={
        when{
            PrayerPreferenceStore(context).read().location==null -> message=label(language,"Pilih lokasi terlebih dahulu melalui tombol lokasi di beranda.","Choose a location from the home screen first.")
            Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED -> permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            !AdhanScheduler.exactAllowed(context) -> {message=label(language,"Izinkan alarm tepat waktu, lalu kembali dan tekan Aktifkan.","Allow exact alarms, return, then tap Enable.");runCatching{context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,android.net.Uri.parse("package:${context.packageName}")))}}
            !NotificationManagerCompat.from(context).areNotificationsEnabled() -> {message=label(language,"Aktifkan notifikasi Nuraya di pengaturan HP.","Enable Nuraya notifications in device settings.");runCatching{context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName))}}
            else -> {AdhanScheduler.setEnabled(context,true);enabled=true;dialog=false;message=""}
        }
    }){Text(label(language,"Aktifkan","Enable"))}},dismissButton={TextButton(onClick={dialog=false}){Text(label(language,"Batal","Cancel"))}})
}
