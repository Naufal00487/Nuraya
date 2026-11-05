package com.nuraya.app.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.nuraya.app.R

/**
 * Bundled, offline Amiri Quran font for the unchanged Tanzil Uthmani text.
 * This font includes Quranic marks and OpenType Arabic shaping; it does not
 * convert, normalize, or otherwise rewrite the underlying verse characters.
 * Source and SIL OFL 1.1 attribution: docs/QURAN_FONT.md and assets/licenses.
 */
val QuranArabicFont = FontFamily(
    Font(R.font.amiri_quran, weight = FontWeight.Normal, style = FontStyle.Normal),
)
