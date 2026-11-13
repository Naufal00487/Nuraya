package com.nuraya.app.quranreading

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize

/** Original pixels plus a separately drawn overlay, sharing one coordinate transform. */
@Composable
fun MushafImageLayer(
    bitmap: Bitmap,
    description: String,
    regions: List<MushafRegion>,
    selected: Pair<Int, Int>?,
    grayscale: Boolean,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onVerse: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val tap by rememberUpdatedState(onTap)
    val doubleTap by rememberUpdatedState(onDoubleTap)
    val choose by rememberUpdatedState(onVerse)
    val placement = remember(bitmap, size) {
        if (size.width > 0 && size.height > 0) MushafImagePlacement.fit(bitmap.width, bitmap.height, size.width.toFloat(), size.height.toFloat()) else null
    }
    val filter = remember(grayscale) { if (grayscale) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null }
    Box(modifier.onSizeChanged { size = it }.pointerInput(placement, regions) {
        detectTapGestures(onTap = { tap() }, onDoubleTap = { doubleTap() }, onLongPress = { point ->
            placement?.imagePoint(point.x, point.y)?.let { (x, y) ->
                regions.firstOrNull { region -> region.rectangles.any { it.contains(x, y) } }?.let { choose(it.surah, it.ayah) }
            }
        })
    }) {
        Image(bitmap.asImageBitmap(), description, Modifier.fillMaxSize().testTag("mushaf_page_image"), contentScale = ContentScale.Fit, colorFilter = filter)
        val active = regions.firstOrNull { it.surah == selected?.first && it.ayah == selected?.second }
        if (active != null && placement != null) Canvas(Modifier.fillMaxSize().testTag("mushaf_highlight_${active.surah}_${active.ayah}")) {
            active.rectangles.forEach { box ->
                drawRect(Color(0xFFFFD54F).copy(alpha = .28f),
                    topLeft = Offset(placement.left + box.left * placement.scale, placement.top + box.top * placement.scale),
                    size = Size((box.right - box.left) * placement.scale, (box.bottom - box.top) * placement.scale))
            }
        }
    }
}
