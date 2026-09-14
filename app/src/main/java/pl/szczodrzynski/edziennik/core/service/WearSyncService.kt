/*
 * Copyright (c) Kuba Szczodrzyński 2024.
 *
 * Keeps the local-network sync server alive so the watch can fetch data
 * at any time, even when the app is in the background.
 */

package pl.szczodrzynski.edziennik.core.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R

class WearSyncService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 2

        fun start(context: Context) {
            val intent = Intent(context, WearSyncService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }

    private val app: App
        get() = application as App

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(app.notificationManager.sync.id, buildNotification())
        app.wearSyncManager.lanServer.start()
        return START_STICKY
    }

    private fun buildNotification() = NotificationCompat.Builder(this, app.notificationManager.sync.key)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_wear_sync))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
}
