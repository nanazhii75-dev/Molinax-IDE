package com.termux.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.termux.app.router.FileRouter;

import java.io.File;

/**
 * Headless classifier: receives a path/content:// Uri via implicit Intent
 * (action ACTION_ROUTE_FILE), asks FileRouter for the target, then either
 * opens EditorActivity/system-chooser itself, or hands MEDIA results back
 * to the caller via setResult() so mpv's own playerLauncher (which tracks
 * returningFromPlayer) stays the one launching MPVActivity.
 */
public class RouterActivity extends AppCompatActivity {

    public static final String ACTION_ROUTE_FILE = "com.termux.app.ACTION_ROUTE_FILE";
    public static final String EXTRA_PATH = "path";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String path = getIntent().getStringExtra(EXTRA_PATH);
        if (path == null) {
            finish();
            return;
        }

        FileRouter.RouteTarget target = FileRouter.decide(this, path);
        switch (target) {
            case EDITOR:
                Intent editorIntent = new Intent(this, EditorActivity.class);
                editorIntent.putExtra(EditorActivity.EXTRA_FILE_PATH, path);
                startActivity(editorIntent);
                finish();
                break;

            case MEDIA:
                // Jangan launch MPVActivity di sini — kembalikan ke caller supaya
                // tetap lewat playerLauncher milik MainScreenFragment.
                Intent result = new Intent();
                result.putExtra(EXTRA_PATH, path);
                setResult(RESULT_OK, result);
                finish();
                break;

            case UNSUPPORTED:
            default:
                openWithSystemChooser(path);
                finish();
                break;
        }
    }

    private void openWithSystemChooser(String pathOrUri) {
        try {
            Uri uri = pathOrUri.startsWith("content://")
                ? Uri.parse(pathOrUri)
                : Uri.fromFile(new File(pathOrUri));
            Intent viewIntent = new Intent(Intent.ACTION_VIEW);
            viewIntent.setData(uri);
            viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(viewIntent, "Buka dengan"));
        } catch (Exception e) {
            Toast.makeText(this, "Tidak ada aplikasi yang bisa membuka file ini", Toast.LENGTH_LONG).show();
        }
    }
}
