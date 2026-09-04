package com.molinax.medialibrary

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

data class VideoMetadata(
    val title: String,
    val thumbnailUrl: String?,
    val durationSec: Int
)

object YtdlpMetadataFetcher {

    private const val YT_DLP_PATH = "/data/data/com.termux/files/usr/bin/yt-dlp"

    suspend fun fetch(url: String): VideoMetadata? = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder(YT_DLP_PATH, "--dump-json", url)
                .redirectErrorStream(false)
                .start()

            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode != 0 || output.isBlank()) {
                return@withContext null
            }

            val json = JSONObject(output)
            VideoMetadata(
                title = json.optString("title", url),
                thumbnailUrl = json.optString("thumbnail", null),
                durationSec = json.optInt("duration", 0)
            )
        } catch (e: Exception) {
            null
        }
    }
}
