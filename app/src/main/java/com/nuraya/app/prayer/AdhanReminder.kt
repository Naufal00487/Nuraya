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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import java.time.format.DateTimeFormatter

internal val prayerReminderIds=setOf("fajr","dhuhr","asr","maghrib","isha")
data class PrayerReminder(val prayer:PrayerMoment,val before:Boolean) {
    val at:Instant get()=if(before)prayer.instant.minusSeconds(600) else prayer.instant
    val key:String get()="${prayer.id}:${at.toEpochMilli()}:$before"
}
internal fun nextPrayerReminder(now:Instant,location:PrayerLocation,correction:Int,selected:Set<String>):PrayerReminder? {
    val date=now.atZone(location.zone).toLocalDate()
    return (0L..2L).flatMap{PrayerCalculator.calculate(date.plusDays(it),location,correction).moments}
        .filter{it.isPrayer&&it.id in selected}.flatMap{listOf(PrayerReminder(it,true),PrayerReminder(it,false))}
        .filter{it.at.isAfter(now)}.minByOrNull{it.at}
}

object AdhanScheduler {
    const val ACTION="com.nuraya.app.ADHAN"
    private const val ID=4201
    internal fun preferences(context:Context)=context.getSharedPreferences("nuraya_adhan",Context.MODE_PRIVATE)
    /** Sound is independent of the per-prayer notification selections. */
    fun enabled(context:Context)=preferences(context).getBoolean("enabled",false)
    fun selected(context:Context):Set<String> = (preferences(context).getStringSet("prayers",null)
        ?: if(enabled(context))prayerReminderIds else emptySet()).intersect(prayerReminderIds)
    fun exactAllowed(context:Context)=Build.VERSION.SDK_INT<31||context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    fun permissionsAllowed(context:Context)=exactAllowed(context)&&NotificationManagerCompat.from(context).areNotificationsEnabled()
    fun setEnabled(context:Context,value:Boolean){
        val choices=selected(context).let{if(value&&it.isEmpty())prayerReminderIds else it}
        preferences(context).edit().putStringSet("prayers",choices).putBoolean("enabled",value).apply()
        if(!value)context.stopService(Intent(context,AdhanService::class.java))
        schedule(context)
    }
    fun setPrayerEnabled(context:Context,id:String,value:Boolean){
        require(id in prayerReminderIds)
        val choices=selected(context).toMutableSet().apply{if(value)add(id)else remove(id)}
        preferences(context).edit().putStringSet("prayers",choices).apply()
        if(!value)context.stopService(Intent(context,AdhanService::class.java))
        if(choices.isEmpty())setEnabled(context,false) else schedule(context)
    }
    fun reconcile(context:Context){
        if(!permissionsAllowed(context)){
            preferences(context).edit().putBoolean("enabled",false).putStringSet("prayers",emptySet()).apply()
            context.stopService(Intent(context,AdhanService::class.java))
        }
        schedule(context)
    }
    fun schedule(context:Context){
        val alarms=context.getSystemService(AlarmManager::class.java)
        val intent=Intent(context,AdhanReceiver::class.java).setAction(ACTION)
        PendingIntent.getBroadcast(context,ID,intent,PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let{alarms.cancel(it)}
        preferences(context).edit().remove("scheduled_key").apply()
        if(!permissionsAllowed(context))return
        val prefs=PrayerPreferenceStore(context).read()
        val location=prefs.location?:return
        val next=nextPrayerReminder(Instant.now(),location,prefs.minuteAdjustment,selected(context))?:return
        intent.putExtra("at",next.at.toEpochMilli()).putExtra("prayerAt",next.prayer.instant.toEpochMilli())
            .putExtra("name",next.prayer.nameId).putExtra("id",next.prayer.id).putExtra("before",next.before).putExtra("key",next.key)
        val alarm=PendingIntent.getBroadcast(context,ID,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try{
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.at.toEpochMilli(),alarm)
            preferences(context).edit().putString("scheduled_key",next.key).apply()
        }catch(_:SecurityException){/* A revoked system permission never starts audio. */}
    }
}
class AdhanReceiver:BroadcastReceiver(){
    override fun onReceive(context:Context,intent:Intent){
        if(intent.action!=AdhanScheduler.ACTION){AdhanScheduler.reconcile(context);return}
        val at=intent.getLongExtra("at",0L)
        val id=intent.getStringExtra("id").orEmpty()
        val location=PrayerPreferenceStore(context).read().location
        val valid=location!=null&&id in AdhanScheduler.selected(context)&&AdhanScheduler.permissionsAllowed(context)&&
            System.currentTimeMillis()-at in 0L..119_999L&&intent.getStringExtra("key")!=null&&
            intent.getStringExtra("key")==AdhanScheduler.preferences(context).getString("scheduled_key",null)
        AdhanScheduler.schedule(context)
        if(!valid)return
        val before=intent.getBooleanExtra("before",false)
        val notificationId=4300+prayerReminderIds.indexOf(id)
        if(!before&&AdhanScheduler.enabled(context)){
            try{
                ContextCompat.startForegroundService(context,Intent(context,AdhanService::class.java)
                    .putExtra("name",intent.getStringExtra("name")).putExtra("notificationId",notificationId))
                return
            }catch(_:RuntimeException){/* Preserve the time notification if the OS prevents audio. */}
        }
        val manager=context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("prayer_reminders","Pengingat salat",NotificationManager.IMPORTANCE_HIGH))
        val open=PendingIntent.getActivity(context,2,Intent(context,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE)
        val name=intent.getStringExtra("name").orEmpty()
        val time=Instant.ofEpochMilli(intent.getLongExtra("prayerAt",at)).atZone(location.zone).format(DateTimeFormatter.ofPattern("HH:mm"))
        val notification=NotificationCompat.Builder(context,"prayer_reminders").setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if(before)"Menjelang salat $name" else "Waktu salat $name")
            .setContentText("$name pukul $time · ${location.name}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(if(before)"Pengingat 10 menit sebelum salat. $name pukul $time · ${location.name}" else "Telah masuk waktu salat $name, pukul $time · ${location.name}"))
            .setContentIntent(open).setAutoCancel(true).setCategory(NotificationCompat.CATEGORY_REMINDER).build()
        manager.notify(notificationId,notification)
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
        startForeground(intent?.getIntExtra("notificationId",4202)?:4202,notification)
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

@Composable private fun rememberReminderRevision():Int {
    val context=LocalContext.current
    val preferences=remember{AdhanScheduler.preferences(context)}
    var revision by remember{mutableIntStateOf(0)}
    DisposableEffect(preferences){
        val listener=SharedPreferences.OnSharedPreferenceChangeListener{_,key->if(key=="prayers"||key=="enabled")revision++}
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose{preferences.unregisterOnSharedPreferenceChangeListener(listener)}
    }
    return revision
}
@Composable fun PrayerReminderSwitch(id:String,name:String,language:String){
    val context=LocalContext.current
    val revision=rememberReminderRevision()
    val checked=remember(revision,id){id in AdhanScheduler.selected(context)}
    var dialog by remember{mutableStateOf(false)}
    Switch(checked=checked,onCheckedChange={on->
        if(on&&!AdhanScheduler.permissionsAllowed(context))dialog=true else AdhanScheduler.setPrayerEnabled(context,id,on)
    },modifier=Modifier.testTag("reminder_$id").semantics{contentDescription=label(language,"Pengingat $name","$name reminder")})
    if(dialog)ReminderPermissionDialog(language,{dialog=false}){AdhanScheduler.setPrayerEnabled(context,id,true);dialog=false}
}
@Composable fun AdhanToggle(language:String){
    val context=LocalContext.current
    val revision=rememberReminderRevision()
    val enabled=remember(revision){AdhanScheduler.enabled(context)}
    var dialog by remember{mutableStateOf(false)}
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle,context){
        val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_RESUME)AdhanScheduler.reconcile(context)}
        lifecycle.addObserver(observer)
        onDispose{lifecycle.removeObserver(observer)}
    }
    FilledTonalIconButton(onClick={if(enabled)AdhanScheduler.setEnabled(context,false)else dialog=true},modifier=Modifier.size(56.dp).testTag("adhan_toggle"),colors=IconButtonDefaults.filledTonalIconButtonColors(containerColor=Color(0xFFF4E6B9),contentColor=Color(0xFF5E4922))){
        Icon(if(enabled)Icons.AutoMirrored.Outlined.VolumeUp else Icons.AutoMirrored.Outlined.VolumeOff,label(language,if(enabled)"Nonaktifkan suara azan" else "Aktifkan suara azan",if(enabled)"Disable adhan sound" else "Enable adhan sound"))
    }
    if(dialog)ReminderPermissionDialog(language,{dialog=false}){AdhanScheduler.setEnabled(context,true);dialog=false}
}
@Composable private fun ReminderPermissionDialog(language:String,onDismiss:()->Unit,onGranted:()->Unit){
    val context=LocalContext.current
    var message by remember{mutableStateOf("")}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->message=if(granted)label(language,"Izin notifikasi aktif. Tekan Aktifkan untuk melanjutkan.","Notifications enabled. Tap Enable to continue.")else label(language,"Izin notifikasi diperlukan untuk pengingat salat.","Notification permission is needed for prayer reminders.")}
    AlertDialog(onDismissRequest=onDismiss,title={Text(label(language,"Pengingat salat","Prayer reminders"))},text={Text(message.ifBlank{label(language,"Pengingat muncul 10 menit sebelum dan saat waktu salat. Pilih salat yang diingatkan di Ibadah. Suara azan mengikuti tombol pengeras suara dan volume alarm HP; mematikannya tetap mempertahankan pilihan notifikasi.","Receive reminders ten minutes before and at prayer time. Choose prayers under Prayer. Adhan sound follows the speaker switch and alarm volume; muting it keeps notification selections.")})},confirmButton={TextButton(onClick={
        when{
            PrayerPreferenceStore(context).read().location==null->message=label(language,"Pilih lokasi terlebih dahulu di Beranda atau Ibadah.","Choose a location under Home or Prayer first.")
            Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED->permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            !AdhanScheduler.exactAllowed(context)->{message=label(language,"Izinkan alarm tepat waktu, lalu kembali dan tekan Aktifkan.","Allow exact alarms, return, then tap Enable.");runCatching{context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,android.net.Uri.parse("package:${context.packageName}")))}}
            !NotificationManagerCompat.from(context).areNotificationsEnabled()->{message=label(language,"Aktifkan notifikasi Nuraya di pengaturan HP.","Enable Nuraya notifications in device settings.");runCatching{context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName))}}
            else->onGranted()
        }
    }){Text(label(language,"Aktifkan","Enable"))}},dismissButton={TextButton(onClick=onDismiss){Text(label(language,"Batal","Cancel"))}})
}
