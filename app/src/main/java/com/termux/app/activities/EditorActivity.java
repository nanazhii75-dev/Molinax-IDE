package com.termux.app.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.termux.R;
import com.termux.app.editor.EditableSource;
import com.termux.app.editor.TabState;
import com.termux.app.editor.TextMateSetup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme;
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry;
import io.github.rosemoe.sora.widget.CodeEditor;

public class EditorActivity extends AppCompatActivity {

    public static final String EXTRA_FILE_PATH = "com.termux.app.activities.EditorActivity.EXTRA_FILE_PATH";

    private final List<TabState> tabs = new ArrayList<>();
    private int activeTabIndex = -1;

    private LinearLayout tabStrip;
    private TextView filePathView;
    private CodeEditor contentInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        tabStrip = findViewById(R.id.editor_tab_strip);
        filePathView = findViewById(R.id.editor_file_path);
        contentInput = findViewById(R.id.editor_content_input);

        Toast.makeText(this, "DEBUG onCreate, tabs=" + tabs.size(), Toast.LENGTH_LONG).show();
        handleIncomingIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Toast.makeText(this, "DEBUG onNewIntent, tabs sebelum=" + tabs.size() + " path=" + intent.getStringExtra(EXTRA_FILE_PATH), Toast.LENGTH_LONG).show();
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        String pathOrUri = intent.getStringExtra(EXTRA_FILE_PATH);
        if (pathOrUri == null) {
            if (tabs.isEmpty()) {
                Toast.makeText(this, "Path file tidak ditemukan", Toast.LENGTH_SHORT).show();
                finish();
            }
            return;
        }
        openOrFocusTab(pathOrUri);
    }

    private void openOrFocusTab(String pathOrUri) {
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).sourcePathOrUri.equals(pathOrUri)) {
                switchToTab(i);
                return;
            }
        }

        // simpan tab aktif sebelumnya sebelum pindah ke tab baru
        saveActiveTabState();

        TabState tab = new TabState(pathOrUri);
        tab.source = EditableSource.from(this, pathOrUri);
        try {
            tab.content = tab.source.read();
        } catch (IOException e) {
            Toast.makeText(this, "Gagal baca file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        tabs.add(tab);
        switchToTab(tabs.size() - 1);
    }

    private void switchToTab(int index) {
        if (index < 0 || index >= tabs.size()) return;

        saveActiveTabState();

        activeTabIndex = index;
        TabState tab = tabs.get(index);

        filePathView.setText(tab.sourcePathOrUri);
        contentInput.setText(tab.content);
        applyHighlighting(tab);

        contentInput.post(() -> {
            try {
                contentInput.setSelection(tab.cursorLine, 0, true);
            } catch (Exception ignored) {
                // baris tersimpan mungkin sudah tidak valid (file berubah di luar), abaikan
            }
        });

        rebuildTabStrip();
    }

    private void saveActiveTabState() {
        if (activeTabIndex < 0 || activeTabIndex >= tabs.size()) return;
        TabState tab = tabs.get(activeTabIndex);
        String currentText = contentInput.getText().toString();
        if (!currentText.equals(tab.content)) {
            tab.content = currentText;
            tab.isDirty = true;
        }
        try {
            tab.cursorLine = contentInput.getCursor().getLeftLine();
        } catch (Exception ignored) {
        }
    }

    private void rebuildTabStrip() {
        tabStrip.removeAllViews();
        for (int i = 0; i < tabs.size(); i++) {
            final int index = i;
            TabState tab = tabs.get(i);

            TextView tabView = new TextView(this);
            tabView.setText(tab.getDisplayName());
            tabView.setTextSize(13);
            tabView.setPadding(32, 24, 32, 24);
            tabView.setGravity(Gravity.CENTER);
            tabView.setSingleLine(true);
            tabView.setTextColor(Color.WHITE);
            tabView.setBackgroundColor(index == activeTabIndex ? Color.parseColor("#00C853") : Color.parseColor("#212121"));
            tabView.setOnClickListener(v -> switchToTab(index));

            tabStrip.addView(tabView);
        }
    }

    private void applyHighlighting(TabState tab) {
        if (!TextMateSetup.isReady()) {
            contentInput.postDelayed(() -> applyHighlighting(tab), 200);
            return;
        }

        String scopeName = scopeNameForFile(tab.getDisplayName());
        if (scopeName == null) return;

        try {
            contentInput.setColorScheme(TextMateColorScheme.create(ThemeRegistry.getInstance()));
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            contentInput.setEditorLanguage(TextMateLanguage.create(scopeName, true));
        } catch (Throwable e) {
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
        saveActiveTabState();
        for (TabState tab : tabs) {
            if (tab.isDirty) {
                try {
                    tab.source.write(tab.content);
                    tab.isDirty = false;
                } catch (IOException e) {
                    Toast.makeText(this, "Gagal simpan " + tab.getDisplayName() + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
