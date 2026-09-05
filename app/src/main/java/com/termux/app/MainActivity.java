package com.termux.app;

import android.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.molinax.medialibrary.YtSearchResult;
import java.util.List;
import com.molinax.medialibrary.DownloadService;
import com.termux.shared.android.PermissionUtils;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.molinax.medialibrary.MediaLibraryBridge;
import com.termux.R;

public class MainActivity extends AppCompatActivity {

    private FrameLayout contentFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bottom_nav_host);
        contentFrame = findViewById(R.id.main_content_frame);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_terminal) {
                startActivity(new Intent(this, TermuxActivity.class));
                // return false: Terminal buka Activity terpisah, contentFrame gak berubah,
                // jadi highlight bottom nav JANGAN ikut pindah ke Terminal — biarkan tetap
                // nunjukin tab yang isinya beneran lagi ditampilkan di contentFrame.
                return false;
            } else if (id == R.id.nav_mpv) {
                showMpvHome();
                return true;
            } else if (id == R.id.nav_editor) {
                showPlaceholder("Code Editor — coming soon");
                return true;
            } else if (id == R.id.nav_utilities) {
                showPlaceholder("Utilities — coming soon");
                return true;
            }
            return false;
        });

        // Default landing screen
        showMpvHome();
    }

    private void showPlaceholder(String label) {
        contentFrame.removeAllViews();
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(18);
        tv.setPadding(48, 48, 48, 48);
        contentFrame.addView(tv);
    }

    private void showMpvHome() {
        contentFrame.removeAllViews();
        View view = getLayoutInflater().inflate(R.layout.view_mpv_home, contentFrame, false);
        contentFrame.addView(view);
        EditText urlInput = view.findViewById(R.id.mpv_url_input);
        Button playButton = view.findViewById(R.id.mpv_play_button);
        Button browseButton = view.findViewById(R.id.mpv_browse_button);
        Button downloadAudioButton = view.findViewById(R.id.mpv_download_audio_button);
        ProgressBar loadingIndicator = view.findViewById(R.id.mpv_loading_indicator);
        playButton.setOnClickListener(v -> {
            String input = urlInput.getText().toString().trim();
            if (TextUtils.isEmpty(input)) {
                Toast.makeText(this, "Masukkan judul atau URL dulu", Toast.LENGTH_SHORT).show();
                return;
            }
            if (input.startsWith("http://") || input.startsWith("https://")) {
                playUrl(input, playButton, loadingIndicator);
            } else {
                searchByTitle(input, playButton, loadingIndicator);
            }
        });
        browseButton.setOnClickListener(v ->
            startActivity(new Intent(this, is.xyz.mpv.MainActivity.class))
        );
        downloadAudioButton.setOnClickListener(v -> {
            String url = urlInput.getText().toString().trim();
            if (TextUtils.isEmpty(url) || !(url.startsWith("http://") || url.startsWith("https://"))) {
                Toast.makeText(this, "Masukkan URL video dulu (bukan judul) untuk download", Toast.LENGTH_SHORT).show();
                return;
            }
            showDownloadPresetDialog(url);
        });
    }
    private void playUrl(String url, Button playButton, ProgressBar loadingIndicator) {
        playButton.setEnabled(false);
        loadingIndicator.setVisibility(View.VISIBLE);
        MediaLibraryBridge.INSTANCE.resolveAndSave(
            getApplicationContext(),
            url,
            (title, thumbnailUrl) -> {
                playButton.setEnabled(true);
                loadingIndicator.setVisibility(View.GONE);
                launchMpvPlayer(url);
            }
        );
    }
    private void showDownloadPresetDialog(String url) {
        final String[] labels = {"1080p (video)", "720p (video)", "480p (video)", "Audio saja"};
        final String[] presets = {"1080p", "720p", "480p", "audio"};
        new AlertDialog.Builder(this)
            .setTitle("Pilih kualitas unduhan")
            .setItems(labels, (dialog, which) -> startDownload(url, presets[which]))
            .setNegativeButton("Batal", null)
            .show();
    }

    private void startDownload(String url, String preset) {
        if (!PermissionUtils.checkAndRequestLegacyOrManageExternalStoragePermissionIfPathOnPrimaryExternalStorage(
                this, "/storage/emulated/0/Download/Molinax/", 1001, true)) {
            return;
        }
        Intent serviceIntent = new Intent(this, DownloadService.class);
        serviceIntent.putExtra(DownloadService.EXTRA_URL, url);
        serviceIntent.putExtra(DownloadService.EXTRA_PRESET, preset);
        ContextCompat.startForegroundService(this, serviceIntent);
        Toast.makeText(this, "Download dimulai, cek notifikasi untuk progress", Toast.LENGTH_SHORT).show();
    }

    private void searchByTitle(String query, Button playButton, ProgressBar loadingIndicator) {
        playButton.setEnabled(false);
        loadingIndicator.setVisibility(View.VISIBLE);
        MediaLibraryBridge.INSTANCE.search(query, results -> {
            playButton.setEnabled(true);
            loadingIndicator.setVisibility(View.GONE);
            if (results.isEmpty()) {
                Toast.makeText(this, "Tidak ditemukan", Toast.LENGTH_SHORT).show();
                return;
            }
            showSearchResultsDialog(results, playButton, loadingIndicator);
        });
    }
    private void showSearchResultsDialog(List<YtSearchResult> results, Button playButton, ProgressBar loadingIndicator) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_search_results, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.search_results_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Pilih video")
            .setView(dialogView)
            .setNegativeButton("Batal", null)
            .create();
        SearchResultAdapter adapter = new SearchResultAdapter(results, new SearchResultAdapter.OnResultClick() {
            @Override
            public void onResultClick(com.molinax.medialibrary.YtSearchResult result) {
                dialog.dismiss();
                playUrl(result.getUrl(), playButton, loadingIndicator);
            }

            @Override
            public void onDownloadClick(com.molinax.medialibrary.YtSearchResult result) {
                dialog.dismiss();
                showDownloadPresetDialog(result.getUrl());
            }
        });
        recyclerView.setAdapter(adapter);
        dialog.show();
    }
    private void launchMpvPlayer(String url) {
        Intent intent = new Intent(this, is.xyz.mpv.MPVActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }
}
