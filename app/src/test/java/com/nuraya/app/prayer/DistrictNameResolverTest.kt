package com.nuraya.app.prayer
import android.app.Application
import android.location.Address
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.util.Locale
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=Application::class)
class DistrictNameResolverTest {
    @Test fun explicitDistrictWinsOverVillageOrRegency(){
        val address=Address(Locale.forLanguageTag("id-ID")).apply{
            locality="Tangerang";subLocality="Bojong Nangka";subAdminArea="Kabupaten Tangerang"
            setAddressLine(0,"Jalan Raya, Kec. Kelapa Dua, Kabupaten Tangerang")
        }
        assertEquals("Kelapa Dua",districtLabel(address))
        address.locality="Kecamatan Curug";assertEquals("Curug",districtLabel(address))
    }
    @Test fun emptyAddressNeverFabricatesAnAreaName(){
        assertNull(districtLabel(Address(Locale.ROOT)))
        val address=Address(Locale.ROOT).apply{locality="Kelapa Dua";subLocality="Pakulonan Barat"}
        assertEquals("Kelapa Dua",districtLabel(address))
    }
    @Test fun missingGeocoderKeepsCoordinatesAndProvidesNoInventedName()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Application>()
        org.robolectric.shadows.ShadowGeocoder.setIsPresent(false)
        val location=PrayerLocation("Lokasi tersimpan",-6.23,106.62,"Asia/Jakarta")
        assertNull(DistrictNameResolver(context).resolve(location))
        assertEquals(-6.23,location.latitude,0.0)
    }
}
