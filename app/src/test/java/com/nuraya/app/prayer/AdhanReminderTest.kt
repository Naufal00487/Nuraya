package com.nuraya.app.prayer
import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.Instant
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=Application::class)
class AdhanReminderTest {
    private val context get()=ApplicationProvider.getApplicationContext<Application>()
    private val alarms get()=Shadows.shadowOf(context.getSystemService(AlarmManager::class.java))
    @Test fun remindersAreOffByDefaultAndOnlyNextPrayerIsScheduledAfterOptIn(){
        context.getSharedPreferences("nuraya_adhan",Context.MODE_PRIVATE).edit().clear().commit()
        val location=PrayerLocation("Jakarta",-6.2,106.8,"Asia/Jakarta")
        PrayerPreferenceStore(context).save(PrayerPreferences(location))
        assertFalse(AdhanScheduler.enabled(context));assertTrue(alarms.scheduledAlarms.isEmpty())
        AdhanScheduler.setEnabled(context,true)
        val alarm=alarms.scheduledAlarms.single()
        val expected=PrayerCalculator.next(Instant.now(),location)!!
        assertTrue(expected.isPrayer);assertNotEquals("sunrise",expected.id)
        assertEquals(expected.instant.toEpochMilli(),alarm.triggerAtTime)
        AdhanScheduler.schedule(context);assertEquals(1,alarms.scheduledAlarms.size)
        AdhanScheduler.setEnabled(context,false);assertTrue(alarms.scheduledAlarms.isEmpty())
    }
    @Test fun missingLocationAndStaleAlarmNeverStartAdhanService(){
        AdhanScheduler.setEnabled(context,true)
        assertTrue(alarms.scheduledAlarms.isEmpty())
        AdhanReceiver().onReceive(context,Intent(AdhanScheduler.ACTION).putExtra("at",0L))
        assertNull(Shadows.shadowOf(context).nextStartedService)
    }
    @Test fun revokingNotificationsDisablesAndCancelsPreviouslyEnabledReminders(){
        PrayerPreferenceStore(context).save(PrayerPreferences(PrayerLocation("Jakarta",-6.2,106.8,"Asia/Jakarta")))
        AdhanScheduler.setEnabled(context,true)
        assertEquals(1,alarms.scheduledAlarms.size)
        Shadows.shadowOf(context.getSystemService(android.app.NotificationManager::class.java)).setNotificationsEnabled(false)
        AdhanScheduler.reconcile(context)
        assertFalse(AdhanScheduler.enabled(context))
        assertTrue(alarms.scheduledAlarms.isEmpty())
    }
}
