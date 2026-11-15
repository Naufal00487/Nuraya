package com.nuraya.app.reminders

import android.app.*
import android.content.*
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.data.QuranRepository
import com.nuraya.app.prayer.*
import com.nuraya.app.profile.ProfileStore
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.*
import java.time.chrono.HijrahDate
import java.time.temporal.TemporalAdjusters
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=Application::class)
class ReminderTest {
    private val context get()=ApplicationProvider.getApplicationContext<Application>()
    private val alarms get()=Shadows.shadowOf(context.getSystemService(AlarmManager::class.java))
    private val notifications get()=Shadows.shadowOf(context.getSystemService(NotificationManager::class.java))
    private val prefs get()=QuranReminderScheduler.preferences(context)
    private val zone=ZoneId.of("Asia/Jakarta")
    private val day=LocalDate.of(2026,11,15)
    private fun at(hour:Int,minute:Int=0)=day.atTime(hour,minute).atZone(zone).toInstant()
    private fun planned() { prefs.edit().putString("plan:$day","""{"m0":540,"m1":960,"id0":1,"id1":2}""").commit() }
    private fun pendingIntent()=Shadows.shadowOf(alarms.scheduledAlarms.single().operation).savedIntent
    @Before fun clear() {
        listOf("nuraya_quran_reminders","nuraya_adhan","nuraya_permission_prompts","nuraya_profile").forEach{context.getSharedPreferences(it,0).edit().clear().commit()}
        PrayerPreferenceStore(context).preferences.edit().clear().commit()
        notifications.setNotificationsEnabled(true)
        ReminderNavigation.requests.value?.let{ReminderNavigation.consume(it)}
    }
    @Test fun allOneHundredSuppliedReferencesExistInTheCanonicalCorpus() {
        val rows=ReminderContent(context).quran;val quran=QuranRepository(context)
        assertEquals((1..100).toSet(),rows.map{it.id}.toSet())
        rows.forEach{row->for(ayah in row.firstAyah..row.lastAyah)assertNotNull(row.reference,quran.verse(row.surah,ayah))}
        assertTrue(rows.any{it.lastAyah>it.firstAyah})
    }
    @Test fun templatesFollowRamadanFridayPrayerAndGeneralPriorities() {
        val json=JSONObject(context.assets.open("notifications/prayer.json").bufferedReader().use{it.readText()})
        val content=ReminderContent(context)
        val ramadanFriday=LocalDate.from(HijrahDate.of(1448,9,1)).with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY))
        fun check(id:String,date:LocalDate,array:org.json.JSONArray) {
            val expected=(0 until array.length()).map{array.getJSONObject(it)}.map{ReminderText(it.getString("title").replace("{minutes}","15").replace("{salat}","Pilihan"),it.getString("body").replace("{minutes}","15").replace("{salat}","Pilihan"))}.toSet()
            val actual=(0..80).map{content.beforePrayer(id,"Pilihan",15,date,random=Random(it))}.toSet()
            assertEquals(expected,actual)
            actual.forEach{assertFalse(it.toString().contains("{"))}
        }
        check("maghrib",ramadanFriday,json.getJSONObject("ramadan").getJSONArray("maghrib"))
        check("fajr",ramadanFriday,json.getJSONObject("ramadan").getJSONArray("subuh"))
        check("dhuhr",ramadanFriday,json.getJSONArray("jumat"))
        check("asr",ramadanFriday,json.getJSONObject("per_salat").getJSONArray("ashar"))
        check("dhuhr",day,json.getJSONObject("per_salat").getJSONArray("dzuhur"))
        check("unknown",day,json.getJSONArray("umum"))
    }
    @Test fun randomPlansStayInDaytimeHaveDifferentMessagesAndThreeHourGap() {
        val times=mutableSetOf<Int>();val ids=mutableSetOf<Int>()
        repeat(1000){seed->
            val plan=dailyQuranPlan(day.plusDays(seed.toLong()),(1..100).toList(),Random(seed))
            assertEquals(2,plan.size);assertEquals(2,plan.map{it.messageId}.distinct().size)
            assertTrue(plan.all{it.minute in 480..1199});assertTrue(plan[1].minute-plan[0].minute>=180)
            times.addAll(plan.map{it.minute});ids.addAll(plan.map{it.messageId})
        }
        assertTrue(times.size>100);assertEquals(100,ids.size)
    }
    @Test fun reopenPreservesDailyPlanAndOnlyOneNextAlarm() {
        planned();val first=QuranReminderScheduler.plan(context,day)
        repeat(3){QuranReminderScheduler.schedule(context,at(7),zone)}
        assertEquals(first,QuranReminderScheduler.plan(context,day));assertEquals(at(9).toEpochMilli(),alarms.scheduledAlarms.single().triggerAtTime)
        assertEquals("$day:0:1",prefs.getString("scheduled",null))
    }
    @Test fun daylightSavingChangesDoNotShiftTheLocalReminderHour() {
        val timezone=ZoneId.of("America/New_York")
        for(date in listOf(LocalDate.of(2026,3,8),LocalDate.of(2026,11,1))) {
            val slot=QuranReminderSlot(date,1,1199,1)
            assertEquals(date.atTime(19,59),slot.instant(timezone).atZone(timezone).toLocalDateTime())
        }
    }
    @Test fun successfulDeliveryOpensCorrectVerseAndReplayDoesNotDuplicate() {
        planned();QuranReminderScheduler.schedule(context,at(7),zone);val intent=pendingIntent()
        val receiver=QuranReminderReceiver();receiver.deliver(context,intent,at(9),zone)
        assertEquals(1,notifications.size());assertEquals(at(9).toEpochMilli(),prefs.getLong("sent:$day:0",0))
        val notification=notifications.allNotifications.single()
        ReminderNavigation.accept(Shadows.shadowOf(notification.contentIntent).savedIntent)
        val message=ReminderContent(context).quran.first{it.id==1}
        assertEquals(message.surah,ReminderNavigation.requests.value!!.surah);assertEquals(message.firstAyah,ReminderNavigation.requests.value!!.ayah)
        receiver.deliver(context,intent,at(9),zone);assertEquals(1,notifications.size())
        assertEquals(at(16).toEpochMilli(),alarms.scheduledAlarms.single().triggerAtTime)
        receiver.deliver(context,pendingIntent(),at(16),zone);assertEquals(2,notifications.size())
        assertTrue(alarms.scheduledAlarms.single().triggerAtTime>at(20).toEpochMilli())
    }
    @Test fun androidDelayPreservesActualThreeHourGapAndQuietHours() {
        planned();QuranReminderScheduler.schedule(context,at(7),zone)
        QuranReminderReceiver().deliver(context,pendingIntent(),at(15),zone)
        assertEquals(at(18).toEpochMilli(),alarms.scheduledAlarms.single().triggerAtTime)
        QuranReminderReceiver().deliver(context,pendingIntent(),at(20),zone)
        assertEquals(1,notifications.size());assertFalse(prefs.contains("sent:$day:1"))
        assertTrue(alarms.scheduledAlarms.single().triggerAtTime>at(20).toEpochMilli())
    }
    @Test fun previousDayOrChangedTimeZoneCannotDeliverStaleContent() {
        planned();QuranReminderScheduler.schedule(context,at(7),zone);val old=pendingIntent()
        QuranReminderReceiver().deliver(context,old,at(9),ZoneId.of("Asia/Makassar"))
        assertEquals(0,notifications.size())
        QuranReminderReceiver().deliver(context,old,at(9).plusSeconds(86400),zone)
        assertEquals(0,notifications.size())
    }
    @Test fun clockRollbackCannotRepeatAnAlreadyDeliveredSlot() {
        planned();QuranReminderScheduler.schedule(context,at(7),zone)
        QuranReminderReceiver().deliver(context,pendingIntent(),at(9),zone)
        QuranReminderScheduler.schedule(context,at(7),zone)
        assertEquals(at(16).toEpochMilli(),alarms.scheduledAlarms.single().triggerAtTime)
        assertEquals("$day:1:2",prefs.getString("scheduled",null))
    }
    @Test fun disablingOrRevokingNotificationsCancelsQueueAndRetainsTheDayPlan() {
        planned();QuranReminderScheduler.schedule(context,at(7),zone)
        QuranReminderScheduler.setEnabled(context,false);assertTrue(alarms.scheduledAlarms.isEmpty())
        assertNotNull(prefs.getString("plan:$day",null))
        prefs.edit().putBoolean("enabled",true).commit();notifications.setNotificationsEnabled(false)
        QuranReminderScheduler.schedule(context,at(7),zone);assertTrue(alarms.scheduledAlarms.isEmpty())
    }
    @Test @Config(sdk=[33]) fun ordinaryNotificationsWorkWithoutExactAlarmPermissionAndAudioStaysOff() {
        org.robolectric.shadows.ShadowAlarmManager.setCanScheduleExactAlarms(false)
        assertFalse(AdhanScheduler.exactAllowed(context))
        PrayerPreferenceStore(context).save(PrayerPreferences(PrayerLocation("Jakarta",-6.2,106.8,"Asia/Jakarta")))
        AdhanScheduler.reconcile(context);assertFalse(AdhanScheduler.enabled(context))
        assertEquals(1,alarms.scheduledAlarms.size)
        QuranReminderScheduler.schedule(context,at(7),zone);assertEquals(2,alarms.scheduledAlarms.size)
        assertTrue(alarms.scheduledAlarms.all{it.windowLengthMs!=0L})
    }
    @Test fun prePrayerLeadMinutesChangeTheActualScheduleAndNeverAffectAudioDefault() {
        val location=PrayerLocation("Jakarta",-6.2,106.8,"Asia/Jakarta")
        val moment=PrayerCalculator.calculate(day,location,0).moments.first{it.id=="fajr"}
        listOf(5,10,15,30).forEach{minutes->
            val reminder=nextPrayerReminder(moment.instant.minusSeconds(3600),location,0,setOf("fajr"),minutes)!!
            assertEquals(moment.instant.minusSeconds(minutes*60L),reminder.at)
            AdhanScheduler.setBeforeMinutes(context,minutes);assertEquals(minutes,AdhanScheduler.beforeMinutes(context));assertFalse(AdhanScheduler.enabled(context))
        }
    }
    @Test fun notificationPromptIsWeeklyAndClockRollbackDoesNotRetriggerIt() {
        val store=PermissionPromptStore(context);val now=1_800_000_000_000L
        assertTrue(store.notificationDue(now));store.markNotificationsShown(now)
        assertFalse(store.notificationDue(now));assertFalse(store.notificationDue(now-86_400_000))
        assertFalse(store.notificationDue(now+7*86_400_000L-1));assertTrue(store.notificationDue(now+7*86_400_000L))
    }
    @Test fun oldAvatarSelectionMigratesWithoutResettingTheName() {
        val store=ProfileStore(context);store.preferences.edit().putString("nickname","Naufal").putInt("avatar",4).commit()
        assertTrue(store.isComplete());assertEquals("Naufal",store.read().nickname);assertEquals(2,store.read().avatar)
        assertEquals(3,context.assets.list("brand/avatars")!!.size)
    }
    @Test fun notificationIntentsAreDistinctAndInvalidReferencesAreIgnored() {
        val one=ReminderNavigation.pending(context,"one",112,1);val two=ReminderNavigation.pending(context,"two",55,13)
        assertNotEquals(one,two)
        ReminderNavigation.accept(Shadows.shadowOf(two).savedIntent);val first=ReminderNavigation.requests.value!!
        ReminderNavigation.accept(Shadows.shadowOf(one).savedIntent);val second=ReminderNavigation.requests.value!!
        ReminderNavigation.consume(first);assertEquals(second,ReminderNavigation.requests.value)
        ReminderNavigation.consume(second);assertNull(ReminderNavigation.requests.value)
        ReminderNavigation.accept(Shadows.shadowOf(one).savedIntent.putExtra("surah",115));assertNull(ReminderNavigation.requests.value)
        ReminderNavigation.accept(Shadows.shadowOf(ReminderNavigation.pending(context,"prayer")).savedIntent)
        assertNotNull(ReminderNavigation.requests.value);assertNull(ReminderNavigation.requests.value!!.surah)
    }
}
