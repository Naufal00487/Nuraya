package com.nuraya.app.prayer

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class PrayerCalculatorTest {
    private val jakarta = PrayerCities.all.single { it.name == "Jakarta" }

    @Test fun jakartaScheduleUsesLocalCivilDateAndTheSelectedAngles() {
        val date = LocalDate.of(2026, 10, 28)
        val result = PrayerCalculator.calculate(date, jakarta)
        assertTrue(result.available)
        assertFalse(result.highLatitude)
        // Reference Adhan 1.2.1: 20°/18°, Shafi, +2 minutes except sunrise.
        assertEquals(listOf("04:10", "05:27", "11:38", "14:52", "17:48", "18:59"),
            result.moments.map { it.instant.atZone(jakarta.zone).toLocalTime().toString() })
        assertTrue(result.moments.all { it.instant.atZone(jakarta.zone).toLocalDate() == date })
        assertTrue(result.moments.zipWithNext().all { (a, b) -> a.instant < b.instant })
        assertTrue(result.moments.all { it.instant.nano == 0 && it.instant.epochSecond % 60 == 0L })
    }

    @Test fun timeCorrectionChangesPrayersButNeverSunriseOrDeviceClock() {
        val date = LocalDate.of(2026, 10, 28)
        val base = PrayerCalculator.calculate(date, jakarta, 0)
        val corrected = PrayerCalculator.calculate(date, jakarta, 7)
        base.moments.zip(corrected.moments).forEach { (before, after) ->
            assertEquals(before.instant.plusSeconds(if (before.isPrayer) 420 else 0), after.instant)
        }
    }

    @Test fun afterIshaTheNextPrayerIsTomorrowsFajr() {
        val date = LocalDate.of(2026, 10, 28)
        val tonight = PrayerCalculator.calculate(date, jakarta).moments.last().instant.plusSeconds(1)
        val next = requireNotNull(PrayerCalculator.next(tonight, jakarta))
        assertEquals("fajr", next.id)
        assertEquals(date.plusDays(1), next.instant.atZone(jakarta.zone).toLocalDate())
        assertEquals(PrayerCalculator.calculate(date.plusDays(1), jakarta).moments.first(), next)
    }

    @Test fun sunriseIsDisplayedButNotSelectedAsTheNextPrayer() {
        val day = PrayerCalculator.calculate(LocalDate.of(2026, 10, 28), jakarta)
        assertEquals("sunrise", day.moments.single { !it.isPrayer }.id)
        assertEquals("dhuhr", PrayerCalculator.next(day.moments.first().instant, jakarta)?.id)
        assertEquals("fajr", PrayerCalculator.next(day.moments.first().instant.minusSeconds(1), jakarta)?.id)
    }

    @Test fun countdownClampsExpiredTimesAndSupportsAnOvernightInterval() {
        val now = Instant.parse("2026-10-28T16:00:00Z")
        assertEquals("04:03:02", PrayerCalculator.countdown(now, now.plusSeconds(4 * 3600 + 3 * 60 + 2)))
        assertEquals("00:00:00", PrayerCalculator.countdown(now, now))
        assertEquals("00:00:00", PrayerCalculator.countdown(now, now.minusSeconds(1)))
    }

    @Test fun polarSummerDoesNotInventSunriseOrPrayerTimes() {
        val polar = PrayerLocation("Longyearbyen", 78.2232, 15.6469, "Arctic/Longyearbyen")
        val result = PrayerCalculator.calculate(LocalDate.of(2026, 6, 21), polar)
        assertTrue(result.highLatitude)
        assertFalse(result.available)
        assertTrue(result.moments.isEmpty())
        assertNull(PrayerCalculator.next(Instant.parse("2026-06-21T10:00:00Z"), polar))
    }

    @Test fun qiblaUsesTheGreatCircleTrueNorthBearingAndShortestTurn() {
        assertEquals(295.15, PrayerCalculator.qiblaBearing(jakarta), 0.05)
        assertEquals(10.0, PrayerCalculator.signedTurn(5.0, 355.0), 0.0001)
        assertEquals(-20.0, PrayerCalculator.signedTurn(350.0, 10.0), 0.0001)
        assertEquals(-180.0, PrayerCalculator.signedTurn(0.0, 180.0), 0.0001)
        assertEquals(0.0, PrayerCalculator.signedTurn(730.0, -350.0), 0.0001)
    }

    @Test fun hijriAdjustmentCrossesTheRamadanBoundaryWithoutChangingCivilDate() {
        val date = LocalDate.of(2024, 4, 10)
        assertEquals("1 Syawal 1445 H", HijriCalendar.label(date))
        assertEquals("30 Ramadan 1445 H", HijriCalendar.label(date, -1))
        assertEquals("29 Ramadan 1445 H", HijriCalendar.label(date, -2))
        assertEquals("3 Syawal 1445 H", HijriCalendar.label(date, 2))
        assertEquals("1 Shawwal 1445 H", HijriCalendar.label(date, language = "en"))
        assertEquals(LocalDate.of(2024, 4, 10), date)
    }

    @Test fun invalidLocationAndAdjustmentInputsAreRejected() {
        listOf<() -> Unit>(
            { PrayerLocation("", 0.0, 0.0, "UTC") },
            { PrayerLocation("x", Double.NaN, 0.0, "UTC") },
            { PrayerLocation("x", 90.0, 0.0, "UTC") },
            { PrayerLocation("x", 0.0, 181.0, "UTC") },
            { PrayerLocation("x", 0.0, 0.0, "Not/AZone") },
            { PrayerPreferences(hijriAdjustment = 3) },
            { PrayerPreferences(minuteAdjustment = 31) },
            { PrayerCalculator.calculate(LocalDate.of(2026, 10, 28), jakarta, -31) },
        ).forEach { action ->
            assertTrue(runCatching(action).isFailure)
        }
        assertNull(PrayerPreferences().location)
        assertEquals(3, PrayerCities.all.map { it.zone.rules.getOffset(Instant.parse("2026-10-28T00:00:00Z")) }.toSet().size)
    }
}
