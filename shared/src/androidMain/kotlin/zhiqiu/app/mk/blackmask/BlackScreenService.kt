package zhiqiu.app.mk.blackmask

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import zhiqiu.app.mk.settings.AppSettings
import zhiqiu.app.mk.shared.R

/**
 * Renders a fullscreen black overlay that makes the screen look powered off while
 * the app keeps running behind it.
 *
 * The overlay is a plain [WindowManager] window of type [WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY]
 * (or [WindowManager.LayoutParams.TYPE_PHONE] below API 26). It is pure black: the floating ball lives
 * in its own window (see [FloatingBallService]) so nothing is drawn on top of the black screen.
 *
 * Exit mechanisms (each can be enabled/disabled in the settings):
 *  - tapping the floating ball (see [FloatingBallService])
 *  - tapping with 4 or more fingers
 *  - swiping up
 *  - turning the physical screen off
 *
 * When keep-alive is enabled in the settings the service also runs as a foreground service with a
 * notification and/or holds a partial wake lock.
 */
class BlackScreenService : Service() {

    companion object {
        private const val TAG = "BlackScreenService"
        const val EXTRA_STOP = "stop"
        const val EXTRA_TOGGLE = "toggle"
        private const val MIN_SWIPE_DISTANCE = 200F
        private const val MIN_SWIPE_VELOCITY = -500F
        private const val HINT_TOAST_DELAY_MILLIS = 500L

        private var instance: BlackScreenService? = null

        var isRunning: Boolean
            get() = BlackScreenStatus.isRunning
            set(value) {
                BlackScreenStatus.isRunning = value
            }

        fun start(context: Context): Boolean {
            if (isRunning) return true

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                return false
            }

            val started = try {
                context.startService(Intent(context, BlackScreenService::class.java))
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start service", e)
                false
            }
            // Optimistic flag: the service is about to run (or is already running). Cleared again
            // in onDestroy, or left false if the start was cancelled before onCreate.
            isRunning = started
            return started
        }

        fun stop(context: Context) {
            if (!isRunning) return
            // Clear the flag before stopping so a pending start that never reaches onCreate
            // cannot leave the flag stuck as true.
            isRunning = false
            runCatching {
                context.stopService(Intent(context, BlackScreenService::class.java))
            }.onFailure { Log.e(TAG, "Failed to stop service", it) }
        }

        /** Re-applies the keep-alive settings (foreground service / wake lock) while running. */
        fun reconfigure() {
            instance?.reconfigureInternal()
        }
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private lateinit var gestureDetector: GestureDetector
    private val handler = Handler(Looper.getMainLooper())

    private var wakeLock: PowerManager.WakeLock? = null
    private var foregroundStarted = false

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                // The device was actually locked/turned off, or the user unlocked it again:
                // drop the black screen so the user is never stuck with a black overlay.
                Intent.ACTION_SCREEN_OFF,
                Intent.ACTION_USER_PRESENT -> stopSelf()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        isRunning = true
        registerScreenOffReceiver()
        reconfigureInternal()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getBooleanExtra(EXTRA_STOP, false) == true) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.getBooleanExtra(EXTRA_TOGGLE, false) == true && overlayView != null) {
            // The black screen is already showing: tapping the persistent notification exits it.
            stopSelf()
            return START_NOT_STICKY
        }
        if (overlayView == null) {
            if (!showOverlay()) {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        showExitHint()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        removeOverlay()
        isRunning = false
        instance = null
        runCatching { unregisterReceiver(screenOffReceiver) }
        releaseWakeLock()
        handler.removeCallbacksAndMessages(null)
        // Restore the floating ball once the black screen is gone, if it is enabled.
        if (AppSettings().showFloatingBall) {
            FloatingBallService.setVisible(true)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @Suppress("DEPRECATION")
    private fun showOverlay(): Boolean {
        return try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

            // Legacy flags kept for parity with the reference implementation on older devices;
            // modern immersive mode is applied separately in [applyImmersiveMode].
            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayWindowType(),
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
                PixelFormat.OPAQUE
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutParams.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }

            val view = createOverlayView()
            windowManager.addView(view, layoutParams)
            applyImmersiveMode(view)
            overlayView = view
            // Keep the display completely black: hide the floating ball while the black screen is on.
            FloatingBallService.setVisible(false)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay", e)
            false
        }
    }

    private fun overlayWindowType(): Int = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else -> @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
    }

    private fun createOverlayView(): View {
        val container = OverlayContainer(this)
        container.setBackgroundColor(Color.BLACK)
        container.keepScreenOn = true

        gestureDetector = GestureDetector(this, GestureListener())
        val circleDetector = CircleGestureDetector { stopSelf() }

        container.setOnTouchListener { _, event ->
            if (AppSettings().exitMethodFourFinger && event.pointerCount >= 4) {
                // Four or more simultaneous touches exit the black screen (FakeStandby-style).
                stopSelf()
                return@setOnTouchListener true
            }
            if (AppSettings().exitMethodDrawCircle && circleDetector.onTouch(event)) {
                // Drawing an "O" on the screen exits it too.
                return@setOnTouchListener true
            }
            gestureDetector.onTouchEvent(event)
            true
        }

        return container
    }

    private fun showExitHint() {
        handler.postDelayed({
            Toast.makeText(this, getString(R.string.black_screen_hint), Toast.LENGTH_LONG).show()
        }, HINT_TOAST_DELAY_MILLIS)
    }

    /**
     * The status/navigation bars are separate system windows drawn above a plain
     * [WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY], so translucent/fullscreen flags alone
     * cannot cover them. The system has to be asked to hide them explicitly (immersive mode).
     */
    private fun applyImmersiveMode(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.windowInsetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            view.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        }
    }

    /**
     * Re-applies immersive mode whenever the overlay window regains focus, since the system may
     * temporarily reveal the status/navigation bars (e.g. after a swipe from the edge).
     */
    private inner class OverlayContainer(context: Context) : FrameLayout(context) {
        override fun onWindowFocusChanged(hasFocus: Boolean) {
            super.onWindowFocusChanged(hasFocus)
            if (hasFocus && isRunning) {
                applyImmersiveMode(this)
            }
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (AppSettings().exitMethodSwipeUp &&
                e1 != null && e2.y - e1.y < -MIN_SWIPE_DISTANCE && velocityY < MIN_SWIPE_VELOCITY
            ) {
                stopSelf()
                return true
            }
            return false
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            runCatching { windowManager.removeViewImmediate(it) }
            overlayView = null
        }
    }

    private fun registerScreenOffReceiver() {
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        filter.addAction(Intent.ACTION_USER_PRESENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Protected system broadcasts; NOT_EXPORTED is sufficient and avoids exposing the
            // receiver to other apps.
            registerReceiver(screenOffReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenOffReceiver, filter)
        }
    }

    /** Applies or removes the keep-alive options based on the current settings. */
    private fun reconfigureInternal() {
        val settings = AppSettings()
        if (settings.keepAliveForeground && !foregroundStarted) {
            KeepAlive.startForeground(
                this,
                KeepAlive.BLACK_SCREEN_NOTIFICATION_ID,
                KeepAlive.blackScreenNotification(this)
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
}
