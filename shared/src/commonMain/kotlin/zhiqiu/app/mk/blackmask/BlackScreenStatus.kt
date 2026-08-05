package zhiqiu.app.mk.blackmask

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Global, observable black-screen running state.
 *
 * Updated directly by the platform implementation (e.g. [android.app.Service] lifecycle callbacks),
 * so the UI stays in sync even when the black screen is toggled from outside the app UI, such as
 * tapping the floating ball (which never pauses the activity, so lifecycle resume is not enough).
 * Compose screens read this reactively.
 */
object BlackScreenStatus {
    var isRunning by mutableStateOf(false)
        internal set
}

/**
 * One-way requests from platform overlays (e.g. the floating ball) to the app UI.
 *
 * The floating ball lives in a system overlay window, so it cannot show Compose dialogs directly.
 * When a confirmation is required, the ball brings the app to the foreground (with
 * [EXTRA_CONFIRM_START] on the launch intent) and sets [pendingBallConfirm]; the view model picks
 * it up on the next lifecycle resume and shows the same confirm dialog as the home button.
 */
object BlackScreenRequest {
    const val EXTRA_CONFIRM_START = "zhiqiu.app.mk.extra_confirm_black_screen"

    /** Set by [android.app.Activity] when the ball's launch intent carries [EXTRA_CONFIRM_START]. */
    var pendingBallConfirm by mutableStateOf(false)
}
