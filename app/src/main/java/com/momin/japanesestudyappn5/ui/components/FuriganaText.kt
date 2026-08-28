package com.momin.japanesestudyappn5.ui.components

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.momin.japanesestudyappn5.data.model.FuriganaParser
import com.momin.japanesestudyappn5.data.model.FuriganaSegment
import com.momin.japanesestudyappn5.util.KanjiConverter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FuriganaText(
    rawText: String,
    modifier: Modifier = Modifier,
    mainFontSize: TextUnit = 22.sp,
    furiganaFontSize: TextUnit = 11.sp,
    mainColor: Color = MaterialTheme.colorScheme.onSurface,
    furiganaColor: Color = MaterialTheme.colorScheme.primary,
    fontWeight: FontWeight = FontWeight.Bold,
    kanjiDisabledOverride: Boolean? = null
) {
    val context = LocalContext.current
    val isKanjiDisabled = remember(kanjiDisabledOverride, rawText) {
        kanjiDisabledOverride ?: run {
            val prefs = context.getSharedPreferences("japanese_study_prefs", Context.MODE_PRIVATE)
            prefs.getBoolean("kanji_disabled", false) || prefs.getBoolean("disable_kanji", false)
        }
    }

    val segments = FuriganaParser.parse(rawText)

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalArrangement = Arrangement.Bottom
    ) {
        segments.forEach { segment ->
            FuriganaSegmentView(
                segment = segment,
                mainFontSize = mainFontSize,
                furiganaFontSize = furiganaFontSize,
                mainColor = mainColor,
                furiganaColor = furiganaColor,
                fontWeight = fontWeight,
                kanjiDisabled = isKanjiDisabled
            )
        }
    }
}

@Composable
fun FuriganaSegmentView(
    segment: FuriganaSegment,
    mainFontSize: TextUnit = 22.sp,
    furiganaFontSize: TextUnit = 11.sp,
    mainColor: Color = MaterialTheme.colorScheme.onSurface,
    furiganaColor: Color = MaterialTheme.colorScheme.primary,
    fontWeight: FontWeight = FontWeight.Bold,
    kanjiDisabled: Boolean = false
) {
    if (kanjiDisabled) {
        val kanaText = if (!segment.furigana.isNullOrBlank()) {
            segment.furigana
        } else {
            KanjiConverter.toKana(segment.text)
        }
        Text(
            text = kanaText,
            fontSize = mainFontSize,
            lineHeight = mainFontSize,
            color = mainColor,
            fontWeight = fontWeight,
            textAlign = TextAlign.Center
        )
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            if (!segment.furigana.isNullOrBlank()) {
                Text(
                    text = segment.furigana,
                    fontSize = furiganaFontSize,
                    lineHeight = furiganaFontSize,
                    color = furiganaColor,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            } else {
                Spacer(modifier = Modifier.height(with(androidx.compose.ui.platform.LocalDensity.current) { furiganaFontSize.toDp() }))
            }
            Text(
                text = segment.text,
                fontSize = mainFontSize,
                lineHeight = mainFontSize,
                color = mainColor,
                fontWeight = fontWeight,
                textAlign = TextAlign.Center
            )
        }
    }
}
