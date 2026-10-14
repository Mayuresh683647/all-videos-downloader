package com.example.videobackup

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.BufferedReader
import java.io.InputStreamReader

class DownloadUploadService : Service() {
    companion object {
        const val ACTION_START_SINGLE = "com.example.videobackup.START_SINGLE"
        const val ACTION_START_BATCH = "com.example.videobackup.START_BATCH"
        const val ACTION_CANCEL = "com.example.videobackup.CANCEL"

        const val EXTRA_LINK = "extra_link"
        const val EXTRA_TEXT_URI = "extra_text_uri"

        const val CHANNEL_ID = "video_backup_channel"
        const val NOTIFICATION_ID = 1
    }

    private val settings by lazy { SettingsRepository(applicationContext) }
    private val isCancelled = java.util.concurrent.atomic.AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SINGLE -> {
                val link = intent.getStringExtra(EXTRA_LINK)
                if (!link.isNullOrBlank()) {
                    startForeground(NOTIFICATION_ID, buildNotification("Preparing: $link"))
                    Thread {
                        processSingle(link)
                    }.start()
                }
            }
            ACTION_START_BATCH -> {
                val uriString = intent.getStringExtra(EXTRA_TEXT_URI)
                if (!uriString.isNullOrBlank()) {
                    val uri = Uri.parse(uriString)
                    startForeground(NOTIFICATION_ID, buildNotification("Reading batch file"))
                    Thread {
                        processBatch(uri)
                    }.start()
                }
            }
            ACTION_CANCEL -> {
                isCancelled.set(true)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun processSingle(link: String) {
        try {
            val runner = YtDlpRunner(applicationContext)
            val file = runner.download(link)
            val uploader = TelegramUploader(settings)
            uploader.uploadFile(file)
            updateNotification("Completed: $link")
        } catch (e: Exception) {
            updateNotification("Failed: ${e.message ?: "unknown error"}")
        } finally {
            stopSelf()
        }
    }

    private fun processBatch(uri: Uri) {
        val links = mutableListOf<String>()
        contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input)).useLines { lines ->
                lines.forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty()) links.add(trimmed)
                }
            }
        }

        if (links.isEmpty()) {
            updateNotification("No links found in batch file")
            stopSelf()
            return
        }

        val runner = YtDlpRunner(applicationContext)
        val uploader = TelegramUploader(settings)

        for ((index, link) in links.withIndex()) {
            if (isCancelled.get()) break
            updateNotification("[$index/${links.size}] Downloading: $link")
            try {
                val file = runner.download(link)
                updateNotification("[$index/${links.size}] Uploading: ${file.name}")
                uploader.uploadFile(file)
                file.delete()
            } catch (e: Exception) {
                updateNotification("[$index/${links.size}] Failed: ${e.message ?: "unknown error"}")
            }
        }

        updateNotification("Batch finished")
        stopSelf()
    }

    private fun buildNotification(content: String): Notification {
        val cancelIntent = Intent(this, DownloadUploadService::class.java).apply {
            action = ACTION_CANCEL
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            0,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Video Backup")
            .setContentText(content)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, "Cancel", cancelPendingIntent)
            .build()
    }

    private fun updateNotification(content: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, buildNotification(content))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Video Backup",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
