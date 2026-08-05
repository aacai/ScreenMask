package zhiqiu.app.mk.settings

/** 持久化偏好：悬浮球开关、退出方式开关、保活开关。 */
class AppSettings(private val store: KeyValueStore = createKeyValueStore()) {

    /** 是否显示可拖动的悬浮球，点击悬浮球开启黑屏。默认开启。 */
    var showFloatingBall: Boolean
        get() = store.getBoolean(KEY_SHOW_FLOATING_BALL, true)
        set(value) = store.putBoolean(KEY_SHOW_FLOATING_BALL, value)

    /** 点击悬浮球时是否先弹窗确认再开启黑屏。默认开启。 */
    var ballClickConfirm: Boolean
        get() = store.getBoolean(KEY_BALL_CLICK_CONFIRM, true)
        set(value) = store.putBoolean(KEY_BALL_CLICK_CONFIRM, value)

    /** 黑屏退出方式：4 根及以上手指同时触摸屏幕。默认开启。 */
    var exitMethodFourFinger: Boolean
        get() = store.getBoolean(KEY_EXIT_METHOD_FOUR_FINGER, true)
        set(value) = store.putBoolean(KEY_EXIT_METHOD_FOUR_FINGER, value)

    /** 黑屏退出方式：从屏幕底部向上滑动。默认开启。 */
    var exitMethodSwipeUp: Boolean
        get() = store.getBoolean(KEY_EXIT_METHOD_SWIPE_UP, true)
        set(value) = store.putBoolean(KEY_EXIT_METHOD_SWIPE_UP, value)

    /** 黑屏退出方式：在屏幕上画一个圆圈（O）。默认开启。 */
    var exitMethodDrawCircle: Boolean
        get() = store.getBoolean(KEY_EXIT_METHOD_DRAW_CIRCLE, true)
        set(value) = store.putBoolean(KEY_EXIT_METHOD_DRAW_CIRCLE, value)

    /** 以带常驻通知的前台服务运行，防止被杀掉（Android 上最可靠）。默认关闭。 */
    var keepAliveForeground: Boolean
        get() = store.getBoolean(KEY_KEEP_ALIVE_FOREGROUND, false)
        set(value) = store.putBoolean(KEY_KEEP_ALIVE_FOREGROUND, value)

    /** 保持 CPU 唤醒，防止系统休眠后停止服务。默认关闭。 */
    var keepAliveWakeLock: Boolean
        get() = store.getBoolean(KEY_KEEP_ALIVE_WAKE_LOCK, false)
        set(value) = store.putBoolean(KEY_KEEP_ALIVE_WAKE_LOCK, value)

    private companion object {
        const val KEY_SHOW_FLOATING_BALL = "mask.show_floating_ball"
        const val KEY_BALL_CLICK_CONFIRM = "mask.ball_click_confirm"
        const val KEY_EXIT_METHOD_FOUR_FINGER = "mask.exit_method_four_finger"
        const val KEY_EXIT_METHOD_SWIPE_UP = "mask.exit_method_swipe_up"
        const val KEY_EXIT_METHOD_DRAW_CIRCLE = "mask.exit_method_draw_circle"
        const val KEY_KEEP_ALIVE_FOREGROUND = "mask.keep_alive_foreground"
        const val KEY_KEEP_ALIVE_WAKE_LOCK = "mask.keep_alive_wake_lock"
    }
}
