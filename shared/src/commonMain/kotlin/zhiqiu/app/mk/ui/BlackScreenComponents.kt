package zhiqiu.app.mk.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import org.jetbrains.compose.resources.stringResource
import screenmask.shared.generated.resources.Res
import screenmask.shared.generated.resources.black_screen_confirm_message
import screenmask.shared.generated.resources.black_screen_confirm_title
import screenmask.shared.generated.resources.cancel
import screenmask.shared.generated.resources.confirm
import screenmask.shared.generated.resources.overlay_permission_required
import screenmask.shared.generated.resources.start_failed
import screenmask.shared.generated.resources.unsupported_platform
import zhiqiu.app.mk.blackmask.BlackScreenRequest
import zhiqiu.app.mk.blackmask.getBlackScreenController

/**
 * Creates the [BlackScreenViewModel] for the current screen via Voyager's [rememberScreenModel]
 * and wires it to the lifecycle (reconcile on resume, auto-start after returning from the
 * overlay-permission screen).
 */
@Composable
fun Screen.rememberBlackScreenViewModel(): BlackScreenViewModel {
    val startFailedText = stringResource(Res.string.start_failed)
    val overlayPermissionText = stringResource(Res.string.overlay_permission_required)

    val model = rememberScreenModel {
        BlackScreenViewModel(getBlackScreenController(), startFailedText, overlayPermissionText)
    }

    // The floating ball (a system overlay) requests confirmation by launching the activity with
    // EXTRA_CONFIRM_START; observe the reactive flag so the dialog appears even when the activity
    // was already in the foreground and never went through a pause/resume cycle.
    LaunchedEffect(BlackScreenRequest.pendingBallConfirm) {
        if (BlackScreenRequest.pendingBallConfirm) {
            BlackScreenRequest.pendingBallConfirm = false
            model.onToggle(true)
        }
    }

    LifecycleResumeEffect(Unit) {
        model.onResume()
        onPauseOrDispose { }
    }

    return model
}

/** Confirmation dialog shown before starting the black screen overlay. */
@Composable
fun BlackScreenConfirmDialog(model: BlackScreenViewModel) {
    if (!model.showConfirmDialog) return

    AlertDialog(
        onDismissRequest = model::dismissDialog,
        title = { Text(stringResource(Res.string.black_screen_confirm_title)) },
        text = { Text(stringResource(Res.string.black_screen_confirm_message)) },
        confirmButton = {
            TextButton(onClick = model::onConfirmStart) {
                Text(stringResource(Res.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = model::dismissDialog) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

/** Shown when the underlying platform cannot display the black screen overlay. */
@Composable
fun UnsupportedNotice() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()
            .padding(16.dp)
    ) {
        Text(stringResource(Res.string.unsupported_platform))
    }
}
