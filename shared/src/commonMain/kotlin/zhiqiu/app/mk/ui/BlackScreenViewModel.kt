package zhiqiu.app.mk.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import zhiqiu.app.mk.blackmask.BlackScreenController
import zhiqiu.app.mk.blackmask.BlackScreenRequest
import zhiqiu.app.mk.blackmask.BlackScreenStatus
import zhiqiu.app.mk.settings.AppSettings

/**
 * Shared view model for the black screen feature, used by both the home and the settings screen.
 *
 * The running state is reactive (backed by [BlackScreenStatus]), so it stays in sync even when the
 * black screen is toggled from outside the UI, such as tapping the floating ball.
 */
class BlackScreenViewModel(
    private val controller: BlackScreenController,
    private val startFailedText: String,
    private val overlayPermissionText: String,
) : ScreenModel {

    /** Whether the underlying platform can show the black screen overlay at all. */
    val supported: Boolean = controller.supported

    /** Whether the black screen is currently showing; reactive to external toggles (e.g. the ball). */
    val isRunning: Boolean
        get() = BlackScreenStatus.isRunning

    var showConfirmDialog by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var pendingStart = false

    /** Called on lifecycle resume: reconcile the ball, handle a ball-tap confirm, and auto-start after a permission grant. */
    fun onResume() {
        controller.setFloatingBallVisible(AppSettings().showFloatingBall)
        if (BlackScreenRequest.pendingBallConfirm) {
            BlackScreenRequest.pendingBallConfirm = false
            onToggle(true)
        }
        if (pendingStart) {
            pendingStart = false
            if (controller.hasOverlayPermission()) {
                start()
            } else {
                errorMessage = overlayPermissionText
            }
        }
    }

    /** Toggles the black screen from a switch/button state. */
    fun onToggle(checked: Boolean) {
        errorMessage = null
        if (checked) {
            showConfirmDialog = true
        } else {
            controller.stop()
        }
    }

    /** Confirms the start dialog, requesting the overlay permission first if needed. */
    fun onConfirmStart() {
        showConfirmDialog = false
        when {
            !controller.needsOverlayPermission -> start()
            controller.hasOverlayPermission() -> start()
            else -> {
                pendingStart = true
                controller.requestOverlayPermission()
            }
        }
    }

    fun dismissDialog() {
        showConfirmDialog = false
    }

    fun start() {
        errorMessage = null
        val started = controller.start()
        if (!started) {
            errorMessage = startFailedText
        }
    }

    /** Shows or hides the independent floating ball window (e.g. from the settings toggle). */
    fun setFloatingBallVisible(visible: Boolean) {
        controller.setFloatingBallVisible(visible)
    }

    /** Re-applies the keep-alive options after they have been changed in the settings. */
    fun applyKeepAlive() {
        controller.applyKeepAlive()
    }
}
