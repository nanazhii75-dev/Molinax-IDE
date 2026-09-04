package com.molinax.medialibrary

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class DownloadService : Service() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        const val EXTRA_URL = "extra_url"
        private const val YT_DLP_PATH = "/data/data/com.termux/files/usr/bin/yt-dlp"
        private const val CHANNEL_ID = "molinax_download_channel"
        private const val NOTIF_ID = 4242
        private val PROGRESS_REGEX = Regex("""\[download]\s+(\d{1,3}\.\d)%""")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra(EXTRA_URL)
        if (url.isNullOrBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("Menyiapkan unduhan...", 0))

        scope.launch {
            runDownload(url)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun runDownload(url: String) {
        val outDir = "/storage/emulated/0/Download/Molinax/%(title)s.%(ext)s"
        try {
            val process = ProcessBuilder(
                YT_DLP_PATH,
                "-f", "bestaudio",
                "--no-warnings",
                "-o", outDir,
                url
            ).redirectErrorStream(true).start()

            BufferedReader(InputStreamReader(process.inputStream)).forEachLine { line ->
                val match = PROGRESS_REGEX.find(line)
                if (match != null) {
                    val percent = match.groupValues[1].toFloatOrNull()?.toInt() ?: 0
                    updateNotification("Mengunduh audio...", percent)
                }
            }
            process.waitFor()
            updateNotification("Unduhan selesai", 100)
        } catch (e: Exception) {
            updateNotification("Unduhan gagal: ${e.message}", 0)
        }
    }

    private fun buildNotification(text: String, progress: Int): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Molinax IDE — Download")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0 && text.contains("Menyiapkan"))
            .setOngoing(progress in 1..99)
            .build()
    }

    private fun updateNotification(text: String, progress: Int) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIF_ID, buildNotification(text, progress))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Download Molinax",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
