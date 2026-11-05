package com.nuraya.app.prayer

import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.HighLatitudeRule
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.Qibla
import com.batoulapps.adhan.data.DateComponents
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Coordinates belong to an explicitly selected place, never a guessed device location. */
data class PrayerLocation(val name: String, val latitude: Double, val longitude: Double, val zoneId: String) {
    init {
        require(name.isNotBlank() && name.length <= 80)
        require(latitude.isFinite() && latitude in -89.0..89.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
        ZoneId.of(zoneId)
    }
    val zone: ZoneId get() = ZoneId.of(zoneId)
}

data class PrayerPreferences(
    val location: PrayerLocation? = null,
    val hijriAdjustment: Int = 0,
    val minuteAdjustment: Int = 2,
) {
    init { require(hijriAdjustment in -2..2); require(minuteAdjustment in -30..30) }
}

data class PrayerMoment(val id: String, val nameId: String, val nameEn: String, val instant: Instant, val isPrayer: Boolean = true) {
    fun name(language: String) = if (language == "en") nameEn else nameId
}

data class PrayerDay(val date: LocalDate, val moments: List<PrayerMoment>, val highLatitude: Boolean) {
    val available: Boolean get() = moments.size == 6
}

object PrayerCalculator {
    /** Astronomical calculation, not an official downloaded ministry timetable. */
    fun calculate(date: LocalDate, location: PrayerLocation, minuteAdjustment: Int = 2): PrayerDay {
        require(minuteAdjustment in -30..30)
        val parameters = CalculationParameters(20.0, 18.0).apply {
            madhab = Madhab.SHAFI
            highLatitudeRule = HighLatitudeRule.TWILIGHT_ANGLE
            adjustments.fajr = minuteAdjustment
            adjustments.dhuhr = minuteAdjustment
            adjustments.asr = minuteAdjustment
            adjustments.maghrib = minuteAdjustment
            adjustments.isha = minuteAdjustment
        }
        val times = PrayerTimes(Coordinates(location.latitude, location.longitude), DateComponents(date.year, date.monthValue, date.dayOfMonth), parameters)
        val dates = listOf(times.fajr, times.sunrise, times.dhuhr, times.asr, times.maghrib, times.isha)
        if (dates.any { it == null }) return PrayerDay(date, emptyList(), kotlin.math.abs(location.latitude) > 48.0)
        val names = listOf(Triple("fajr", "Subuh", "Fajr"), Triple("sunrise", "Terbit", "Sunrise"),
            Triple("dhuhr", "Zuhur", "Dhuhr"), Triple("asr", "Asar", "Asr"),
            Triple("maghrib", "Magrib", "Maghrib"), Triple("isha", "Isya", "Isha"))
        return PrayerDay(date, dates.mapIndexed { index, time ->
            val name = names[index]
            // Adhan rounds to a minute but its Calendar can retain construction-time
            // milliseconds. Remove that unrelated fraction so countdown transitions
            // and the displayed minute agree exactly.
            PrayerMoment(name.first, name.second, name.third,
                requireNotNull(time).toInstant().truncatedTo(ChronoUnit.MINUTES), index != 1)
        }, kotlin.math.abs(location.latitude) > 48.0)
    }

    /** Sunrise is shown in the schedule but is not a sixth obligatory prayer. */
    fun next(now: Instant, location: PrayerLocation, minuteAdjustment: Int = 2): PrayerMoment? {
        val date = now.atZone(location.zone).toLocalDate()
        return listOf(date, date.plusDays(1)).flatMap { calculate(it, location, minuteAdjustment).moments }
            .filter { it.isPrayer && it.instant.isAfter(now) }.minByOrNull { it.instant }
    }

    fun qiblaBearing(location: PrayerLocation): Double =
        Qibla(Coordinates(location.latitude, location.longitude)).direction

    fun countdown(now: Instant, target: Instant): String {
        val seconds = Duration.between(now, target).seconds.coerceAtLeast(0)
        return String.format(Locale.ROOT, "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
    }

    fun signedTurn(targetBearing: Double, trueHeading: Double): Double {
        require(targetBearing.isFinite() && trueHeading.isFinite())
        return ((targetBearing - trueHeading) % 360.0 + 540.0) % 360.0 - 180.0
    }
}

object HijriCalendar {
    private val monthId = listOf("Muharam", "Safar", "Rabiulawal", "Rabiulakhir", "Jumadilawal", "Jumadilakhir", "Rajab", "Syakban", "Ramadan", "Syawal", "Zulkaidah", "Zulhijah")
    private val monthEn = listOf("Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani", "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Shaban", "Ramadan", "Shawwal", "Dhu al-Qadah", "Dhu al-Hijjah")
    fun label(date: LocalDate, adjustment: Int = 0, language: String = "id"): String {
        require(adjustment in -2..2)
        val hijri = HijrahDate.from(date.plusDays(adjustment.toLong()))
        val months = if (language == "en") monthEn else monthId
        return "${hijri.get(ChronoField.DAY_OF_MONTH)} ${months[hijri.get(ChronoField.MONTH_OF_YEAR) - 1]} ${hijri.get(ChronoField.YEAR_OF_ERA)} H"
    }
}

object PrayerCities {
    val all = listOf(
        PrayerLocation("Banda Aceh", 5.5483, 95.3238, "Asia/Jakarta"),
        PrayerLocation("Medan", 3.5952, 98.6722, "Asia/Jakarta"),
        PrayerLocation("Padang", -0.9471, 100.4172, "Asia/Jakarta"),
        PrayerLocation("Palembang", -2.9761, 104.7754, "Asia/Jakarta"),
        PrayerLocation("Jakarta", -6.2088, 106.8456, "Asia/Jakarta"),
        PrayerLocation("Bandung", -6.9175, 107.6191, "Asia/Jakarta"),
        PrayerLocation("Semarang", -6.9667, 110.4167, "Asia/Jakarta"),
        PrayerLocation("Yogyakarta", -7.7956, 110.3695, "Asia/Jakarta"),
        PrayerLocation("Surabaya", -7.2575, 112.7521, "Asia/Jakarta"),
        PrayerLocation("Pontianak", -0.0263, 109.3425, "Asia/Pontianak"),
        PrayerLocation("Denpasar", -8.6500, 115.2167, "Asia/Makassar"),
        PrayerLocation("Banjarmasin", -3.3186, 114.5944, "Asia/Makassar"),
        PrayerLocation("Makassar", -5.1477, 119.4327, "Asia/Makassar"),
        PrayerLocation("Manado", 1.4748, 124.8421, "Asia/Makassar"),
        PrayerLocation("Jayapura", -2.5337, 140.7181, "Asia/Jayapura"),
    )
}
