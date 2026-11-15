package com.nuraya.app.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nuraya.app.R
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

data class QuranReminderSlot(val date: LocalDate, val slot: Int, val minute: Int, val messageId: Int) {
    val key get() = "$date:$slot:$messageId"
    fun instant(zone: ZoneId): Instant = date.atTime(minute / 60, minute % 60).atZone(zone).toInstant()
}
internal fun dailyQuranPlan(date: LocalDate, ids: List<Int>, random: Random = Random.Default): List<QuranReminderSlot> {
    require(ids.distinct().size >= 2)
    val chosen = ids.distinct().shuffled(random).take(2)
    return listOf(QuranReminderSlot(date, 0, random.nextInt(480, 721), chosen[0]),
        QuranReminderSlot(date, 1, random.nextInt(900, 1200), chosen[1]))
}
object QuranReminderScheduler {
    const val ACTION = "com.nuraya.app.QURAN_REMINDER"
    const val CHANNEL = "quran_daily"
    private const val REQUEST = 4401
    fun preferences(context: Context) = context.applicationContext.getSharedPreferences("nuraya_quran_reminders", Context.MODE_PRIVATE)
    fun enabled(context: Context) = preferences(context).getBoolean("enabled", true)
    fun setEnabled(context: Context, enabled: Boolean) { preferences(context).edit().putBoolean("enabled", enabled).apply(); schedule(context) }
    internal fun plan(context: Context, date: LocalDate): List<QuranReminderSlot> {
        val prefs = preferences(context); val key = "plan:$date"
        val ids = ReminderContent(context).quran.map { it.id }
        val saved = runCatching {
            val json = JSONObject(prefs.getString(key, null) ?: return@runCatching null)
            List(2) { index -> QuranReminderSlot(date, index, json.getInt("m$index"), json.getInt("id$index")) }.takeIf {
                it[0].minute in 480..720 && it[1].minute in 900..1199 && it.all { row -> row.messageId in ids } && it[0].messageId != it[1].messageId
            }
        }.getOrNull()
        if (saved != null) return saved
        val result = dailyQuranPlan(date, ids)
        val json = JSONObject().apply { result.forEach { put("m${it.slot}", it.minute); put("id${it.slot}", it.messageId) } }
        prefs.edit().putString(key, json.toString()).commit()
        return result
    }
    internal fun candidateTime(context: Context, slot: QuranReminderSlot, zone: ZoneId): Instant? {
        val prefs = preferences(context)
        if (prefs.contains("sent:${slot.date}:${slot.slot}")) return null
        val other = prefs.getLong("sent:${slot.date}:${1-slot.slot}", 0)
        val at = maxOf(slot.instant(zone), if (other > 0) Instant.ofEpochMilli(other).plusSeconds(3*3600) else Instant.EPOCH)
        return at.takeIf { it.atZone(zone).toLocalDate() == slot.date && it.atZone(zone).hour < 20 }
    }
    @Synchronized fun schedule(context: Context, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, QuranReminderReceiver::class.java).setAction(ACTION)
        PendingIntent.getBroadcast(context, REQUEST, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let { alarms.cancel(it) }
        val prefs = preferences(context); prefs.edit().remove("scheduled").apply()
        if (!enabled(context) || !NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val today = now.atZone(zone).toLocalDate()
        // Keep a small delivery ledger across reboots, clock and time-zone changes.
        prefs.edit().apply {
            prefs.all.keys.filter { it.startsWith("plan:") || it.startsWith("sent:") }.forEach { key ->
                val date = runCatching { LocalDate.parse(key.substringAfter(':').take(10)) }.getOrNull()
                if (date != null && date < today.minusDays(14)) remove(key)
            }
        }.apply()
        val next = (0L..2L).flatMap { plan(context, today.plusDays(it)) }
            .mapNotNull { slot -> candidateTime(context, slot, zone)?.takeIf { it.isAfter(now) }?.let { slot to it } }
            .minByOrNull { it.second } ?: return
        intent.putExtra("key", next.first.key).putExtra("day", next.first.date.toString()).putExtra("slot", next.first.slot)
            .putExtra("message", next.first.messageId).putExtra("at", next.second.toEpochMilli()).putExtra("zone", zone.id)
        val pending = PendingIntent.getBroadcast(context, REQUEST, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        // These ordinary notifications do not require the exact-alarm permission.
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.second.toEpochMilli(), pending)
        prefs.edit().putString("scheduled", next.first.key).apply()
    }
}
class QuranReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        deliver(context, intent, Instant.now(), ZoneId.systemDefault())
    }
    internal fun deliver(context: Context, intent: Intent, now: Instant, zone: ZoneId) {
        val prefs = QuranReminderScheduler.preferences(context)
        val local = now.atZone(zone)
        val day = runCatching { LocalDate.parse(intent.getStringExtra("day")) }.getOrNull()
        val slot = intent.getIntExtra("slot", -1); val id = intent.getIntExtra("message", -1)
        val lastOther = if (day != null && slot in 0..1) prefs.getLong("sent:$day:${1-slot}", 0) else 0
        val valid = intent.action == QuranReminderScheduler.ACTION && QuranReminderScheduler.enabled(context) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled() && day == local.toLocalDate() &&
            local.hour in 8..19 && slot in 0..1 && intent.getStringExtra("zone") == zone.id &&
            now.toEpochMilli() >= intent.getLongExtra("at", Long.MAX_VALUE) &&
            intent.getStringExtra("key") == prefs.getString("scheduled", null) && intent.getStringExtra("key") != null &&
            !prefs.contains("sent:$day:$slot") && (lastOther == 0L || now.toEpochMilli()-lastOther >= 3*3600_000L)
        if (valid) {
            val message = ReminderContent(context).quran.firstOrNull { it.id == id }
            if (message != null) {
                prefs.edit().putLong("sent:$day:$slot", now.toEpochMilli()).commit()
                val manager = context.getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(NotificationChannel(QuranReminderScheduler.CHANNEL, "Pengingat Al-Qur’an", NotificationManager.IMPORTANCE_DEFAULT))
                val body = "${message.text}\n${message.reference}"
                val notification = NotificationCompat.Builder(context, QuranReminderScheduler.CHANNEL).setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("Pengingat Al-Qur’an").setContentText(message.text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(body).setSummaryText(message.reference))
                    .setContentIntent(ReminderNavigation.pending(context, "quran/${day}/$slot", message.surah, message.firstAyah))
                    .setAutoCancel(true).setCategory(NotificationCompat.CATEGORY_REMINDER).build()
                runCatching { manager.notify(4400+slot, notification) }
            }
        }
        QuranReminderScheduler.schedule(context, now, zone)
    }
}
