package com.termux.app;

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
                return true;
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
            String url = urlInput.getText().toString().trim();
            if (TextUtils.isEmpty(url)) {
                Toast.makeText(this, "Masukkan URL dulu", Toast.LENGTH_SHORT).show();
                return;
            }

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
        });

        browseButton.setOnClickListener(v ->
            startActivity(new Intent(this, is.xyz.mpv.MainActivity.class))
        );
    }

    private void launchMpvPlayer(String url) {
        Intent intent = new Intent(this, is.xyz.mpv.MPVActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }
}
