package com.termux.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.termux.R;

public class MainActivity extends AppCompatActivity {

    private FrameLayout contentFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contentFrame = findViewById(R.id.main_content_frame);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_terminal) {
                startActivity(new Intent(this, TermuxActivity.class));
                return true;
            } else if (id == R.id.nav_mpv) {
                showPlaceholder("Mpv — coming soon");
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
    }

    private void showPlaceholder(String label) {
        contentFrame.removeAllViews();
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(18);
        tv.setPadding(48, 48, 48, 48);
        contentFrame.addView(tv);
    }
}
