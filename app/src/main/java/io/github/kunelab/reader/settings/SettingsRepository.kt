package io.github.kunelab.reader.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.kunelab.reader.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rsvp_settings")

/** The typeface options offered for the RSVP word. */
enum class FontChoice { MONOSPACE, SANS_SERIF, SERIF }

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
    val theme: AppTheme = AppTheme.DARK,
    val fontFamily: FontChoice = FontChoice.MONOSPACE,
    val letterSpacing: Float = 0f,
    val contextLineSpacing: Float = 1.2f,
    val contextMarginHorizontal: Int = 24
)

/**
 * Clamps every value to a range the reader can actually display.
 *
 * Kept in one place so the bounds are visible together; they used to be spread across
 * nineteen near-identical update methods.
 */
private fun RsvpSettings.coerced(): RsvpSettings = copy(
    wpm = wpm.coerceIn(50, 1500),
    commaPauseMs = commaPauseMs.coerceIn(0, 2000),
    periodPauseMs = periodPauseMs.coerceIn(0, 3000),
    paragraphPauseMs = paragraphPauseMs.coerceIn(0, 5000),
    lengthThreshold = lengthThreshold.coerceIn(3, 15),
    msPerExtraChar = msPerExtraChar.coerceIn(0, 100),
    specialCharPenaltyMs = specialCharPenaltyMs.coerceIn(0, 200),
    contextWordCount = contextWordCount.coerceIn(1, 50),
    nextWordCount = nextWordCount.coerceIn(0, 50),
    fontSize = fontSize.coerceIn(20, 80),
    rampUpDurationWords = rampUpDurationWords.coerceIn(5, 50),
    letterSpacing = letterSpacing.coerceIn(-2f, 5f),
    contextLineSpacing = contextLineSpacing.coerceIn(0.8f, 3f),
    contextMarginHorizontal = contextMarginHorizontal.coerceIn(0, 80)
)

class SettingsRepository(private val context: Context) {

    private companion object {
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
        val KEY_FONT_SIZE = intPreferencesKey("font_size")
        val KEY_RAMP_UP_ENABLED = booleanPreferencesKey("ramp_up_enabled")
        val KEY_RAMP_UP_DURATION = intPreferencesKey("ramp_up_duration_words")
        val KEY_THEME = stringPreferencesKey("theme")
        val KEY_FONT_FAMILY = stringPreferencesKey("font_family")
        val KEY_LETTER_SPACING = intPreferencesKey("letter_spacing_x10")  // stored as int * 10
        val KEY_CONTEXT_LINE_SPACING = intPreferencesKey("context_line_spacing_x10")
        val KEY_CONTEXT_MARGIN = intPreferencesKey("context_margin_horizontal")

        /** Falls back to the default if the stored name is not one we recognise. */
        inline fun <reified T : Enum<T>> String?.toEnumOr(fallback: T): T =
            this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback
    }

    private val defaults = RsvpSettings()

    val settingsFlow: Flow<RsvpSettings> = context.dataStore.data.map { prefs ->
        RsvpSettings(
            wpm = prefs[KEY_WPM] ?: defaults.wpm,
            commaPauseMs = prefs[KEY_COMMA_PAUSE] ?: defaults.commaPauseMs,
            periodPauseMs = prefs[KEY_PERIOD_PAUSE] ?: defaults.periodPauseMs,
            paragraphPauseMs = prefs[KEY_PARAGRAPH_PAUSE] ?: defaults.paragraphPauseMs,
            adaptiveSpeed = prefs[KEY_ADAPTIVE_SPEED] ?: defaults.adaptiveSpeed,
            lengthThreshold = prefs[KEY_LENGTH_THRESHOLD] ?: defaults.lengthThreshold,
            msPerExtraChar = prefs[KEY_MS_PER_EXTRA_CHAR] ?: defaults.msPerExtraChar,
            specialCharPenaltyMs = prefs[KEY_SPECIAL_CHAR_PENALTY] ?: defaults.specialCharPenaltyMs,
            contextWordCount = prefs[KEY_CONTEXT_WORD_COUNT] ?: defaults.contextWordCount,
            nextWordCount = prefs[KEY_NEXT_WORD_COUNT] ?: defaults.nextWordCount,
            showContext = prefs[KEY_SHOW_CONTEXT] ?: defaults.showContext,
            fontSize = prefs[KEY_FONT_SIZE] ?: defaults.fontSize,
            rampUpEnabled = prefs[KEY_RAMP_UP_ENABLED] ?: defaults.rampUpEnabled,
            rampUpDurationWords = prefs[KEY_RAMP_UP_DURATION] ?: defaults.rampUpDurationWords,
            theme = prefs[KEY_THEME].toEnumOr(defaults.theme),
            fontFamily = prefs[KEY_FONT_FAMILY].toEnumOr(defaults.fontFamily),
            letterSpacing = (prefs[KEY_LETTER_SPACING] ?: 0) / 10f,
            contextLineSpacing = (prefs[KEY_CONTEXT_LINE_SPACING] ?: 12) / 10f,
            contextMarginHorizontal = prefs[KEY_CONTEXT_MARGIN] ?: defaults.contextMarginHorizontal
        )
    }

    /**
     * Applies [transform] to the stored settings and writes the result.
     *
     * One entry point instead of a setter per field: the setters were nineteen copies of
     * the same three lines, and each mapped to its own method on the ViewModel.
     */
    suspend fun update(transform: (RsvpSettings) -> RsvpSettings) {
        context.dataStore.edit { prefs ->
            val current = RsvpSettings(
                wpm = prefs[KEY_WPM] ?: defaults.wpm,
                commaPauseMs = prefs[KEY_COMMA_PAUSE] ?: defaults.commaPauseMs,
                periodPauseMs = prefs[KEY_PERIOD_PAUSE] ?: defaults.periodPauseMs,
                paragraphPauseMs = prefs[KEY_PARAGRAPH_PAUSE] ?: defaults.paragraphPauseMs,
                adaptiveSpeed = prefs[KEY_ADAPTIVE_SPEED] ?: defaults.adaptiveSpeed,
                lengthThreshold = prefs[KEY_LENGTH_THRESHOLD] ?: defaults.lengthThreshold,
                msPerExtraChar = prefs[KEY_MS_PER_EXTRA_CHAR] ?: defaults.msPerExtraChar,
                specialCharPenaltyMs = prefs[KEY_SPECIAL_CHAR_PENALTY] ?: defaults.specialCharPenaltyMs,
                contextWordCount = prefs[KEY_CONTEXT_WORD_COUNT] ?: defaults.contextWordCount,
                nextWordCount = prefs[KEY_NEXT_WORD_COUNT] ?: defaults.nextWordCount,
                showContext = prefs[KEY_SHOW_CONTEXT] ?: defaults.showContext,
                fontSize = prefs[KEY_FONT_SIZE] ?: defaults.fontSize,
                rampUpEnabled = prefs[KEY_RAMP_UP_ENABLED] ?: defaults.rampUpEnabled,
                rampUpDurationWords = prefs[KEY_RAMP_UP_DURATION] ?: defaults.rampUpDurationWords,
                theme = prefs[KEY_THEME].toEnumOr(defaults.theme),
                fontFamily = prefs[KEY_FONT_FAMILY].toEnumOr(defaults.fontFamily),
                letterSpacing = (prefs[KEY_LETTER_SPACING] ?: 0) / 10f,
                contextLineSpacing = (prefs[KEY_CONTEXT_LINE_SPACING] ?: 12) / 10f,
                contextMarginHorizontal = prefs[KEY_CONTEXT_MARGIN] ?: defaults.contextMarginHorizontal
            )

            val next = transform(current).coerced()

            prefs[KEY_WPM] = next.wpm
            prefs[KEY_COMMA_PAUSE] = next.commaPauseMs
            prefs[KEY_PERIOD_PAUSE] = next.periodPauseMs
            prefs[KEY_PARAGRAPH_PAUSE] = next.paragraphPauseMs
            prefs[KEY_ADAPTIVE_SPEED] = next.adaptiveSpeed
            prefs[KEY_LENGTH_THRESHOLD] = next.lengthThreshold
            prefs[KEY_MS_PER_EXTRA_CHAR] = next.msPerExtraChar
            prefs[KEY_SPECIAL_CHAR_PENALTY] = next.specialCharPenaltyMs
            prefs[KEY_CONTEXT_WORD_COUNT] = next.contextWordCount
            prefs[KEY_NEXT_WORD_COUNT] = next.nextWordCount
            prefs[KEY_SHOW_CONTEXT] = next.showContext
            prefs[KEY_FONT_SIZE] = next.fontSize
            prefs[KEY_RAMP_UP_ENABLED] = next.rampUpEnabled
            prefs[KEY_RAMP_UP_DURATION] = next.rampUpDurationWords
            prefs[KEY_THEME] = next.theme.name
            prefs[KEY_FONT_FAMILY] = next.fontFamily.name
            prefs[KEY_LETTER_SPACING] = (next.letterSpacing * 10).toInt()
            prefs[KEY_CONTEXT_LINE_SPACING] = (next.contextLineSpacing * 10).toInt()
            prefs[KEY_CONTEXT_MARGIN] = next.contextMarginHorizontal
        }
    }
}
