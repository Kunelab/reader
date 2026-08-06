package com.maxreader.app.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rsvp_settings")

data class RsvpSettings(
    val wpm: Int = 300,
    val commaPauseMs: Long = 150,
    val periodPauseMs: Long = 300,
    val paragraphPauseMs: Long = 400,
    val adaptiveSpeed: Boolean = true,
    val lengthThreshold: Int = 6,        // chars above which extra time is added
    val msPerExtraChar: Long = 12,       // ms per char beyond threshold
    val specialCharPenaltyMs: Long = 30,  // ms per special char (- ' / etc.)
    val contextWordCount: Int = 5,
    val nextWordCount: Int = 3,
    val showContext: Boolean = true,
    val fontSize: Int = 42,
    val rampUpEnabled: Boolean = true,
    val rampUpDurationWords: Int = 15,
    val theme: String = "DARK",
    val fontFamily: String = "monospace",
    val letterSpacing: Float = 0f,
    val contextLineSpacing: Float = 1.2f,
    val contextMarginHorizontal: Int = 24
)

class SettingsRepository(private val context: Context) {

    companion object {
        val KEY_WPM = intPreferencesKey("wpm")
        val KEY_COMMA_PAUSE = longPreferencesKey("comma_pause_ms")
        val KEY_PERIOD_PAUSE = longPreferencesKey("period_pause_ms")
        val KEY_PARAGRAPH_PAUSE = longPreferencesKey("paragraph_pause_ms")
        val KEY_ADAPTIVE_SPEED = booleanPreferencesKey("adaptive_speed")
        val KEY_LENGTH_THRESHOLD = intPreferencesKey("length_threshold")
        val KEY_MS_PER_EXTRA_CHAR = longPreferencesKey("ms_per_extra_char")
        val KEY_SPECIAL_CHAR_PENALTY = longPreferencesKey("special_char_penalty_ms")
        val KEY_CONTEXT_WORD_COUNT = intPreferencesKey("context_word_count")
        val KEY_NEXT_WORD_COUNT = intPreferencesKey("next_word_count")
        val KEY_SHOW_CONTEXT = booleanPreferencesKey("show_context")
        val KEY_SHOW_LAST_WORD = booleanPreferencesKey("show_last_word")
        val KEY_FONT_SIZE = intPreferencesKey("font_size")
        val KEY_RAMP_UP_ENABLED = booleanPreferencesKey("ramp_up_enabled")
        val KEY_RAMP_UP_DURATION = intPreferencesKey("ramp_up_duration_words")
        val KEY_THEME = stringPreferencesKey("theme")
        val KEY_FONT_FAMILY = stringPreferencesKey("font_family")
        val KEY_LETTER_SPACING = intPreferencesKey("letter_spacing_x10")  // stored as int * 10
        val KEY_CONTEXT_LINE_SPACING = intPreferencesKey("context_line_spacing_x10")
        val KEY_CONTEXT_MARGIN = intPreferencesKey("context_margin_horizontal")
    }

    val settingsFlow: Flow<RsvpSettings> = context.dataStore.data.map { prefs ->
        RsvpSettings(
            wpm = prefs[KEY_WPM] ?: 300,
            commaPauseMs = prefs[KEY_COMMA_PAUSE] ?: 150L,
            periodPauseMs = prefs[KEY_PERIOD_PAUSE] ?: 300L,
            paragraphPauseMs = prefs[KEY_PARAGRAPH_PAUSE] ?: 400L,
            adaptiveSpeed = prefs[KEY_ADAPTIVE_SPEED] ?: true,
            lengthThreshold = prefs[KEY_LENGTH_THRESHOLD] ?: 6,
            msPerExtraChar = prefs[KEY_MS_PER_EXTRA_CHAR] ?: 12L,
            specialCharPenaltyMs = prefs[KEY_SPECIAL_CHAR_PENALTY] ?: 30L,
            contextWordCount = prefs[KEY_CONTEXT_WORD_COUNT] ?: 5,
            nextWordCount = prefs[KEY_NEXT_WORD_COUNT] ?: 3,
            showContext = prefs[KEY_SHOW_CONTEXT] ?: true,
            fontSize = prefs[KEY_FONT_SIZE] ?: 42,
            rampUpEnabled = prefs[KEY_RAMP_UP_ENABLED] ?: true,
            rampUpDurationWords = prefs[KEY_RAMP_UP_DURATION] ?: 15,
            theme = prefs[KEY_THEME] ?: "DARK",
            fontFamily = prefs[KEY_FONT_FAMILY] ?: "monospace",
            letterSpacing = (prefs[KEY_LETTER_SPACING] ?: 0) / 10f,
            contextLineSpacing = (prefs[KEY_CONTEXT_LINE_SPACING] ?: 12) / 10f,
            contextMarginHorizontal = prefs[KEY_CONTEXT_MARGIN] ?: 24
        )
    }

    suspend fun updateWpm(wpm: Int) {
        context.dataStore.edit { it[KEY_WPM] = wpm.coerceIn(50, 1500) }
    }

    suspend fun updateCommaPause(ms: Long) {
        context.dataStore.edit { it[KEY_COMMA_PAUSE] = ms.coerceIn(0, 2000) }
    }

    suspend fun updatePeriodPause(ms: Long) {
        context.dataStore.edit { it[KEY_PERIOD_PAUSE] = ms.coerceIn(0, 3000) }
    }

    suspend fun updateParagraphPause(ms: Long) {
        context.dataStore.edit { it[KEY_PARAGRAPH_PAUSE] = ms.coerceIn(0, 5000) }
    }

    suspend fun updateContextWordCount(count: Int) {
        context.dataStore.edit { it[KEY_CONTEXT_WORD_COUNT] = count.coerceIn(1, 50) }
    }

    suspend fun updateNextWordCount(count: Int) {
        context.dataStore.edit { it[KEY_NEXT_WORD_COUNT] = count.coerceIn(0, 50) }
    }

    suspend fun updateShowContext(show: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_CONTEXT] = show }
    }

    suspend fun updateAdaptiveSpeed(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ADAPTIVE_SPEED] = enabled }
    }

    suspend fun updateLengthThreshold(chars: Int) {
        context.dataStore.edit { it[KEY_LENGTH_THRESHOLD] = chars.coerceIn(3, 15) }
    }

    suspend fun updateMsPerExtraChar(ms: Long) {
        context.dataStore.edit { it[KEY_MS_PER_EXTRA_CHAR] = ms.coerceIn(0, 100) }
    }

    suspend fun updateSpecialCharPenalty(ms: Long) {
        context.dataStore.edit { it[KEY_SPECIAL_CHAR_PENALTY] = ms.coerceIn(0, 200) }
    }

    suspend fun updateFontSize(size: Int) {
        context.dataStore.edit { it[KEY_FONT_SIZE] = size.coerceIn(20, 80) }
    }

    suspend fun updateRampUpEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_RAMP_UP_ENABLED] = enabled }
    }

    suspend fun updateRampUpDuration(words: Int) {
        context.dataStore.edit { it[KEY_RAMP_UP_DURATION] = words.coerceIn(5, 50) }
    }

    suspend fun updateTheme(theme: String) {
        context.dataStore.edit { it[KEY_THEME] = theme }
    }

    suspend fun updateFontFamily(family: String) {
        context.dataStore.edit { it[KEY_FONT_FAMILY] = family }
    }

    suspend fun updateLetterSpacing(sp: Float) {
        context.dataStore.edit { it[KEY_LETTER_SPACING] = (sp * 10).toInt().coerceIn(-20, 50) }
    }

    suspend fun updateContextLineSpacing(sp: Float) {
        context.dataStore.edit { it[KEY_CONTEXT_LINE_SPACING] = (sp * 10).toInt().coerceIn(8, 30) }
    }

    suspend fun updateContextMargin(dp: Int) {
        context.dataStore.edit { it[KEY_CONTEXT_MARGIN] = dp.coerceIn(0, 80) }
    }
}
