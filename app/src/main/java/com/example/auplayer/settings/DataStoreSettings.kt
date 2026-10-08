package com.example.auplayer.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.auplayer.core.AppSettings
import com.example.auplayer.core.AspectSetting
import com.example.auplayer.core.DecoderPref
import com.example.auplayer.core.EnhanceMode
import com.example.auplayer.core.OrientationSetting
import com.example.auplayer.core.ReaderFilter
import com.example.auplayer.core.ReaderModeSetting
import com.example.auplayer.core.SettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore(name = "au_settings")

/** DataStore backing for [SettingsStore] (MS-024). Keys mirror [AppSettings]. */
class DataStoreSettingsStore(private val context: Context) : SettingsStore {
    override val settings: Flow<AppSettings> = context.store.data.map { prefs -> readAll(prefs) }

    private fun readAll(prefs: Preferences): AppSettings {
        val playback = withPlayback(AppSettings(), prefs)
        val subs = withSubs(playback, prefs)
        return withReader(subs, prefs)
    }

    private fun withPlayback(base: AppSettings, prefs: Preferences): AppSettings {
        return base.copy(
            resume = prefs[RESUME] ?: true,
            autoNext = prefs[AUTO_NEXT] ?: true,
            backgroundPlay = prefs[BACKGROUND] ?: true,
            pip = prefs[PIP] ?: true,
            gestureSeek = prefs[G_SEEK] ?: true,
            gestureBrightness = prefs[G_BRIGHT] ?: true,
            gestureVolume = prefs[G_VOL] ?: true,
            doubleTapSeconds = prefs[TAP_SECONDS] ?: 10L,
            decoder = prefs[DECODER]?.toEnum() ?: DecoderPref.HARDWARE,
            enhanceMode = prefs[ENHANCE]?.toEnum() ?: EnhanceMode.OFF
        )
    }

    private fun withSubs(base: AppSettings, prefs: Preferences): AppSettings {
        return base.copy(
            subSizeSp = prefs[SUB_SIZE] ?: 16,
            subColorArgb = prefs[SUB_COLOR] ?: -1L,
            subOutline = prefs[SUB_OUTLINE] ?: 2,
            subPosition = prefs[SUB_POS] ?: 100f,
            subFont = prefs[SUB_FONT],
            subDelaySec = prefs[SUB_DELAY] ?: 0.0,
            audioBoost = prefs[A_BOOST] ?: 1f,
            audioLanguage = prefs[A_LANG],
            downmix = prefs[DOWNMIX] ?: true
        )
    }

    private fun withReader(base: AppSettings, prefs: Preferences): AppSettings {
        return base.copy(
            orientation = prefs[ORIENT]?.toEnum() ?: OrientationSetting.AUTO,
            defaultAspect = prefs[ASPECT]?.toEnum() ?: AspectSetting.FIT,
            readerMode = prefs[R_MODE]?.toEnum() ?: ReaderModeSetting.LTR,
            readerKeepScreenOn = prefs[R_KEEP_ON] ?: true,
            readerFilter = prefs[R_FILTER]?.toEnum() ?: ReaderFilter.NONE,
            readerCropBorders = prefs[R_CROP] ?: false
        )
    }

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        val current = settings.first()
        val next = transform(current)
        context.store.edit { prefs ->
            prefs[RESUME] = next.resume
            prefs[AUTO_NEXT] = next.autoNext
            prefs[BACKGROUND] = next.backgroundPlay
            prefs[PIP] = next.pip
            prefs[G_SEEK] = next.gestureSeek
            prefs[G_BRIGHT] = next.gestureBrightness
            prefs[G_VOL] = next.gestureVolume
            prefs[TAP_SECONDS] = next.doubleTapSeconds
            prefs[SUB_SIZE] = next.subSizeSp
            prefs[SUB_COLOR] = next.subColorArgb
            prefs[SUB_OUTLINE] = next.subOutline
            prefs[SUB_POS] = next.subPosition
            prefs[SUB_DELAY] = next.subDelaySec
            prefs[A_BOOST] = next.audioBoost
            prefs[DOWNMIX] = next.downmix
            prefs[ORIENT] = next.orientation.name
            prefs[ASPECT] = next.defaultAspect.name
            prefs[DECODER] = next.decoder.name
            prefs[R_MODE] = next.readerMode.name
            prefs[R_KEEP_ON] = next.readerKeepScreenOn
            prefs[R_FILTER] = next.readerFilter.name
            prefs[R_CROP] = next.readerCropBorders
            prefs[ENHANCE] = next.enhanceMode.name
            val font = next.subFont
            if (font == null) {
                prefs.remove(SUB_FONT)
            } else {
                prefs[SUB_FONT] = font
            }
            val lang = next.audioLanguage
            if (lang == null) {
                prefs.remove(A_LANG)
            } else {
                prefs[A_LANG] = lang
            }
        }
    }

    private inline fun <reified T : Enum<T>> String.toEnum(): T? =
        runCatching { enumValueOf<T>(this) }.getOrNull()

    // Preference keys are conventionally SCREAMING; they are immutable key
    // instances, never magic values.
    @Suppress("ObjectPropertyNaming")
    companion object {
        private val RESUME = booleanPreferencesKey("resume")
        private val AUTO_NEXT = booleanPreferencesKey("auto_next")
        private val BACKGROUND = booleanPreferencesKey("background")
        private val PIP = booleanPreferencesKey("pip")
        private val G_SEEK = booleanPreferencesKey("g_seek")
        private val G_BRIGHT = booleanPreferencesKey("g_bright")
        private val G_VOL = booleanPreferencesKey("g_vol")
        private val TAP_SECONDS = longPreferencesKey("tap_seconds")
        private val SUB_SIZE = intPreferencesKey("sub_size")
        private val SUB_COLOR = longPreferencesKey("sub_color")
        private val SUB_OUTLINE = intPreferencesKey("sub_outline")
        private val SUB_POS = floatPreferencesKey("sub_pos")
        private val SUB_FONT = stringPreferencesKey("sub_font")
        private val SUB_DELAY = doublePreferencesKey("sub_delay")
        private val A_BOOST = floatPreferencesKey("a_boost")
        private val A_LANG = stringPreferencesKey("a_lang")
        private val DOWNMIX = booleanPreferencesKey("downmix")
        private val ORIENT = stringPreferencesKey("orient")
        private val ASPECT = stringPreferencesKey("aspect")
        private val DECODER = stringPreferencesKey("decoder")
        private val R_MODE = stringPreferencesKey("r_mode")
        private val R_KEEP_ON = booleanPreferencesKey("r_keep_on")
        private val R_FILTER = stringPreferencesKey("r_filter")
        private val R_CROP = booleanPreferencesKey("r_crop")
        private val ENHANCE = stringPreferencesKey("enhance")
    }
}
