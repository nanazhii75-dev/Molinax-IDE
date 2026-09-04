package com.molinax.medialibrary

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Java-friendly bridge into :medialibrary's suspend/coroutine APIs.
 * :app stays pure Java, so this wraps coroutine calls with plain callbacks.
 */
object MediaLibraryBridge {

    fun interface OnResolved {
        fun onResolved(title: String, thumbnailUrl: String?)
    }

    /**
     * Fetches metadata for [url] via yt-dlp, upserts it into the history DB,
     * and invokes [callback] on the main thread with the resolved title.
     * If metadata fetch fails, falls back to using the raw URL as title
     * so playback is never blocked by a failed lookup.
     */
    fun resolveAndSave(context: Context, url: String, callback: OnResolved) {
        CoroutineScope(Dispatchers.Main).launch {
            val metadata = YtdlpMetadataFetcher.fetch(url)
            val title = metadata?.title ?: url
            val thumbnailUrl = metadata?.thumbnailUrl

            val entry = MediaHistoryEntry(
                url = url,
                title = title,
                thumbnailUrl = thumbnailUrl,
                lastPlayedAt = System.currentTimeMillis()
            )
            AppDatabase.getInstance(context).mediaHistoryDao().upsert(entry)

            callback.onResolved(title, thumbnailUrl)
        }
    }
}
