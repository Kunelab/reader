package io.github.kunelab.reader.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kunelab.reader.R
import io.github.kunelab.reader.model.RsvpWord
import io.github.kunelab.reader.settings.FontChoice
import io.github.kunelab.reader.ui.theme.*

@Composable
fun RsvpWordDisplay(
    word: RsvpWord?,
    contextWords: List<RsvpWord>,
    nextWords: List<RsvpWord> = emptyList(),
    fontSize: Int,
    showContext: Boolean,
    fontFamily: FontChoice = FontChoice.MONOSPACE,
    letterSpacing: Float = 0f,
    contextLineSpacing: Float = 1.2f,
    contextMargin: Int = 24,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current

    val resolvedFontFamily = when (fontFamily) {
        FontChoice.SERIF -> FontFamily.Serif
        FontChoice.SANS_SERIF -> FontFamily.SansSerif
        FontChoice.MONOSPACE -> FontFamily.Monospace
    }

    // An empty word would make the ORP coerce below collapse to an empty range and throw.
    if (word == null || word.text.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.reader_empty),
                color = tc.textMuted,
                fontSize = 20.sp
            )
        }
        return
    }

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    // Auto-scale font for long words to prevent overflow
    val baseSize = fontSize.toFloat()
    val wordLen = word.text.length
    val effectiveFontSize = when {
        wordLen > 18 -> (baseSize * 0.55f)
        wordLen > 14 -> (baseSize * 0.65f)
        wordLen > 11 -> (baseSize * 0.75f)
        wordLen > 8  -> (baseSize * 0.85f)
        else -> baseSize
    }
    val fontSizeSp = effectiveFontSize.sp

    // Use a reference character "M" to fix line height regardless of actual content.
    // Remembered because it depends only on the type settings, not on the word: at
    // 1500 WPM this composable runs 25 times a second and each measure builds a layout.
    val fixedLineHeightDp = remember(baseSize, resolvedFontFamily, letterSpacing, density) {
        val referenceStyle = TextStyle(
            fontSize = baseSize.sp,
            fontFamily = resolvedFontFamily,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = letterSpacing.sp
        )
        val referenceLayout = textMeasurer.measure(text = "Mj", style = referenceStyle)
        with(density) { referenceLayout.size.height.toDp() }
    }

    // Box layout: word stays centered, context above, next words below
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // === CENTER: The current word with guide ticks ===
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
        ) {
            // Top guide tick
            val accentColor = tc.accent
            Canvas(modifier = Modifier.width(1.dp).height(12.dp)) {
                drawLine(
                    color = accentColor,
                    start = Offset(size.width / 2, 0f),
                    end = Offset(size.width / 2, size.height),
                    strokeWidth = 2f
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Measure the prefix up to ORP to compute the offset
            val text = word.text
            val orpIdx = word.orpIndex.coerceIn(0, text.length - 1)

            val prefix = text.substring(0, orpIdx)
            val orpChar = text[orpIdx].toString()
            val suffix = if (orpIdx + 1 < text.length) text.substring(orpIdx + 1) else ""

            // Three more layouts per word, so keyed on everything that affects them.
            // Repeating the same word (or pausing) then costs no measuring at all.
            val offset = remember(text, orpIdx, fontSizeSp, resolvedFontFamily, letterSpacing, density) {
                val textStyle = TextStyle(
                    fontSize = fontSizeSp,
                    fontFamily = resolvedFontFamily,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = letterSpacing.sp
                )

                val prefixLayout = textMeasurer.measure(text = prefix, style = textStyle)
                val orpLayout = textMeasurer.measure(
                    text = orpChar,
                    style = textStyle.copy(fontWeight = FontWeight.ExtraBold)
                )
                val fullLayout = textMeasurer.measure(text = text, style = textStyle)

                with(density) {
                    (fullLayout.size.width / 2).toDp() -
                        prefixLayout.size.width.toDp() -
                        (orpLayout.size.width / 2).toDp()
                }
            }

            // Fixed-height box so the word never shifts vertically
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.height(fixedLineHeightDp)
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = tc.textPrimary, fontWeight = FontWeight.Normal)) {
                            append(prefix)
                        }
                        withStyle(SpanStyle(color = tc.orpColor, fontWeight = FontWeight.ExtraBold)) {
                            append(orpChar)
                        }
                        withStyle(SpanStyle(color = tc.textPrimary, fontWeight = FontWeight.Normal)) {
                            append(suffix)
                        }
                    },
                    fontSize = fontSizeSp,
                    fontFamily = resolvedFontFamily,
                    letterSpacing = letterSpacing.sp,
                    modifier = Modifier.offset(x = offset)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Bottom guide tick
            Canvas(modifier = Modifier.fillMaxWidth().height(8.dp)) {
                val centerX = size.width / 2
                drawLine(
                    color = accentColor,
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, size.height),
                    strokeWidth = 2f
                )
            }
        }

        // === TOP: Context trail (previous words) ===
        if (showContext && contextWords.isNotEmpty()) {
            val prevWords = contextWords.dropLast(1)
            if (prevWords.isNotEmpty()) {
                val contextFontSize = (fontSize * 0.33f).sp
                val contextLineHeight = (fontSize * 0.33f * contextLineSpacing).sp
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = -(fixedLineHeightDp / 2 + 24.dp))
                        .padding(horizontal = contextMargin.dp)
                ) {
                    Text(
                        text = buildAnnotatedString {
                            prevWords.forEachIndexed { index, w ->
                                if (index > 0) append(" ")
                                if (index == prevWords.size - 1) {
                                    withStyle(SpanStyle(color = tc.textSecondary, fontWeight = FontWeight.Bold)) {
                                        append(w.text)
                                    }
                                } else {
                                    append(w.text)
                                }
                            }
                        },
                        color = tc.textMuted,
                        fontSize = contextFontSize,
                        fontFamily = resolvedFontFamily,
                        textAlign = TextAlign.Center,
                        lineHeight = contextLineHeight
                    )
                }
            }
        }

        // === BOTTOM: Next words preview ===
        if (showContext && nextWords.isNotEmpty()) {
            val contextFontSize = (fontSize * 0.33f).sp
            val contextLineHeight = (fontSize * 0.33f * contextLineSpacing).sp
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = fixedLineHeightDp / 2 + 24.dp)
                    .padding(horizontal = contextMargin.dp)
            ) {
                Text(
                    text = nextWords.joinToString(" ") { it.text },
                    color = tc.textMuted.copy(alpha = 0.5f),
                    fontSize = contextFontSize,
                    fontFamily = resolvedFontFamily,
                    textAlign = TextAlign.Center,
                    lineHeight = contextLineHeight
                )
            }
        }
    }
}
