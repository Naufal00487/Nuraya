package com.nuraya.app.media

import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadSafetyTest {
    @Test fun acceptsHttpsRecitationSources() {
        assertTrue(DownloadSafety.safeSource(URI("https://server8.mp3quran.net/afs/001.mp3")))
    }

    @Test fun refusesInsecureCredentialedOrUnexpectedPortSources() {
        listOf("http://audio.example/001.mp3", "https://user:password@audio.example/001.mp3", "https://audio.example:8080/001.mp3", "file:///tmp/audio.mp3").forEach {
            assertFalse(it, DownloadSafety.safeSource(URI(it)))
        }
    }

    @Test fun redirectsRemainOnTheAuthenticatedSourceHost() {
        val source = URI("https://audio.example/a.mp3")
        assertTrue(DownloadSafety.allowedRedirect(source, URI("https://audio.example/new/a.mp3")))
        assertFalse(DownloadSafety.allowedRedirect(source, URI("https://different.example/a.mp3")))
        assertFalse(DownloadSafety.allowedRedirect(source, URI("http://audio.example/a.mp3")))
    }

    @Test fun qariIdentifiersCannotEscapeAppStorage() {
        assertTrue(DownloadSafety.validQariId("ar.alafasy"))
        listOf("../data", "..", "/afs", "", "a/b", "a:b").forEach { assertFalse(it, DownloadSafety.validQariId(it)) }
    }

    @Test fun incompleteDownloadsNeverReportComplete() {
        assertEquals(0, DownloadSafety.progress(200, -1))
        assertEquals(50, DownloadSafety.progress(500, 1000))
        assertEquals(99, DownloadSafety.progress(1000, 1000))
        assertEquals(99, DownloadSafety.progress(1100, 1000))
    }
}
