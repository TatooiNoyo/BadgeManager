package io.github.tatooinoyo.star.badge.utils

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LanguageUtils {
    const val LANGUAGE_AUTO = "auto"
    const val LANGUAGE_ENGLISH = "en"
    const val LANGUAGE_CHINESE = "zh"

    /**
     * 设置应用语言
     */
    fun setLanguage(context: Context, languageCode: String) {
        val locale = when (languageCode) {
            LANGUAGE_ENGLISH -> Locale.ENGLISH
            LANGUAGE_CHINESE -> Locale.CHINESE
            else -> Locale.getDefault()
        }
        
        Locale.setDefault(locale)
        val resources = context.resources
        val configuration = Configuration(resources.configuration)
        configuration.setLocale(locale)
        context.createConfigurationContext(configuration)
        context.resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    /**
     * 获取当前系统语言
     */
    fun getCurrentSystemLanguage(): String {
        return Locale.getDefault().language
    }

    /**
     * 获取当前应用语言设置
     */
    fun getCurrentLanguage(context: Context): String {
        val config = context.resources.configuration
        return config.locales.get(0).language
    }
}