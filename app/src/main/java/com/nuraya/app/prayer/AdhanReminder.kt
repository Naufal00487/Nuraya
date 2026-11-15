package com.nuraya.app.prayer

import com.nuraya.app.ui.icons.AppIcons as Icons
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
data class PrayerReminder(val prayer:PrayerMoment,val before:Boolean,val minutes:Int=10) {
    val at:Instant get()=if(before)prayer.instant.minusSeconds(minutes * 60L) else prayer.instant
    val key:String get()="${prayer.id}:${at.toEpochMilli()}:$before"
}
internal fun nextPrayerReminder(now:Instant,location:PrayerLocation,correction:Int,selected:Set<String>,minutes:Int=10):PrayerReminder? {
    val date=now.atZone(location.zone).toLocalDate()
    return (0L..2L).flatMap{PrayerCalculator.calculate(date.plusDays(it),location,correction).moments}
        .filter{it.isPrayer&&it.id in selected}.flatMap{listOf(PrayerReminder(it,true,minutes),PrayerReminder(it,false,minutes))}
        .filter{it.at.isAfter(now)}.minByOrNull{it.at}
}

object AdhanScheduler {
    const val ACTION="com.nuraya.app.ADHAN"
    private const val ID=4201
    internal fun preferences(context:Context)=context.getSharedPreferences("nuraya_adhan",Context.MODE_PRIVATE)
    /** Sound is independent of the per-prayer notification selections. */
    fun enabled(context:Context)=preferences(context).getBoolean("enabled",false)
    fun selected(context:Context):Set<String> = (preferences(context).getStringSet("prayers",null)
        ?: prayerReminderIds).intersect(prayerReminderIds)
    fun exactAllowed(context:Context)=Build.VERSION.SDK_INT<31||context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    fun permissionsAllowed(context:Context)=NotificationManagerCompat.from(context).areNotificationsEnabled()
    fun beforeMinutes(context:Context)=preferences(context).getInt("before_minutes",10).takeIf{it in listOf(5,10,15,30)}?:10
    fun setBeforeMinutes(context:Context,value:Int){require(value in listOf(5,10,15,30));preferences(context).edit().putInt("before_minutes",value).apply();schedule(context)}
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
        if(!permissionsAllowed(context)||!exactAllowed(context)){
            preferences(context).edit().putBoolean("enabled",false).apply()
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
        val next=nextPrayerReminder(Instant.now(),location,prefs.minuteAdjustment,selected(context),beforeMinutes(context))?:return
        intent.putExtra("at",next.at.toEpochMilli()).putExtra("prayerAt",next.prayer.instant.toEpochMilli())
            .putExtra("minutes",next.minutes).putExtra("name",next.prayer.nameId).putExtra("id",next.prayer.id).putExtra("before",next.before).putExtra("key",next.key)
        val alarm=PendingIntent.getBroadcast(context,ID,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try{
            if(exactAllowed(context))alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.at.toEpochMilli(),alarm)
            else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.at.toEpochMilli(),alarm)
            preferences(context).edit().putString("scheduled_key",next.key).apply()
        }catch(_:SecurityException){
            preferences(context).edit().putBoolean("enabled",false).apply()
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.at.toEpochMilli(),alarm)
            preferences(context).edit().putString("scheduled_key",next.key).apply()
        }
    }
}
class AdhanReceiver:BroadcastReceiver(){
    override fun onReceive(context:Context,intent:Intent){
        if(intent.action!=AdhanScheduler.ACTION){AdhanScheduler.reconcile(context);com.nuraya.app.reminders.QuranReminderScheduler.schedule(context);return}
        val now=System.currentTimeMillis()
        val at=intent.getLongExtra("at",0L)
        val id=intent.getStringExtra("id").orEmpty()
        val settings=PrayerPreferenceStore(context).read()
        val location=settings.location
        val prayerAt=intent.getLongExtra("prayerAt",at)
        val before=intent.getBooleanExtra("before",false)&&prayerAt>now
        val deliveryKey="$id:$prayerAt:$before"
        val preferences=AdhanScheduler.preferences(context)
        val valid=location!=null&&id in AdhanScheduler.selected(context)&&AdhanScheduler.permissionsAllowed(context)&&
            now-at in 0L..2_700_000L&&intent.getStringExtra("key")!=null&&
            intent.getStringExtra("key")==preferences.getString("scheduled_key",null)&&
            preferences.getString("delivered_$id",null)!=deliveryKey
        AdhanScheduler.schedule(context)
        if(!valid)return
        preferences.edit().putString("delivered_$id",deliveryKey).apply()
        val notificationId=4300+prayerReminderIds.indexOf(id)
        if(!before&&AdhanScheduler.enabled(context)&&AdhanScheduler.exactAllowed(context)&&now-prayerAt in 0L..119_999L){
            try{
                ContextCompat.startForegroundService(context,Intent(context,AdhanService::class.java)
                    .putExtra("name",intent.getStringExtra("name")).putExtra("notificationId",notificationId))
                return
            }catch(_:RuntimeException){/* Keep the ordinary notification if Android prevents audio. */}
        }
        val manager=context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("prayer_reminders","Pengingat salat",NotificationManager.IMPORTANCE_HIGH))
        val name=intent.getStringExtra("name").orEmpty()
        val local=Instant.ofEpochMilli(prayerAt).atZone(requireNotNull(location).zone)
        val time=local.format(DateTimeFormatter.ofPattern("HH:mm"))
        // If Android delays an inexact notification, never promise an outdated countdown.
        val minutes=kotlin.math.ceil((prayerAt-now)/60_000.0).toInt().coerceIn(1,AdhanScheduler.beforeMinutes(context))
        val text=if(before)com.nuraya.app.reminders.ReminderContent(context).beforePrayer(id,name,minutes,local.toLocalDate(),settings.hijriAdjustment)
            else com.nuraya.app.reminders.ReminderText("Waktu salat $name","Telah masuk waktu salat $name.")
        val body="${text.body}\n$name pukul $time · ${location.name}"
        val notification=NotificationCompat.Builder(context,"prayer_reminders").setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(text.title).setContentText(text.body).setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(com.nuraya.app.reminders.ReminderNavigation.pending(context,"prayer/$id"))
            .setAutoCancel(true).setCategory(NotificationCompat.CATEGORY_REMINDER).build()
        runCatching{manager.notify(notificationId,notification)}
    }
}
class AdhanService:Service(){
    private var player:MediaPlayer?=null
    private var focus:AudioFocusRequest?=null
    override fun onBind(intent:Intent?)=null
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{
        if(intent?.action=="stop"||!AdhanScheduler.enabled(this)||!AdhanScheduler.exactAllowed(this)||!AdhanScheduler.permissionsAllowed(this)){stopSelf();return START_NOT_STICKY}
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
        val listener=SharedPreferences.OnSharedPreferenceChangeListener{_,key->if(key=="prayers"||key=="enabled"||key=="before_minutes")revision++}
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
    FilledTonalIconButton(onClick={if(enabled)AdhanScheduler.setEnabled(context,false)else dialog=true},modifier=Modifier.size(56.dp).testTag("adhan_toggle"),colors=IconButtonDefaults.filledTonalIconButtonColors(containerColor=MaterialTheme.colorScheme.primaryContainer,contentColor=MaterialTheme.colorScheme.onPrimaryContainer)){
        Icon(if(enabled)Icons.AutoMirrored.Outlined.VolumeUp else Icons.AutoMirrored.Outlined.VolumeOff,label(language,if(enabled)"Nonaktifkan suara azan" else "Aktifkan suara azan",if(enabled)"Disable adhan sound" else "Enable adhan sound"))
    }
    if(dialog)ReminderPermissionDialog(language,{dialog=false},alarm=true){AdhanScheduler.setEnabled(context,true);dialog=false}
}
@Composable internal fun ReminderPermissionDialog(language:String,onDismiss:()->Unit,alarm:Boolean=false,onGranted:()->Unit){
    val context=LocalContext.current
    var message by remember{mutableStateOf("")}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->message=if(granted)label(language,"Izin notifikasi aktif. Tekan Aktifkan untuk melanjutkan.","Notifications enabled. Tap Enable to continue.")else label(language,"Izin notifikasi diperlukan untuk pengingat salat.","Notification permission is needed for prayer reminders.")}
    com.nuraya.app.reminders.NoticePopup(onDismissRequest=onDismiss,title={Text(label(language,"Pengingat salat","Prayer reminders"))},text={Text(message.ifBlank{label(language,"Notifikasi salat dan alarm suara azan diatur terpisah. Notifikasi tidak memerlukan izin alarm. Suara azan opsional, mengikuti volume alarm HP, dan memerlukan izin alarm tepat waktu.","Prayer notifications and adhan alarms are separate. Notifications do not require alarm permission. Optional adhan audio follows the phone alarm volume and requires exact-alarm permission.")})},confirmButton={TextButton(onClick={
        when{
            PrayerPreferenceStore(context).read().location==null->message=label(language,"Pilih lokasi terlebih dahulu di Beranda atau Ibadah.","Choose a location under Home or Prayer first.")
            Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED->permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            alarm&&!AdhanScheduler.exactAllowed(context)->{message=label(language,"Izinkan alarm tepat waktu, lalu kembali dan tekan Aktifkan.","Allow exact alarms, return, then tap Enable.");runCatching{context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,android.net.Uri.parse("package:${context.packageName}")))}}
            !NotificationManagerCompat.from(context).areNotificationsEnabled()->{message=label(language,"Aktifkan notifikasi Nuraya di pengaturan HP.","Enable Nuraya notifications in device settings.");runCatching{context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName))}}
            else->onGranted()
        }
    }){Text(label(language,"Aktifkan","Enable"))}},dismissButton={TextButton(onClick=onDismiss){Text(label(language,"Batal","Cancel"))}})
}
