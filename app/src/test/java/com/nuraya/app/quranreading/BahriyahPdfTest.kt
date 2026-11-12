package com.nuraya.app.quranreading

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.NurayaApplication
import com.nuraya.app.ui.NurayaTheme
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = NurayaApplication::class, qualifiers = "w411dp-h891dp")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BahriyahPdfTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context: Context get() = ApplicationProvider.getApplicationContext<NurayaApplication>()

    @Before fun clearSelection() {
        context.getSharedPreferences("bahriyah_pdf", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun documentAndLastPagePersistWithoutMixingWithQuranVerseNumbers() {
        val store = BahriyahPdfStore(context)
        assertNull(store.read())
        val chosen = BahriyahDocument("content://documents/bahriyah", "Bahriyah edisi keluarga.pdf", 19)
        store.save(chosen)
        assertEquals(chosen, BahriyahPdfStore(context).read())
        assertEquals(1, boundedPdfPage(0, 20))
        assertEquals(20, boundedPdfPage(21, 20))
        assertEquals(19, boundedPdfPage(19, 20))
    }

    @Test fun emptyScreenExplainsLocalEditionAndOffersRealPdfImport() {
        compose.setContent { NurayaTheme { Surface { BahriyahPdfScreen("id") } } }
        compose.onNodeWithText("Halaman Bahriyah belum disertakan di Nuraya.", substring = true).assertExists()
        compose.onNodeWithTag("bahriyah_choose_pdf").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Buka PDF Mushaf Bahriyah", substring = false).assertIsDisplayed()
        compose.onNodeWithTag("bahriyah_pdf_page").assertDoesNotExist()
    }

    @Test fun savedPdfPresentationFillsTheReaderAndControlsOnlyOverlayAfterATap() {
        // Robolectric 28 has no native PDF renderer. Exercise the stored-document/UI flow
        // with a rendered fixture; this does not claim to validate Android's PDF decoding.
        val bitmap = android.graphics.Bitmap.createBitmap(641, 917, android.graphics.Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.WHITE)
        BahriyahPdfStore(context).save(BahriyahDocument("content://documents/bahriyah", "Test document.pdf", 1))
        compose.setContent { NurayaTheme { Surface {
            BahriyahPdfScreen("id", loadPage = { _, uri, page, zoom ->
                assertEquals("content://documents/bahriyah", uri.toString())
                assertEquals(1, page)
                assertEquals(1f, zoom)
                PdfFrame(bitmap, 1, 1)
            })
        } } }
        compose.waitUntil(30_000) { compose.onAllNodesWithTag("bahriyah_pdf_page").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("bahriyah_pdf_controls").assertDoesNotExist()
        val pageBounds = compose.onNodeWithTag("bahriyah_pdf_viewport").getUnclippedBoundsInRoot()
        assertEquals(compose.onNodeWithTag("bahriyah_pdf_screen").getUnclippedBoundsInRoot(), pageBounds)
        compose.onNodeWithTag("bahriyah_pdf_viewport").performTouchInput { click(center) }
        compose.onNodeWithTag("bahriyah_pdf_controls").assertIsDisplayed()
        assertEquals(pageBounds, compose.onNodeWithTag("bahriyah_pdf_viewport").getUnclippedBoundsInRoot())
        compose.onNodeWithText("Tutup menu baca").performClick()
        compose.onNodeWithTag("bahriyah_pdf_controls").assertDoesNotExist()
    }

    @Test fun inaccessibleSavedDocumentOffersRetryAndReplacementInsteadOfAnotherEdition() {
        BahriyahPdfStore(context).save(BahriyahDocument("content://missing.provider/bahriyah", "Mushaf.pdf", 1))
        compose.setContent { NurayaTheme { Surface { BahriyahPdfScreen("id") } } }
        compose.waitUntil(10_000) {
            runCatching { compose.onNodeWithText("Coba lagi").fetchSemanticsNode() }.isSuccess
        }
        compose.onNodeWithTag("bahriyah_pdf_screen").performScrollToNode(hasText("Coba lagi"))
        compose.onNodeWithText("Coba lagi").assertIsDisplayed()
        compose.onNodeWithTag("bahriyah_pdf_page").assertDoesNotExist()
        compose.onNodeWithTag("bahriyah_pdf_screen").performScrollToNode(hasTestTag("bahriyah_choose_pdf"))
        compose.onNodeWithText("Pilih PDF lain").assertIsDisplayed()
    }
}
