package zhiqiu.app.mk.util

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import zhiqiu.app.mk.getApplicationContext
import java.util.Locale

/**
 * Android 平台 LanguageHelper actual 实现
 * 直接在这里实现，不依赖 androidApp 模块的 LanguageHelper
 */

/**
 * 获取系统默认 Locale
 */
private fun getSystemLocale(): Locale {
    return LocaleList.getDefault()[0]
}

/**
 * 根据语言代码获取 Locale
 */
private fun getLocaleFromLanguageCode(languageCode: String?): Locale {
    return when (languageCode) {
        "zh-CN", "zh_CN", "zh" -> Locale.SIMPLIFIED_CHINESE
        "en" -> Locale.ENGLISH
        else -> getSystemLocale()
    }
}

actual fun setLanguage(languageCode: String?): Boolean {
    val context = getApplicationContext() ?: return false

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Android 13+ 使用 LocaleManager
        val localeManager = context.getSystemService(LocaleManager::class.java)
        if (languageCode.isNullOrEmpty() || languageCode == "follow-system") {
            localeManager?.applicationLocales = LocaleList.getEmptyLocaleList()
        } else {
            val locale = getLocaleFromLanguageCode(languageCode)
            localeManager?.applicationLocales = LocaleList.forLanguageTags(locale.toLanguageTag())
        }
        false // 不需要 recreate
    } else {
        // Android 12及以下需要 recreate
        true
    }
}

actual fun getCurrentLanguage(): String? {
    val context = getApplicationContext() ?: return null

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val localeManager = context.getSystemService(LocaleManager::class.java)
        val locales = localeManager?.applicationLocales
        if (locales == null || locales.isEmpty) {
            null // 跟随系统
        } else {
            val locale = locales[0]
            when {
                locale.language == "zh" -> "zh-CN"
                locale.language == "en" -> "en"
                else -> locale.toLanguageTag()
            }
        }
    } else {
        val locale = context.resources.configuration.locale
        when {
            locale == Locale.SIMPLIFIED_CHINESE -> "zh-CN"
            locale == Locale.ENGLISH -> "en"
            locale == getSystemLocale() -> null
            else -> locale.toLanguageTag()
        }
    }
}

actual fun getLanguageDisplayName(languageCode: String?): String {
    return when (languageCode) {
        "zh-CN" -> "简体中文"
        "en" -> "English"
        null, "follow-system" -> "跟随系统"
        else -> languageCode
    }
}
