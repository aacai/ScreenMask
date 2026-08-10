package zhiqiu.app.mk

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

private var _applicationContext: android.content.Context? = null

fun initPlatformContext(context: android.content.Context) {
    _applicationContext = context.applicationContext
}

fun getApplicationContext(): android.content.Context? = _applicationContext