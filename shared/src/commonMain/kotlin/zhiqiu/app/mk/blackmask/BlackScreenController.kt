package zhiqiu.app.mk.blackmask

/**
 * Platform abstraction for the black screen overlay feature.
 *
 * The Android implementation shows a fullscreen black overlay via a native
 * [android.app.Service] so the screen looks powered off while the app keeps
 * running behind it. Other platforms currently have no implementation and
 * report [supported] == false.
 */
interface BlackScreenController {

    /** Whether the underlying platform can show the black screen overlay. */
    val supported: Boolean

    /** Whether the black screen overlay is currently showing. */
    val isRunning: Boolean

    /** Whether the platform requires the overlay permission to be granted first. */
    val needsOverlayPermission: Boolean

    /** Whether the overlay permission (if required) is currently granted. */
    fun hasOverlayPermission(): Boolean

    /** Opens the system settings screen where the overlay permission can be granted. */
    fun requestOverlayPermission()

    /**
     * Starts the black screen overlay.
     *
     * The platform shows the exit-method hint itself; no text needs to be passed in.
     *
     * @return true if the overlay started successfully, false otherwise
     *         (e.g. permission missing or platform unsupported).
     */
    fun start(): Boolean

    /** Stops the black screen overlay if it is running. */
    fun stop()

    /**
     * Shows or hides the independent floating ball window.
     *
     * The ball is its own overlay window and is not tied to the black screen's lifecycle:
     * it stays visible while the black screen is on so tapping it toggles the screen off.
     */
    fun setFloatingBallVisible(visible: Boolean)

    /**
     * Re-applies the keep-alive options (foreground service, wake lock) to the running services.
     * Called after the settings have been changed.
     */
    fun applyKeepAlive()
}

expect fun getBlackScreenController(): BlackScreenController
