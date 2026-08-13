package com.pressione.iperteso.services

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Device-level theme mode (matches the web app's `pressione_theme` localStorage key).
 * Values: SYSTEM / LIGHT / DARK — stored in SharedPreferences.
 */
object ThemeManager {

    enum class Mode(val key: String, val label: String) {
        SYSTEM("system", "Sistema"),
        LIGHT("light", "Chiaro"),
        DARK("dark", "Scuro");

        companion object {
            fun fromKey(key: String?): Mode =
                entries.firstOrNull { it.key == key } ?: SYSTEM
        }
    }

    private const val PREFS = "iperteso_theme"
    private const val KEY_MODE = "theme_mode"

    private val _mode = MutableStateFlow(Mode.SYSTEM)
    val mode: StateFlow<Mode> = _mode.asStateFlow()

    private var appContext: Context? = null

    fun init(context: Context) {
        if (appContext != null) return
        val ctx = context.applicationContext
        appContext = ctx
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _mode.value = Mode.fromKey(prefs.getString(KEY_MODE, null))
    }

    fun setMode(newMode: Mode) {
        _mode.value = newMode
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.edit()?.putString(KEY_MODE, newMode.key)?.apply()
    }

    /** Resolve the effective dark flag for the current mode. */
    fun resolveDarkTheme(systemDark: Boolean): Boolean = when (_mode.value) {
        Mode.SYSTEM -> systemDark
        Mode.LIGHT -> false
        Mode.DARK -> true
    }
}
