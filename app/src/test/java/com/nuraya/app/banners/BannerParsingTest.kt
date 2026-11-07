package com.nuraya.app.banners
import android.app.Application
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=Application::class)
class BannerParsingTest {
    @Test fun feedAcceptsOnlyBoundedUniqueBannersAndSafeDestinations(){
        val feed="""{"banners":[{"id":"b1","title":"Belajar","subtitle":"Hari ini","destination":"quran"}]}"""
        assertEquals("quran",parseBanners(feed).single().destination)
        listOf("javascript:alert(1)","file:///etc/passwd","http://example.com","https://user:password@example.com","https://localhost").forEach{assertFalse(it,safeBannerUrl(it))}
        for(invalid in listOf(feed.replace("quran","javascript:alert(1)"),feed.replace("Belajar","x".repeat(101)))){
            try{parseBanners(invalid);fail("invalid feed accepted")}catch(_:IllegalArgumentException){}
        }
    }
}
