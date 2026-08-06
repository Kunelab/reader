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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxreader.app.R
import com.maxreader.app.settings.FontChoice
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
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
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
                SectionHeader(stringResource(R.string.section_theme), tc)

                val themes = AppTheme.entries
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    themes.forEach { theme ->
                        val isSelected = settings.theme == theme
                        val previewColors = themeColorsFor(theme)
                        OutlinedButton(
                            onClick = { viewModel.updateSettings { s -> s.copy(theme = theme) } },
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
                                text = stringResource(theme.labelRes),
                                fontSize = 11.sp,
                                color = previewColors.textPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // --- SPEED ---
                SectionHeader(stringResource(R.string.section_speed), tc)

                SliderSetting(
                    label = stringResource(R.string.setting_wpm),
                    value = settings.wpm.toFloat(),
                    valueText = stringResource(R.string.value_wpm, settings.wpm),
                    range = 50f..1500f,
                    steps = 57,
                    onValueChange = { viewModel.updateSettings { s -> s.copy(wpm = it.toInt()) } },
                    tc = tc
                )

                SwitchSetting(
                    label = stringResource(R.string.setting_ramp_up),
                    checked = settings.rampUpEnabled,
                    onCheckedChange = { viewModel.updateSettings { s -> s.copy(rampUpEnabled = it) } },
                    tc = tc
                )

                if (settings.rampUpEnabled) {
                    SliderSetting(
                        label = stringResource(R.string.setting_ramp_up_words),
                        value = settings.rampUpDurationWords.toFloat(),
                        valueText = pluralStringResource(
                            R.plurals.value_words,
                            settings.rampUpDurationWords,
                            settings.rampUpDurationWords
                        ),
                        range = 5f..50f,
                        steps = 8,
                        onValueChange = { viewModel.updateSettings { s -> s.copy(rampUpDurationWords = it.toInt()) } },
                        tc = tc
                    )
                }

                SwitchSetting(
                    label = stringResource(R.string.setting_adaptive_speed),
                    checked = settings.adaptiveSpeed,
                    onCheckedChange = { viewModel.updateSettings { s -> s.copy(adaptiveSpeed = it) } },
                    tc = tc
                )

                if (settings.adaptiveSpeed) {
                    SliderSetting(
                        label = stringResource(R.string.setting_length_threshold),
                        value = settings.lengthThreshold.toFloat(),
                        valueText = pluralStringResource(
                            R.plurals.value_chars,
                            settings.lengthThreshold,
                            settings.lengthThreshold
                        ),
                        range = 3f..15f,
                        steps = 11,
                        onValueChange = { viewModel.updateSettings { s -> s.copy(lengthThreshold = it.toInt()) } },
                        tc = tc
                    )

                    SliderSetting(
                        label = stringResource(R.string.setting_ms_per_char),
                        value = settings.msPerExtraChar.toFloat(),
                        valueText = stringResource(R.string.value_ms, settings.msPerExtraChar),
                        range = 0f..100f,
                        steps = 19,
                        onValueChange = { viewModel.updateSettings { s -> s.copy(msPerExtraChar = it.toLong()) } },
                        tc = tc
                    )

                    SliderSetting(
                        label = stringResource(R.string.setting_special_char_penalty),
                        value = settings.specialCharPenaltyMs.toFloat(),
                        valueText = stringResource(R.string.value_ms, settings.specialCharPenaltyMs),
                        range = 0f..200f,
                        steps = 19,
                        onValueChange = { viewModel.updateSettings { s -> s.copy(specialCharPenaltyMs = it.toLong()) } },
                        tc = tc
                    )
                }

                // --- PUNCTUATION PAUSES ---
                SectionHeader(stringResource(R.string.section_punctuation), tc)

                NumberInputSetting(
                    label = stringResource(R.string.setting_comma_pause),
                    value = settings.commaPauseMs,
                    suffix = stringResource(R.string.unit_ms),
                    onValueChange = { viewModel.updateSettings { s -> s.copy(commaPauseMs = it) } },
                    tc = tc
                )

                NumberInputSetting(
                    label = stringResource(R.string.setting_period_pause),
                    value = settings.periodPauseMs,
                    suffix = stringResource(R.string.unit_ms),
                    onValueChange = { viewModel.updateSettings { s -> s.copy(periodPauseMs = it) } },
                    tc = tc
                )

                NumberInputSetting(
                    label = stringResource(R.string.setting_paragraph_pause),
                    value = settings.paragraphPauseMs,
                    suffix = stringResource(R.string.unit_ms),
                    onValueChange = { viewModel.updateSettings { s -> s.copy(paragraphPauseMs = it) } },
                    tc = tc
                )

                // --- DISPLAY ---
                SectionHeader(stringResource(R.string.section_display), tc)

                SliderSetting(
                    label = stringResource(R.string.setting_font_size),
                    value = settings.fontSize.toFloat(),
                    valueText = stringResource(R.string.value_sp, settings.fontSize),
                    range = 20f..80f,
                    steps = 29,
                    onValueChange = { viewModel.updateSettings { s -> s.copy(fontSize = it.toInt()) } },
                    tc = tc
                )

                // Font family picker — the stored value is the CSS-style family name,
                // the label next to it is what the user sees.
                val fontOptions = listOf(
                    FontChoice.MONOSPACE to R.string.font_monospace,
                    FontChoice.SANS_SERIF to R.string.font_sans_serif,
                    FontChoice.SERIF to R.string.font_serif
                )
                Text(text = stringResource(R.string.setting_font), color = tc.textPrimary, fontSize = 15.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    fontOptions.forEach { (font, labelRes) ->
                        val isSelected = settings.fontFamily == font
                        OutlinedButton(
                            onClick = { viewModel.updateSettings { s -> s.copy(fontFamily = font) } },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) tc.accent else tc.textMuted
                            )
                        ) {
                            Text(
                                text = stringResource(labelRes),
                                fontSize = 13.sp,
                                color = if (isSelected) tc.accent else tc.textPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                SliderSetting(
                    label = stringResource(R.string.setting_letter_spacing),
                    value = settings.letterSpacing,
                    valueText = stringResource(R.string.value_sp_decimal, settings.letterSpacing),
                    range = -2f..5f,
                    steps = 13,
                    onValueChange = { viewModel.updateSettings { s -> s.copy(letterSpacing = it) } },
                    tc = tc
                )

                SwitchSetting(
                    label = stringResource(R.string.setting_show_context),
                    checked = settings.showContext,
                    onCheckedChange = { viewModel.updateSettings { s -> s.copy(showContext = it) } },
                    tc = tc
                )

                if (settings.showContext) {
                    SliderSetting(
                        label = stringResource(R.string.setting_previous_words),
                        value = settings.contextWordCount.toFloat(),
                        valueText = pluralStringResource(
                            R.plurals.value_words,
                            settings.contextWordCount,
                            settings.contextWordCount
                        ),
                        range = 1f..50f,
                        steps = 48,
                        onValueChange = { viewModel.updateSettings { s -> s.copy(contextWordCount = it.toInt()) } },
                        tc = tc
                    )

                    SliderSetting(
                        label = stringResource(R.string.setting_next_words),
                        value = settings.nextWordCount.toFloat(),
                        valueText = pluralStringResource(
                            R.plurals.value_words,
                            settings.nextWordCount,
                            settings.nextWordCount
                        ),
                        range = 0f..50f,
                        steps = 49,
                        onValueChange = { viewModel.updateSettings { s -> s.copy(nextWordCount = it.toInt()) } },
                        tc = tc
                    )

                    SliderSetting(
                        label = stringResource(R.string.setting_context_line_spacing),
                        value = settings.contextLineSpacing,
                        valueText = stringResource(R.string.value_multiplier, settings.contextLineSpacing),
                        range = 0.8f..3f,
                        steps = 21,
                        onValueChange = { viewModel.updateSettings { s -> s.copy(contextLineSpacing = it) } },
                        tc = tc
                    )

                    SliderSetting(
                        label = stringResource(R.string.setting_context_margins),
                        value = settings.contextMarginHorizontal.toFloat(),
                        valueText = stringResource(R.string.value_dp, settings.contextMarginHorizontal),
                        range = 0f..80f,
                        steps = 15,
                        onValueChange = { viewModel.updateSettings { s -> s.copy(contextMarginHorizontal = it.toInt()) } },
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
