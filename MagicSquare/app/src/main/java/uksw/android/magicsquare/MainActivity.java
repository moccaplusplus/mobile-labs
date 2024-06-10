package uksw.android.magicsquare;

import static uksw.android.magicsquare.SettingsActivity.EXTRA_LEVEL;
import static uksw.android.magicsquare.SettingsActivity.EXTRA_NICKNAME;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final Uri ABOUT_URI = Uri.parse("https://en.wikipedia.org/wiki/Magic_square");
    private static final int SETTINGS_REQUEST_CODE = 12345;

    private Button playButton;
    private TextView messageView;
    private String nickname;
    private int level;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        messageView = findViewById(R.id.tv_message);
        playButton = findViewById(R.id.btn_play);
        playButton.setOnClickListener(v -> game());
        findViewById(R.id.btn_params).setOnClickListener(v -> settings());
        findViewById(R.id.btn_about).setOnClickListener(v -> about());
        findViewById(R.id.btn_quit).setOnClickListener(v -> finishAffinity());
        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        }
        updateButtonsAndMessage();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETTINGS_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            nickname = data.getStringExtra(EXTRA_NICKNAME);
            level = data.getIntExtra(EXTRA_LEVEL, 0);
            updateButtonsAndMessage();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_NICKNAME, nickname);
        outState.putInt(EXTRA_LEVEL, level);
    }

    private void restoreState(Bundle savedInstanceState) {
        nickname = savedInstanceState.getString(EXTRA_NICKNAME);
        level = savedInstanceState.getInt(EXTRA_LEVEL, 0);
    }

    private void updateButtonsAndMessage() {
        if (level == 0 || nickname == null) {
            messageView.setText(getString(R.string.params_info));
            playButton.setEnabled(false);
        } else {
            messageView.setText(getString(R.string.hello, nickname, level));
            playButton.setEnabled(true);
        }
    }

    private void game() {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra(EXTRA_LEVEL, level);
        startActivity(intent);
    }

    private void settings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        if (nickname != null) {
            intent.putExtra(EXTRA_NICKNAME, nickname);
        }
        if (level != 0) {
            intent.putExtra(EXTRA_LEVEL, level);
        }
        //noinspection deprecation
        startActivityForResult(intent, SETTINGS_REQUEST_CODE);
    }

    private void about() {
        startActivity(new Intent(Intent.ACTION_VIEW, ABOUT_URI));
    }
}