package zhiqiu.app.mk.blackmask

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import zhiqiu.app.mk.settings.AppSettings

internal var appContext: Context? = null

/**
 * Initializes the black screen feature with the application context.
 *
 * Must be called once from the Android entry point (e.g. [android.app.Activity.onCreate])
 * before [getBlackScreenController] is used. Also brings up the floating ball window when the
 * "show floating ball" setting is enabled.
 */
fun initBlackScreen(context: Context) {
    appContext = context.applicationContext
    if (AppSettings().showFloatingBall) {
        FloatingBallService.start(context.applicationContext)
    }
}

actual fun getBlackScreenController(): BlackScreenController = AndroidBlackScreenController

private object AndroidBlackScreenController : BlackScreenController {

    override val supported: Boolean = true

    override val isRunning: Boolean
        get() = BlackScreenService.isRunning

    override val needsOverlayPermission: Boolean = true

    override fun hasOverlayPermission(): Boolean =
        appContext?.let { Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(it) } ?: false

    override fun requestOverlayPermission() {
        val context = appContext ?: return
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    override fun start(): Boolean {
        val context = appContext ?: return false
        return BlackScreenService.start(context)
    }

    override fun stop() {
        val context = appContext ?: return
        BlackScreenService.stop(context)
    }

    override fun setFloatingBallVisible(visible: Boolean) {
        val context = appContext ?: return
        if (visible) {
            FloatingBallService.start(context)
        } else {
            FloatingBallService.stop(context)
        }
    }

    override fun applyKeepAlive() {
        FloatingBallService.reconfigure()
        if (BlackScreenService.isRunning) {
            BlackScreenService.reconfigure()
        }
    }
}
