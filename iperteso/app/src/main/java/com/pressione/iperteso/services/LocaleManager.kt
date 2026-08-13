package com.pressione.iperteso.services

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Manages in-app language (it / en) with persistence and activity restart.
 *
 * The locale is applied:
 *  1. On every launch/restart via [wrap] in `attachBaseContext`, so the stored
 *     language is active from the very first composition.
 *  2. On switch via [setLanguage], which persists the choice and recreates the
 *     activity (the "con riavvio" behaviour).
 */
object LocaleManager {

    private const val PREFS_NAME = "iperteso_locale"
    private const val KEY_LANGUAGE = "language"
    const val LANG_ITALIAN = "it"
    const val LANG_ENGLISH = "en"

    private val supportedLanguages = setOf(LANG_ITALIAN, LANG_ENGLISH)

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getLanguage(context: Context): String =
        prefs(context).getString(KEY_LANGUAGE, LANG_ITALIAN) ?: LANG_ITALIAN

    fun getLocale(context: Context): Locale =
        Locale(getLanguage(context))

    /**
     * Wraps the base context with the stored locale so resources resolve
     * correctly even before any activity recreation.
     */
    fun wrap(context: Context): Context {
        val lang = getLanguage(context)
        if (lang !in supportedLanguages) return context
        val locale = Locale(lang)
        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        return context.createConfigurationContext(config)
    }

    /**
     * Switches the language and restarts the activity to apply it everywhere.
     */
    fun setLanguage(activity: Activity, language: String) {
        if (language !in supportedLanguages) return
        if (language == getLanguage(activity)) return
        prefs(activity).edit().putString(KEY_LANGUAGE, language).apply()
        activity.recreate()
    }
}
