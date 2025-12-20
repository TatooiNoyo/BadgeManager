package io.github.tatooinoyo.star.badge.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import java.util.Locale

class LanguageManager private constructor(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "language_prefs"
        private const val KEY_LANGUAGE = "selected_language"
        private var INSTANCE: LanguageManager? = null
        
        @Synchronized
        fun getInstance(context: Context): LanguageManager {
            if (INSTANCE == null) {
                INSTANCE = LanguageManager(context.applicationContext)
            }
            return INSTANCE!!
        }
    }
    
    fun setLanguage(languageCode: String) {
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
        updateLanguage(context, languageCode)
    }
    
    fun getCurrentLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, LanguageUtils.LANGUAGE_AUTO) ?: LanguageUtils.LANGUAGE_AUTO
    }
    
    fun updateLanguage(context: Context, languageCode: String) {
        val locale = when (languageCode) {
            LanguageUtils.LANGUAGE_ENGLISH -> Locale.ENGLISH
            LanguageUtils.LANGUAGE_CHINESE -> Locale.CHINESE
            LanguageUtils.LANGUAGE_CHINESE_TRADITIONAL -> Locale.TRADITIONAL_CHINESE
            else -> Locale.getDefault()
        }
        
        Locale.setDefault(locale)
        val resources = context.resources
        val configuration = Configuration(resources.configuration)
        configuration.setLocale(locale)
        context.createConfigurationContext(configuration)
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }
    
    fun applyLanguage(context: Context) {
        val languageCode = getCurrentLanguage()
        updateLanguage(context, languageCode)
    }
}