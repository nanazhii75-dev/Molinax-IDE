package com.termux.app.activities;

import android.os.Bundle;
import io.github.rosemoe.sora.widget.CodeEditor;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.termux.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class EditorActivity extends AppCompatActivity {

    public static final String EXTRA_FILE_PATH = "com.termux.app.activities.EditorActivity.EXTRA_FILE_PATH";

    private File file;
    private CodeEditor contentInput;
    private TextView filePathView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        filePathView = findViewById(R.id.editor_file_path);
        contentInput = findViewById(R.id.editor_content_input);

        String path = getIntent().getStringExtra(EXTRA_FILE_PATH);
        if (path == null) {
            Toast.makeText(this, "Path file tidak ditemukan", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        file = new File(path);
        filePathView.setText(path);
        loadFile();
    }

    private void loadFile() {
        if (!file.exists()) {
            Toast.makeText(this, "File tidak ditemukan: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[4096];
            int read;
            while ((read = fis.read(data)) != -1) {
                buffer.write(data, 0, read);
            }
            contentInput.setText(buffer.toString("UTF-8"));
        } catch (IOException e) {
            Toast.makeText(this, "Gagal baca file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveFile();
    }

    private void saveFile() {
        if (file == null || contentInput == null) return;
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(contentInput.getText().toString().getBytes("UTF-8"));
        } catch (IOException e) {
            Toast.makeText(this, "Gagal simpan file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
