package zhiqiu.app.mk.blackmask

/**
 * iOS has no supported mechanism for showing a system-wide overlay that mimics a powered-off
 * screen, so the black screen feature is disabled on this platform.
 */
actual fun getBlackScreenController(): BlackScreenController = IosBlackScreenController

private object IosBlackScreenController : BlackScreenController {

    override val supported: Boolean = false

    override val isRunning: Boolean = false

    override val needsOverlayPermission: Boolean = false

    override fun hasOverlayPermission(): Boolean = true

    override fun requestOverlayPermission() = Unit

    override fun start(): Boolean = false

    override fun stop() = Unit

    override fun setFloatingBallVisible(visible: Boolean) = Unit

    override fun applyKeepAlive() = Unit
}
