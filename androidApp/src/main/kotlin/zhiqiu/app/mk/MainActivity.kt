package zhiqiu.app.mk

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import zhiqiu.app.mk.blackmask.BlackScreenRequest
import zhiqiu.app.mk.blackmask.initBlackScreen
import zhiqiu.app.mk.settings.setAndroidContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        requestNotificationPermission()
        setAndroidContext(applicationContext)
        initBlackScreen(applicationContext)
        handleConfirmRequest(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleConfirmRequest(intent)
    }

    /**
     * The floating ball (a system overlay) asks for confirmation by launching this activity with
     * [BlackScreenRequest.EXTRA_CONFIRM_START]; surface it to the view model on the next resume.
     */
    private fun handleConfirmRequest(intent: Intent?) {
        if (intent?.getBooleanExtra(BlackScreenRequest.EXTRA_CONFIRM_START, false) == true) {
            BlackScreenRequest.pendingBallConfirm = true
        }
    }

    /**
     * Foreground keep-alive shows a persistent notification; ask for the notification permission
     * once so it can be displayed on Android 13+.
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
