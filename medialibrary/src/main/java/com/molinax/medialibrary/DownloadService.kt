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
        const val EXTRA_PRESET = "extra_preset"
        private const val YT_DLP_PATH = "/data/data/com.termux/files/usr/bin/yt-dlp"
        private const val FFMPEG_PATH = "/data/data/com.termux/files/usr/bin/ffmpeg"
        private const val CHANNEL_ID = "molinax_download_channel"
        private const val NOTIF_ID = 4242
        private val PROGRESS_REGEX = Regex("""\[download]\s+(\d{1,3}\.\d)%""")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra(EXTRA_URL)
        val preset = intent?.getStringExtra(EXTRA_PRESET) ?: "audio"
        if (url.isNullOrBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("Menyiapkan unduhan...", 0))

        scope.launch {
            runDownload(url, preset)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun formatArgForPreset(preset: String): String = when (preset) {
        "1080p" -> "bv*[height<=1080]+ba/b[height<=1080]"
        "720p" -> "bv*[height<=720]+ba/b[height<=720]"
        "480p" -> "bv*[height<=480]+ba/b[height<=480]"
        else -> "bestaudio"
    }

    private fun runDownload(url: String, preset: String) {
        val outDir = "/storage/emulated/0/Download/Molinax/%(title)s.%(ext)s"
        val formatArg = formatArgForPreset(preset)
        val label = if (preset == "audio") "audio" else "video $preset"
        try {
            val process = ProcessBuilder(
                YT_DLP_PATH,
                "-f", formatArg,
                "--ffmpeg-location", FFMPEG_PATH,
                "--no-warnings",
                "-o", outDir,
                url
            ).redirectErrorStream(true).start()

            val lastLines = ArrayDeque<String>()
            BufferedReader(InputStreamReader(process.inputStream)).forEachLine { line ->
                val match = PROGRESS_REGEX.find(line)
                if (match != null) {
                    val percent = match.groupValues[1].toFloatOrNull()?.toInt() ?: 0
                    updateNotification("Mengunduh $label...", percent)
                }
                if (lastLines.size >= 5) lastLines.removeFirst()
                lastLines.addLast(line)
            }
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                updateNotification("Unduhan selesai", 100)
            } else {
                updateNotification("Unduhan gagal (exit $exitCode): ${lastLines.lastOrNull() ?: "unknown"}", 0)
            }
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
