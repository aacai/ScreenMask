package zhiqiu.app.mk.blackmask

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import zhiqiu.app.mk.settings.AppSettings

/**
 * Hosts the floating ball in its own always-on-top overlay window, independent of the black screen.
 *
 * The ball is draggable (snapping to the nearest edge on release) and a tap starts the black screen.
 *
 * While the black screen is active the ball is hidden so the display stays completely black
 * (no burn-in risk). It reappears when the black screen exits.
 */
class FloatingBallService : Service() {

    companion object {
        private const val TAG = "FloatingBallService"
        private const val BALL_SIZE_DP = 56f

        private var instance: FloatingBallService? = null

        fun start(context: Context) {
            val intent = Intent(context, FloatingBallService::class.java)
            runCatching { context.startService(intent) }
                .onFailure { Log.e(TAG, "Failed to start service", it) }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, FloatingBallService::class.java)) }
                .onFailure { Log.e(TAG, "Failed to stop service", it) }
        }

        /**
         * Shows or hides the ball window, e.g. the black screen hides it while active and shows
         * it again on exit.
         */
        fun setVisible(visible: Boolean) {
            instance?.setVisibleInternal(visible)
        }

        /** Re-applies the keep-alive settings (foreground service / wake lock). */
        fun reconfigure() {
            instance?.reconfigureInternal()
        }
    }

    private var windowManager: WindowManager? = null
    private var ballView: FloatingBallView? = null
    private var ballParams: WindowManager.LayoutParams? = null
    private var ballAdded = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var foregroundStarted = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        showBall()
        reconfigureInternal()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (ballView == null) {
            showBall()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        instance = null
        releaseWakeLock()
        removeBall()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showBall() {
        try {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            windowManager = wm

            val size = dp(BALL_SIZE_DP).toInt()
            val params = WindowManager.LayoutParams(
                size,
                size,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP or Gravity.START

            val (screenWidth, screenHeight) = getScreenSize(wm)
            // Start at the right edge, vertically centered.
            params.x = screenWidth - size
            params.y = (screenHeight - size) / 2

            val view = FloatingBallView(
                context = this,
                windowManager = wm,
                layoutParams = params,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                onTap = ::toggleBlackScreen,
            )
            ballView = view
            ballParams = params
            if (BlackScreenService.isRunning) {
                // Black screen is active: keep the ball off-screen until it exits.
                ballAdded = false
            } else {
                wm.addView(view, params)
                ballAdded = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show floating ball", e)
            stopSelf()
        }
    }

    private fun toggleBlackScreen() {
        if (BlackScreenService.isRunning) {
            BlackScreenService.stop(this)
            return
        }
        if (AppSettings().ballClickConfirm) {
            openAppForConfirm()
        } else if (BlackScreenService.start(this)) {
            // The black screen hides the ball via BlackScreenService.showOverlay; hide it here too
            // in case the service was already running and skipped that path.
            setVisibleInternal(false)
        }
    }

    /**
     * The ball is a system overlay and cannot show Compose dialogs, so when confirmation is enabled
     * we bring the app to the foreground and let the home screen show the same confirm dialog as the
     * "start" button. Falls back to starting directly if the app cannot be brought up.
     */
    private fun openAppForConfirm() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (intent == null) {
            if (BlackScreenService.start(this)) setVisibleInternal(false)
            return
        }
        intent.putExtra(BlackScreenRequest.EXTRA_CONFIRM_START, true)
        val launched = runCatching { startActivity(intent) }.isSuccess
        if (!launched && BlackScreenService.start(this)) {
            setVisibleInternal(false)
        }
    }

    private fun setVisibleInternal(visible: Boolean) {
        val wm = windowManager ?: return
        val view = ballView ?: return
        val params = ballParams ?: return
        if (visible && !ballAdded) {
            runCatching { wm.addView(view, params) }
            ballAdded = true
        } else if (!visible && ballAdded) {
            runCatching { wm.removeViewImmediate(view) }
            ballAdded = false
        }
    }

    private fun removeBall() {
        ballView?.let {
            runCatching { windowManager?.removeViewImmediate(it) }
            ballView = null
            ballAdded = false
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    /** Applies or removes the keep-alive options based on the current settings. */
    private fun reconfigureInternal() {
        val settings = AppSettings()
        if (settings.keepAliveForeground && !foregroundStarted) {
            KeepAlive.startForeground(
                this,
                KeepAlive.KEEP_ALIVE_NOTIFICATION_ID,
                KeepAlive.keepAliveNotification(this)
            )
            foregroundStarted = true
        } else if (!settings.keepAliveForeground && foregroundStarted) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }
        if (settings.keepAliveWakeLock && wakeLock == null) {
            wakeLock = KeepAlive.acquireWakeLock(this)
        } else if (!settings.keepAliveWakeLock) {
            releaseWakeLock()
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            runCatching { if (it.isHeld) it.release() }
        }
        wakeLock = null
    }

    private fun getScreenSize(wm: WindowManager): Pair<Int, Int> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds: Rect = wm.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            @Suppress("DEPRECATION")
            val size = Point()
            wm.defaultDisplay.getRealSize(size)
            size.x to size.y
        }
}
