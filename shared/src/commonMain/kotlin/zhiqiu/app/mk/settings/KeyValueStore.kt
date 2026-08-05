package zhiqiu.app.mk.settings

/**
 * 最小键值存储抽象，取代第三方偏好库以规避 KMP 原生目标上的 API 不一致。
 * 仅覆盖本应用用到的六类设置（悬浮球 / 退出方式 / 保活）。
 */
interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun remove(key: String)
}

internal expect fun createKeyValueStore(): KeyValueStore
