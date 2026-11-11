package com.nuraya.app.prayer
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=Application::class)
class AdhanReminderTest {
    private val context get()=ApplicationProvider.getApplicationContext<Application>()
    private val alarms get()=Shadows.shadowOf(context.getSystemService(AlarmManager::class.java))
    private val location=PrayerLocation("Kelapa Dua",-6.23,106.62,"Asia/Jakarta")
    @Before fun clear(){
        AdhanScheduler.preferences(context).edit().clear().commit()
        PrayerPreferenceStore(context).preferences.edit().clear().commit()
    }
    @Test fun eachSelectedPrayerHasTenMinuteAndAtTimeEventsAndNeverSunrise(){
        val day=PrayerCalculator.calculate(LocalDate.of(2026,11,7),location)
        for(prayer in day.moments.filter{it.isPrayer}){
            val selected=setOf(prayer.id)
            val before=nextPrayerReminder(prayer.instant.minusSeconds(601),location,2,selected)!!
            assertTrue(before.before);assertEquals(prayer.instant.minusSeconds(600),before.at)
            val at=nextPrayerReminder(before.at,location,2,selected)!!
            assertFalse(at.before);assertEquals(prayer.instant,at.at)
            val next=nextPrayerReminder(at.at,location,2,selected)!!
            assertTrue(next.before);assertEquals(prayer.id,next.prayer.id)
            assertTrue(next.at.isAfter(prayer.instant.plusSeconds(20*3600)))
        }
        assertNull(nextPrayerReminder(Instant.now(),location,0,emptySet()))
        assertNull(nextPrayerReminder(Instant.now(),location,0,setOf("sunrise")))
    }
    @Test fun soundAndPerPrayerSelectionsAreIndependentAndOnlyOneAlarmIsQueued(){
        PrayerPreferenceStore(context).save(PrayerPreferences(location))
        assertFalse(AdhanScheduler.enabled(context));assertTrue(AdhanScheduler.selected(context).isEmpty())
        AdhanScheduler.setPrayerEnabled(context,"fajr",true)
        assertFalse(AdhanScheduler.enabled(context));assertEquals(setOf("fajr"),AdhanScheduler.selected(context))
        assertEquals(nextPrayerReminder(Instant.now(),location,2,setOf("fajr"))!!.at.toEpochMilli(),alarms.scheduledAlarms.single().triggerAtTime)
        AdhanScheduler.setEnabled(context,true);AdhanScheduler.setEnabled(context,false)
        assertEquals(setOf("fajr"),AdhanScheduler.selected(context));assertEquals(1,alarms.scheduledAlarms.size)
        AdhanScheduler.schedule(context);assertEquals(1,alarms.scheduledAlarms.size)
        AdhanScheduler.setPrayerEnabled(context,"fajr",false);assertTrue(alarms.scheduledAlarms.isEmpty())
    }
    @Test fun aValidSilentReminderReplacesEarlierNotificationAndReplayIsIgnored(){
        PrayerPreferenceStore(context).save(PrayerPreferences(location))
        AdhanScheduler.setPrayerEnabled(context,"fajr",true)
        val now=System.currentTimeMillis()
        fun send(before:Boolean,key:String){
            AdhanScheduler.preferences(context).edit().putString("scheduled_key",key).commit()
            val intent=Intent(AdhanScheduler.ACTION).putExtra("at",now).putExtra("prayerAt",now+if(before)600000 else 0)
                .putExtra("id","fajr").putExtra("name","Subuh").putExtra("key",key).putExtra("before",before)
            AdhanReceiver().onReceive(context,intent)
            AdhanReceiver().onReceive(context,intent)
        }
        val notifications=Shadows.shadowOf(context.getSystemService(NotificationManager::class.java))
        send(true,"pre-test");assertEquals(1,notifications.size())
        assertEquals("Menjelang salat Subuh",notifications.allNotifications.single().extras.getCharSequence("android.title"))
        send(false,"at-test");assertEquals(1,notifications.size())
        assertEquals("Waktu salat Subuh",notifications.allNotifications.single().extras.getCharSequence("android.title"))
        assertNull(Shadows.shadowOf(context).nextStartedService)
    }
    @Test fun missingLocationAndStaleAlarmNeverStartAdhanService(){
        AdhanScheduler.setEnabled(context,true)
        assertTrue(alarms.scheduledAlarms.isEmpty())
        AdhanReceiver().onReceive(context,Intent(AdhanScheduler.ACTION).putExtra("at",0L))
        assertNull(Shadows.shadowOf(context).nextStartedService)
    }
    @Test fun revokingNotificationsDisablesAndCancelsPreviouslyEnabledReminders(){
        PrayerPreferenceStore(context).save(PrayerPreferences(location))
        AdhanScheduler.setEnabled(context,true)
        assertEquals(1,alarms.scheduledAlarms.size)
        Shadows.shadowOf(context.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(false)
        AdhanScheduler.reconcile(context)
        assertFalse(AdhanScheduler.enabled(context));assertTrue(AdhanScheduler.selected(context).isEmpty())
        assertTrue(alarms.scheduledAlarms.isEmpty())
    }
}
