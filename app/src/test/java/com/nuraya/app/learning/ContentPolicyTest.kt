package com.nuraya.app.learning

import org.junit.Assert.*
import org.junit.Test

class ContentPolicyTest {
    @Test fun urlsRequirePublicHttpsWithoutEmbeddedCredentials() {
        assertTrue(HttpsUrlPolicy.accepts("https://quran.com/1/1"))
        listOf("http://quran.com", "javascript:alert(1)", "https://name:key@example.com", "https://localhost",
            "https://127.0.0.1", "https://10.2.3.4", "https://192.168.2.1", "https://172.20.1.2",
            "https://[::1]", "https://example.com:8080", "https://example.com/#fragment").forEach {
            assertFalse("Unexpected accepted URL: $it", HttpsUrlPolicy.accepts(it))
        }
    }

}
