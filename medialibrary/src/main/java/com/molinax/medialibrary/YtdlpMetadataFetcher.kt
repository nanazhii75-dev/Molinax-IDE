package com.molinax.medialibrary

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    private const val YT_DLP_PATH = "/data/data/com.termux/files/usr/bin/yt-dlp-fb"
    private const val MAX_ATTEMPTS = 3
    private const val RETRY_DELAY_MS = 1500L

    private val NETWORK_ERROR_KEYWORDS = listOf(
        "No address associated with hostname",
        "Temporary failure in name resolution",
        "Network is unreachable",
        "Connection timed out"
    )

    suspend fun fetch(url: String): VideoMetadata? = withContext(Dispatchers.IO) {
        for (attempt in 1..MAX_ATTEMPTS) {
            try {
                val process = ProcessBuilder(YT_DLP_PATH, "--dump-json", url)
                    .redirectErrorStream(false)
                    .start()

                val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
                val errorOutput = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
                val exitCode = process.waitFor()

                if (exitCode == 0 && output.isNotBlank()) {
                    val json = JSONObject(output)
                    return@withContext VideoMetadata(
                        title = json.optString("title", url),
                        thumbnailUrl = json.optString("thumbnail", null),
                        durationSec = json.optInt("duration", 0)
                    )
                }

                val isNetworkError = NETWORK_ERROR_KEYWORDS.any { errorOutput.contains(it) }
                if (!isNetworkError) {
                    return@withContext null
                }
            } catch (e: Exception) {
                // exception di level ProcessBuilder (jarang) — tetap coba retry, tidak bisa
                // dipastikan network atau bukan, jadi diperlakukan sama seperti network error
            }

            if (attempt < MAX_ATTEMPTS) {
                delay(RETRY_DELAY_MS)
            }
        }
        null
    }
}
