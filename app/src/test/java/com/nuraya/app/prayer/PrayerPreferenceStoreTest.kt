package com.nuraya.app.prayer

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class PrayerPreferenceStoreTest {
    private val context get() = ApplicationProvider.getApplicationContext<Application>()
    @Before fun clearSettings() { PrayerPreferenceStore(context).preferences.edit().clear().commit() }

    @Test fun noLocationIsAssumedAndExplicitChoiceSurvivesReopening() {
        val store = PrayerPreferenceStore(context)
        assertEquals(PrayerPreferences(), store.read())
        val selected = PrayerPreferences(PrayerCities.all.single { it.name == "Jayapura" }, -1, 3)
        store.save(selected)
        assertEquals(selected, PrayerPreferenceStore(context).read())
        store.save(selected.copy(location = null))
        assertNull(PrayerPreferenceStore(context).read().location)
        assertFalse(store.preferences.contains("latitude"))
        assertFalse(store.preferences.contains("longitude"))
    }

    @Test fun malformedStoredLocationIsNotSilentlyReplacedWithJakarta() {
        val store = PrayerPreferenceStore(context)
        store.preferences.edit().putBoolean("location_selected", true).putString("name", "Corrupt")
            .putString("latitude", "NaN").putString("longitude", "106.0").putString("zone", "Asia/Jakarta")
            .putInt("hijri_adjustment", 99).putString("minute_adjustment", "corrupt").commit()
        val result = store.read()
        assertNull(result.location)
        assertEquals(2, result.hijriAdjustment)
        assertEquals(2, result.minuteAdjustment)
    }
}
