package zhiqiu.app.mk.blackmask

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import zhiqiu.app.mk.shared.R

/**
 * Shared helpers for the keep-alive options (foreground service + wake lock).
 *
 * The floating ball service and the black screen service both read the settings in
 * [zhiqiu.app.mk.settings.AppSettings] and use these helpers to promote themselves to a
 * foreground service and/or hold a partial wake lock.
 */
internal object KeepAlive {

    const val KEEP_ALIVE_CHANNEL_ID = "keep_alive"
    const val BLACK_SCREEN_CHANNEL_ID = "black_screen"
    const val KEEP_ALIVE_NOTIFICATION_ID = 1
    const val BLACK_SCREEN_NOTIFICATION_ID = 2

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                KEEP_ALIVE_CHANNEL_ID,
                context.getString(R.string.keep_alive_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                BLACK_SCREEN_CHANNEL_ID,
                context.getString(R.string.black_screen_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
        )
    }

    fun keepAliveNotification(context: Context): Notification {
        // Tapping the persistent notification opens the app.
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pending = PendingIntent.getActivity(
            context,
            0,
            launch,
            PendingIntent.FLAG_IMMUTABLE
        )
        return buildNotification(
            context,
            KEEP_ALIVE_CHANNEL_ID,
            context.getString(R.string.keep_alive_notification_title),
            context.getString(R.string.keep_alive_notification_text),
            pending
        )
    }

    fun blackScreenNotification(context: Context): Notification {
        // Tapping the black screen notification exits the black screen.
        val stop = PendingIntent.getService(
            context,
            0,
            Intent(context, BlackScreenService::class.java)
                .putExtra(BlackScreenService.EXTRA_STOP, true),
            PendingIntent.FLAG_IMMUTABLE
        )
        return buildNotification(
            context,
            BLACK_SCREEN_CHANNEL_ID,
            context.getString(R.string.black_screen_notification_title),
            context.getString(R.string.black_screen_notification_text),
            stop
        )
    }

    private fun buildNotification(
        context: Context,
        channelId: String,
        title: String,
        text: String,
        contentIntent: PendingIntent
    ): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }
        return builder
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .build()
    }

    /** Promotes the given service to a foreground service if possible. */
    fun startForeground(service: Service, notificationId: Int, notification: Notification) {
        ensureChannels(service)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            service.startForeground(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            @Suppress("DEPRECATION")
            service.startForeground(notificationId, notification)
        }
    }

    /** Acquires and returns a partial wake lock. Callers must release it. */
    fun acquireWakeLock(context: Context): PowerManager.WakeLock {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ScreenMask:keepalive")
            .apply { acquire() }
    }
}
