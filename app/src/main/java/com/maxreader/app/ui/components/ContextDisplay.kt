package com.maxreader.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxreader.app.model.RsvpWord
import com.maxreader.app.ui.theme.OrpColor
import com.maxreader.app.ui.theme.TextSecondary

/**
 * Shows the last word on top of the display.
 */
@Composable
fun LastWordDisplay(
    lastWord: RsvpWord?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = lastWord?.text ?: "",
            color = TextSecondary,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Shows a context line of the last N words, with the current word highlighted.
 */
@Composable
fun ContextLineDisplay(
    contextWords: List<RsvpWord>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (contextWords.isEmpty()) return@Box

        Text(
            text = buildAnnotatedString {
                contextWords.forEachIndexed { index, word ->
                    if (index == contextWords.size - 1) {
                        // Current word — highlighted
                        withStyle(SpanStyle(color = OrpColor, fontWeight = FontWeight.Bold)) {
                            append(word.text)
                        }
                    } else {
                        withStyle(SpanStyle(color = TextSecondary)) {
                            append(word.text)
                        }
                    }
                    if (index < contextWords.size - 1) append(" ")
                }
            },
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
