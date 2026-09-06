package com.termux.app.router;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Central routing decision for "what should open this file/Uri".
 * Hub-and-spoke pattern: callers (mpv tab, Terminal, Utilities, File Explorer)
 * only ask FileRouter.decide(...) and act on the result — they never
 * know about each other directly.
 */
public class FileRouter {

    private static final String TAG = "FileRouter";

    public enum RouteTarget {
        EDITOR,       // open in EditorActivity (text/code files, unknown/no extension)
        MEDIA,        // open in MPVActivity (video/audio)
        UNSUPPORTED   // hand off to system chooser (Intent.ACTION_VIEW generic)
    }

    private static final Set<String> MEDIA_EXT = new HashSet<>(Arrays.asList(
        "mp4", "mkv", "webm", "avi", "mov", "flv", "wmv", "m4v",
        "mp3", "wav", "flac", "ogg", "m4a", "aac", "opus", "wma"
    ));

    private static final Set<String> UNSUPPORTED_EXT = new HashSet<>(Arrays.asList(
        // images
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg",
        // archives
        "zip", "rar", "7z", "tar", "gz", "bz2", "xz",
        // executables / binaries
        "apk", "so", "dex", "exe", "bin",
        // office / documents (binary format, not plain text)
        "doc", "docx", "xls", "xlsx", "ppt", "pptx", "pdf",
        // fonts
        "ttf", "otf", "woff", "woff2",
        // databases
        "db", "sqlite", "sqlite3"
    ));

    /**
     * @param context needed to resolve content:// Uri metadata (display name)
     * @param pathOrUri either a plain filesystem path or a content:// Uri string
     */
    public static RouteTarget decide(Context context, String pathOrUri) {
        if (pathOrUri == null || pathOrUri.trim().isEmpty()) {
            return RouteTarget.UNSUPPORTED;
        }

        String extension;
        if (pathOrUri.startsWith("content://")) {
            extension = extensionFromContentUri(context, Uri.parse(pathOrUri));
        } else {
            extension = extensionFromPath(pathOrUri);
        }

        if (extension == null || extension.isEmpty()) {
            // no extension (e.g. Makefile, Dockerfile) -> treat as text, send to Editor
            return RouteTarget.EDITOR;
        }

        extension = extension.toLowerCase();

        if (MEDIA_EXT.contains(extension)) return RouteTarget.MEDIA;
        if (UNSUPPORTED_EXT.contains(extension)) return RouteTarget.UNSUPPORTED;
        return RouteTarget.EDITOR;
    }

    private static String extensionFromPath(String path) {
        String name = path.substring(path.lastIndexOf('/') + 1);
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return null;
        return name.substring(dot + 1);
    }

    private static String extensionFromContentUri(Context context, Uri uri) {
        ContentResolver resolver = context.getContentResolver();
        String displayName = null;
        try (Cursor cursor = resolver.query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) displayName = cursor.getString(idx);
            }
        } catch (Exception e) {
            Log.e(TAG, "gagal query display name untuk " + uri, e);
        }
        if (displayName == null) return null;
        int dot = displayName.lastIndexOf('.');
        if (dot < 0 || dot == displayName.length() - 1) return null;
        return displayName.substring(dot + 1);
    }
}
