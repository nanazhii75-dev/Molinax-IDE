package com.termux.app.editor;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Abstracts reading/writing a text file regardless of whether it comes from
 * a plain filesystem path or a content:// Uri (SAF doc-picker). EditorActivity
 * only calls read()/write()/getDisplayName() and never branches on source type.
 */
public interface EditableSource {

    String read() throws IOException;

    void write(String content) throws IOException;

    String getDisplayName();

    static EditableSource from(Context context, String pathOrUri) {
        if (pathOrUri.startsWith("content://")) {
            return new ContentUriEditableSource(context, Uri.parse(pathOrUri));
        }
        return new FileEditableSource(new File(pathOrUri));
    }

    class FileEditableSource implements EditableSource {
        private final File file;

        FileEditableSource(File file) {
            this.file = file;
        }

        @Override
        public String read() throws IOException {
            if (!file.exists()) {
                throw new IOException("File tidak ditemukan: " + file.getAbsolutePath());
            }
            try (FileInputStream fis = new FileInputStream(file)) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] data = new byte[4096];
                int read;
                while ((read = fis.read(data)) != -1) {
                    buffer.write(data, 0, read);
                }
                return buffer.toString("UTF-8");
            }
        }

        @Override
        public void write(String content) throws IOException {
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(content.getBytes("UTF-8"));
            }
        }

        @Override
        public String getDisplayName() {
            return file.getName();
        }
    }

    class ContentUriEditableSource implements EditableSource {
        private final Context context;
        private final Uri uri;

        ContentUriEditableSource(Context context, Uri uri) {
            this.context = context.getApplicationContext();
            this.uri = uri;
        }

        @Override
        public String read() throws IOException {
            ContentResolver resolver = context.getContentResolver();
            try (InputStream is = resolver.openInputStream(uri)) {
                if (is == null) throw new IOException("Tidak bisa membuka Uri: " + uri);
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] data = new byte[4096];
                int read;
                while ((read = is.read(data)) != -1) {
                    buffer.write(data, 0, read);
                }
                return buffer.toString("UTF-8");
            }
        }

        @Override
        public void write(String content) throws IOException {
            ContentResolver resolver = context.getContentResolver();
            try (OutputStream os = resolver.openOutputStream(uri, "wt")) {
                if (os == null) throw new IOException("Tidak bisa menulis ke Uri: " + uri);
                os.write(content.getBytes("UTF-8"));
            }
        }

        @Override
        public String getDisplayName() {
            ContentResolver resolver = context.getContentResolver();
            try (Cursor cursor = resolver.query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) {
                        String name = cursor.getString(idx);
                        if (name != null) return name;
                    }
                }
            } catch (Exception ignored) {
            }
            String last = uri.getLastPathSegment();
            return last != null ? last : "untitled";
        }
    }
}
