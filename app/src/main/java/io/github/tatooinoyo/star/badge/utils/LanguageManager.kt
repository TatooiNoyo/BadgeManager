package io.github.tatooinoyo.star.badge.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import java.util.Locale

class LanguageManager private constructor(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val TAG = "LanguageManager"
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
        Log.d(TAG, "setLanguage: $languageCode")
        // 使用 commit() 而不是 apply() 确保同步保存，在重启前完成
        val success = prefs.edit().putString(KEY_LANGUAGE, languageCode).commit()
        Log.d(TAG, "setLanguage saved: $success, value: ${prefs.getString(KEY_LANGUAGE, "NOT_FOUND")}")
        updateLanguage(context, languageCode)
    }
    
    fun getCurrentLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, LanguageUtils.LANGUAGE_AUTO) ?: LanguageUtils.LANGUAGE_AUTO
    }
    
    fun getLocale(languageCode: String): Locale {
        return when (languageCode) {
            LanguageUtils.LANGUAGE_ENGLISH -> Locale.ENGLISH
            LanguageUtils.LANGUAGE_CHINESE -> Locale.SIMPLIFIED_CHINESE
            LanguageUtils.LANGUAGE_CHINESE_TRADITIONAL -> Locale.TRADITIONAL_CHINESE
            else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.resources.configuration.locales[0]
            } else {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale
            }
        }
    }
    
    fun wrapContext(context: Context): Context {
        // 使用 applicationContext 确保可以访问 SharedPreferences
        val appContext = context.applicationContext ?: context
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val languageCode = prefs.getString(KEY_LANGUAGE, LanguageUtils.LANGUAGE_AUTO) 
            ?: LanguageUtils.LANGUAGE_AUTO
        Log.d(TAG, "wrapContext: languageCode=$languageCode")
        val locale = getLocale(languageCode)
        Log.d(TAG, "wrapContext: locale=$locale")
        
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return context.createConfigurationContext(config)
    }
    
    fun updateLanguage(context: Context, languageCode: String) {
        val locale = getLocale(languageCode)
        Locale.setDefault(locale)
        
        val resources = context.resources
        val configuration = Configuration(resources.configuration)
        configuration.setLocale(locale)
        
        @Suppress("DEPRECATION")
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }
    
    fun applyLanguage(context: Context) {
        val languageCode = getCurrentLanguage()
        updateLanguage(context, languageCode)
    }
}