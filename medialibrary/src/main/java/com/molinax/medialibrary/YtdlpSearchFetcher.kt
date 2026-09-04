package com.molinax.medialibrary

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

data class YtSearchResult(
    val id: String,
    val title: String,
    val thumbnailUrl: String?,
    val durationSec: Int?,
    val url: String
)

object YtdlpSearchFetcher {

    private const val YT_DLP_PATH = "/data/data/com.termux/files/usr/bin/yt-dlp"

    suspend fun search(query: String, limit: Int = 10): List<YtSearchResult> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<YtSearchResult>()
            try {
                val process = ProcessBuilder(
                    YT_DLP_PATH,
                    "--flat-playlist",
                    "--dump-json",
                    "--no-warnings",
                    "ytsearch$limit:$query"
                ).start()

                BufferedReader(InputStreamReader(process.inputStream)).useLines { lines ->
                    lines.forEach { line ->
                        if (line.isNotBlank()) parseLine(line)?.let { results.add(it) }
                    }
                }
                process.waitFor()
            } catch (e: Exception) {
                // gagal search, kembalikan apa yang sempat terkumpul (bisa kosong)
            }
            results
        }

    private fun parseLine(line: String): YtSearchResult? = try {
        val json = JSONObject(line)
        val id = json.optString("id", "")
        if (id.isEmpty()) null
        else YtSearchResult(
            id = id,
            title = json.optString("title", id),
            thumbnailUrl = extractThumbnail(json),
            durationSec = json.optInt("duration", -1).let { if (it >= 0) it else null },
            // jangan percaya field "url" dari flat-playlist (kadang cuma id, bukan URL utuh)
            url = "https://www.youtube.com/watch?v=$id"
        )
    } catch (e: Exception) {
        null
    }

    private fun extractThumbnail(json: JSONObject): String? {
        val direct = json.optString("thumbnail", "")
        if (direct.isNotEmpty()) return direct
        val arr = json.optJSONArray("thumbnails") ?: return null
        if (arr.length() == 0) return null
        return arr.getJSONObject(arr.length() - 1).optString("url", "").ifEmpty { null }
    }
}
