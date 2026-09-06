package com.termux.app.activities;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.termux.R;
import com.termux.app.editor.TextMateSetup;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme;
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry;
import io.github.rosemoe.sora.widget.CodeEditor;

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
        applyHighlighting();
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

    private int highlightRetryCount = 0;

    private void applyHighlighting() {
        if (!TextMateSetup.isReady()) {
            highlightRetryCount++;
            if (highlightRetryCount > 25) {
                Toast.makeText(this, "TextMateSetup timeout. Last error: " + TextMateSetup.getLastError(), Toast.LENGTH_LONG).show();
                return;
            }
            contentInput.postDelayed(this::applyHighlighting, 200);
            return;
        }

        String scopeName = scopeNameForFile(file.getName());
        if (scopeName == null) return;

        try {
            contentInput.setColorScheme(TextMateColorScheme.create(ThemeRegistry.getInstance()));
        } catch (Throwable e) {
            Toast.makeText(this, "Gagal set color scheme: " + e, Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
        try {
            contentInput.setEditorLanguage(TextMateLanguage.create(scopeName, true));
        } catch (Throwable e) {
            Toast.makeText(this, "Gagal set language: " + e, Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private String scopeNameForFile(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".java")) return "source.java";
        if (lower.endsWith(".kt") || lower.endsWith(".kts")) return "source.kotlin";
        if (lower.endsWith(".py")) return "source.python";
        if (lower.endsWith(".xml")) return "text.xml";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text.html.basic";
        if (lower.endsWith(".js")) return "source.js";
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) return "text.html.markdown";
        if (lower.endsWith(".json")) return "source.json";
        if (lower.endsWith(".yml") || lower.endsWith(".yaml")) return "source.yaml";
        if (lower.endsWith(".sh") || lower.endsWith(".bash")) return "source.shell";
        if (lower.endsWith(".gradle")) return "source.groovy";
        if (lower.endsWith(".c") || lower.endsWith(".h")) return "source.c";
        if (lower.endsWith(".cpp") || lower.endsWith(".cc") || lower.endsWith(".cxx") || lower.endsWith(".hpp")) return "source.cpp";
        return null;
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
