package zhiqiu.app.mk.util

import platform.Foundation.NSUserDefaults

/**
 * iOS 平台 LanguageHelper actual 实现
 * 使用 NSUserDefaults 的 AppleLanguages 键，应用重启后生效
 */

actual fun setLanguage(languageCode: String?): Boolean {
    val defaults = NSUserDefaults.standardUserDefaults
    when (languageCode) {
        null, "follow-system" -> defaults.removeObjectForKey("AppleLanguages")
        "zh-CN" -> defaults.setObject(listOf("zh-Hans"), "AppleLanguages")
        "en" -> defaults.setObject(listOf("en"), "AppleLanguages")
        else -> defaults.setObject(listOf(languageCode), "AppleLanguages")
    }
    // iOS 需要重启应用才能生效
    return false
}

actual fun getCurrentLanguage(): String? {
    val first = NSUserDefaults.standardUserDefaults
        .stringArrayForKey("AppleLanguages")
        ?.firstOrNull() as? String
    return when (first) {
        null -> null // 跟随系统
        "zh-Hans", "zh-Hant", "zh" -> "zh-CN"
        "en" -> "en"
        else -> first
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
