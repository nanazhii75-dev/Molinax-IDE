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

import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import com.termux.R;
import com.termux.app.editor.EditableSource;
import com.termux.app.editor.TabState;
import com.termux.app.editor.TextMateSetup;
import com.termux.app.editor.EditorSessionManager;

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
        Toolbar toolbar = findViewById(R.id.editor_toolbar);
        setSupportActionBar(toolbar);

        restoreSession();
        handleIncomingIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
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

    private void restoreSession() {
        EditorSessionManager.SessionState state = EditorSessionManager.load(this);
        if (state == null || state.tabs.isEmpty()) return;

        for (EditorSessionManager.TabEntry entry : state.tabs) {
            if (!isRestorable(entry.pathOrUri)) continue;
            try {
                TabState tab = new TabState(entry.pathOrUri);
                tab.source = EditableSource.from(this, entry.pathOrUri);
                tab.content = tab.source.read();
                tab.cursorLine = entry.cursorLine;
                tabs.add(tab);
            } catch (Exception e) {
                // file hilang / izin content:// dicabut selagi app tidak jalan — skip, jangan crash
            }
        }

        if (!tabs.isEmpty()) {
            int restoreIndex = Math.min(Math.max(state.activeTabIndex, 0), tabs.size() - 1);
            switchToTab(restoreIndex);
        }
    }

    private boolean isRestorable(String pathOrUri) {
        if (pathOrUri.startsWith("content://")) {
            android.net.Uri uri = android.net.Uri.parse(pathOrUri);
            for (android.content.UriPermission perm : getContentResolver().getPersistedUriPermissions()) {
                if (perm.getUri().equals(uri) && perm.isReadPermission()) return true;
            }
            return false;
        }
        return true;
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
        EditorSessionManager.save(this, activeTabIndex, tabs);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_open_file) {
            Toast.makeText(this, "Buka Berkas — segera hadir", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.action_open_folder) {
            Toast.makeText(this, "Buka Folder — segera hadir", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.action_new_tab) {
            Toast.makeText(this, "Tab Baru — segera hadir", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.action_save) {
            Toast.makeText(this, "Simpan — segera hadir", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.action_save_as) {
            Toast.makeText(this, "Simpan Sebagai — segera hadir", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.action_reload) {
            Toast.makeText(this, "Muat Ulang — segera hadir", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.action_recent_files) {
            Toast.makeText(this, "Berkas Terbaru — segera hadir", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.action_undo) {
            Toast.makeText(this, "Undo — segera hadir", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.action_redo) {
            Toast.makeText(this, "Redo — segera hadir", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.action_copy) {
            Toast.makeText(this, "Salin — segera hadir", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.action_paste) {
            Toast.makeText(this, "Tempel — segera hadir", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.action_find) {
            Toast.makeText(this, "Cari — segera hadir", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.action_replace) {
            Toast.makeText(this, "Ganti — segera hadir", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.action_replace_all) {
            Toast.makeText(this, "Ganti Semua — segera hadir", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.action_insert_timestamp) {
            Toast.makeText(this, "Masukkan Tanda Waktu — segera hadir", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.action_goto_line) {
            Toast.makeText(this, "Pergi ke Baris — segera hadir", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.action_word_wrap) {
            Toast.makeText(this, "Pembungkus Kata — segera hadir", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.action_read_only) {
            Toast.makeText(this, "Hanya Baca — segera hadir", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.action_syntax) {
            Toast.makeText(this, "Syntax — segera hadir", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.action_encoding) {
            Toast.makeText(this, "Encode — segera hadir", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.action_run) {
            Toast.makeText(this, "Lakukan — segera hadir", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.action_print) {
            Toast.makeText(this, "Cetak — segera hadir", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.action_share) {
            Toast.makeText(this, "Bagikan — segera hadir", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.action_statistics) {
            Toast.makeText(this, "Statistik — segera hadir", Toast.LENGTH_SHORT).show();
        }
        else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }
}
