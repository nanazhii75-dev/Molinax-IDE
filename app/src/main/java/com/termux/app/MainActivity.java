package com.termux.app;

import android.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.molinax.medialibrary.YtSearchResult;
import java.util.List;

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
        SearchResultAdapter adapter = new SearchResultAdapter(results, result -> {
            dialog.dismiss();
            playUrl(result.getUrl(), playButton, loadingIndicator);
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
