package zhiqiu.app.mk.util

/**
 * 语言切换工具类（跨平台接口）
 * Android 和 iOS 平台使用 expect/actual 实现
 */

/**
 * 设置应用语言
 * @param languageCode 语言代码（zh-CN, en, null表示跟随系统）
 * @return 是否需要 recreate（Android 12及以下返回true）
 */
expect fun setLanguage(languageCode: String?): Boolean

/**
 * 获取当前应用语言
 * @return 当前语言代码（zh-CN, en, null表示跟随系统）
 */
expect fun getCurrentLanguage(): String?

/**
 * 获取语言显示名称
 * @param languageCode 语言代码
 * @return 语言显示名称（如"简体中文"、"English"）
 */
expect fun getLanguageDisplayName(languageCode: String?): String

/**
 * 语言代码常量
 */
object LanguageCodes {
    const val ZH_CN = "zh-CN"
    const val EN = "en"
    const val FOLLOW_SYSTEM = "follow-system"
}
