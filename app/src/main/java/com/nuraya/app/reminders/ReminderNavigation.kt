package com.nuraya.app.reminders

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.nuraya.app.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ReminderDestination(val sequence: Long, val surah: Int? = null, val ayah: Int? = null)
object ReminderNavigation {
    private const val ACTION = "com.nuraya.app.OPEN_REMINDER"
    private val mutable = MutableStateFlow<ReminderDestination?>(null)
    val requests = mutable.asStateFlow()
    private var sequence = 0L
    fun accept(intent: Intent?) {
        if (intent?.action != ACTION) return
        val surah = intent.getIntExtra("surah", 0)
        val ayah = intent.getIntExtra("ayah", 0)
        mutable.value = if (surah in 1..114 && ayah in 1..286) ReminderDestination(++sequence, surah, ayah)
            else if (surah == 0) ReminderDestination(++sequence) else null
    }
    fun consume(request: ReminderDestination) { if (mutable.value == request) mutable.value = null }
    fun pending(context: Context, key: String, surah: Int = 0, ayah: Int = 0): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).setAction(ACTION)
            .setData(Uri.parse("nuraya://reminder/$key"))
            .putExtra("surah", surah).putExtra("ayah", ayah)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
