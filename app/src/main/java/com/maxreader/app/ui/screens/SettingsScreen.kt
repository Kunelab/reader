package com.maxreader.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxreader.app.ui.theme.*
import com.maxreader.app.viewmodel.ReaderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ReaderViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val tc = LocalThemeColors.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = tc.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = tc.surface,
                    titleContentColor = tc.textPrimary,
                    navigationIconContentColor = tc.textPrimary
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // --- THEME ---
                SectionHeader("Theme", tc)

                val themes = AppTheme.entries
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    themes.forEach { theme ->
                        val isSelected = settings.theme == theme.name
                        val previewColors = themeColorsFor(theme)
                        OutlinedButton(
                            onClick = { viewModel.setTheme(theme.name) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) tc.accent else tc.textMuted
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = previewColors.background
                            ),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                text = theme.label,
                                fontSize = 11.sp,
                                color = previewColors.textPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // --- SPEED ---
                SectionHeader("Speed", tc)

                SliderSetting(
                    label = "Words Per Minute",
                    value = settings.wpm.toFloat(),
                    valueText = "${settings.wpm} WPM",
                    range = 50f..1500f,
                    steps = 57,
                    onValueChange = { viewModel.setWpm(it.toInt()) },
                    tc = tc
                )

                SwitchSetting(
                    label = "Speed ramp-up",
                    checked = settings.rampUpEnabled,
                    onCheckedChange = { viewModel.setRampUpEnabled(it) },
                    tc = tc
                )

                if (settings.rampUpEnabled) {
                    SliderSetting(
                        label = "Ramp-up words",
                        value = settings.rampUpDurationWords.toFloat(),
                        valueText = "${settings.rampUpDurationWords} words",
                        range = 5f..50f,
                        steps = 8,
                        onValueChange = { viewModel.setRampUpDuration(it.toInt()) },
                        tc = tc
                    )
                }

                SwitchSetting(
                    label = "Adaptive speed",
                    checked = settings.adaptiveSpeed,
                    onCheckedChange = { viewModel.setAdaptiveSpeed(it) },
                    tc = tc
                )

                if (settings.adaptiveSpeed) {
                    SliderSetting(
                        label = "Length threshold",
                        value = settings.lengthThreshold.toFloat(),
                        valueText = "${settings.lengthThreshold} chars",
                        range = 3f..15f,
                        steps = 11,
                        onValueChange = { viewModel.setLengthThreshold(it.toInt()) },
                        tc = tc
                    )

                    SliderSetting(
                        label = "Extra ms per char",
                        value = settings.msPerExtraChar.toFloat(),
                        valueText = "${settings.msPerExtraChar} ms",
                        range = 0f..100f,
                        steps = 19,
                        onValueChange = { viewModel.setMsPerExtraChar(it.toLong()) },
                        tc = tc
                    )

                    SliderSetting(
                        label = "Special char penalty",
                        value = settings.specialCharPenaltyMs.toFloat(),
                        valueText = "${settings.specialCharPenaltyMs} ms",
                        range = 0f..200f,
                        steps = 19,
                        onValueChange = { viewModel.setSpecialCharPenalty(it.toLong()) },
                        tc = tc
                    )
                }

                // --- PUNCTUATION PAUSES ---
                SectionHeader("Punctuation Pauses", tc)

                NumberInputSetting(
                    label = "Comma / semicolon / colon pause",
                    value = settings.commaPauseMs,
                    suffix = "ms",
                    onValueChange = { viewModel.setCommaPause(it) },
                    tc = tc
                )

                NumberInputSetting(
                    label = "Period / ! / ? pause",
                    value = settings.periodPauseMs,
                    suffix = "ms",
                    onValueChange = { viewModel.setPeriodPause(it) },
                    tc = tc
                )

                NumberInputSetting(
                    label = "Paragraph break pause",
                    value = settings.paragraphPauseMs,
                    suffix = "ms",
                    onValueChange = { viewModel.setParagraphPause(it) },
                    tc = tc
                )

                // --- DISPLAY ---
                SectionHeader("Display", tc)

                SliderSetting(
                    label = "Font Size",
                    value = settings.fontSize.toFloat(),
                    valueText = "${settings.fontSize} sp",
                    range = 20f..80f,
                    steps = 29,
                    onValueChange = { viewModel.setFontSize(it.toInt()) },
                    tc = tc
                )

                // Font family picker
                val fontOptions = listOf("monospace", "sans-serif", "serif")
                Text(text = "Font", color = tc.textPrimary, fontSize = 15.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    fontOptions.forEach { font ->
                        val isSelected = settings.fontFamily == font
                        OutlinedButton(
                            onClick = { viewModel.setFontFamily(font) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) tc.accent else tc.textMuted
                            )
                        ) {
                            Text(
                                text = font.replaceFirstChar { it.uppercase() },
                                fontSize = 13.sp,
                                color = if (isSelected) tc.accent else tc.textPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                SliderSetting(
                    label = "Letter spacing",
                    value = settings.letterSpacing,
                    valueText = "${"%.1f".format(settings.letterSpacing)} sp",
                    range = -2f..5f,
                    steps = 13,
                    onValueChange = { viewModel.setLetterSpacing(it) },
                    tc = tc
                )

                SwitchSetting(
                    label = "Show context words",
                    checked = settings.showContext,
                    onCheckedChange = { viewModel.setShowContext(it) },
                    tc = tc
                )

                if (settings.showContext) {
                    SliderSetting(
                        label = "Previous words",
                        value = settings.contextWordCount.toFloat(),
                        valueText = "${settings.contextWordCount} words",
                        range = 1f..50f,
                        steps = 48,
                        onValueChange = { viewModel.setContextWordCount(it.toInt()) },
                        tc = tc
                    )

                    SliderSetting(
                        label = "Next words",
                        value = settings.nextWordCount.toFloat(),
                        valueText = "${settings.nextWordCount} words",
                        range = 0f..50f,
                        steps = 49,
                        onValueChange = { viewModel.setNextWordCount(it.toInt()) },
                        tc = tc
                    )

                    SliderSetting(
                        label = "Context line spacing",
                        value = settings.contextLineSpacing,
                        valueText = "${"%.1f".format(settings.contextLineSpacing)}x",
                        range = 0.8f..3f,
                        steps = 21,
                        onValueChange = { viewModel.setContextLineSpacing(it) },
                        tc = tc
                    )

                    SliderSetting(
                        label = "Context margins",
                        value = settings.contextMarginHorizontal.toFloat(),
                        valueText = "${settings.contextMarginHorizontal} dp",
                        range = 0f..80f,
                        steps = 15,
                        onValueChange = { viewModel.setContextMargin(it.toInt()) },
                        tc = tc
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, tc: ThemeColors) {
    Text(
        text = title.uppercase(),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = tc.accent,
        letterSpacing = 1.5.sp
    )
}

@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    valueText: String,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    tc: ThemeColors
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = tc.textPrimary, fontSize = 15.sp)
            Text(text = valueText, color = tc.accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = tc.accent,
                activeTrackColor = tc.accent,
                inactiveTrackColor = tc.surface
            )
        )
    }
}

@Composable
private fun NumberInputSetting(
    label: String,
    value: Long,
    suffix: String,
    onValueChange: (Long) -> Unit,
    tc: ThemeColors
) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }

    Column {
        Text(text = label, color = tc.textPrimary, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = textValue,
                onValueChange = { newText ->
                    val filtered = newText.filter { it.isDigit() }
                    textValue = filtered
                    filtered.toLongOrNull()?.let { onValueChange(it) }
                },
                modifier = Modifier.width(120.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = tc.textPrimary,
                    unfocusedTextColor = tc.textPrimary,
                    focusedBorderColor = tc.accent,
                    unfocusedBorderColor = tc.textMuted,
                    cursorColor = tc.accent
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = suffix, color = tc.textSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun SwitchSetting(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tc: ThemeColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = tc.textPrimary, fontSize = 15.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = tc.textPrimary,
                checkedTrackColor = tc.accent,
                uncheckedThumbColor = tc.textSecondary,
                uncheckedTrackColor = tc.surface
            )
        )
    }
}
