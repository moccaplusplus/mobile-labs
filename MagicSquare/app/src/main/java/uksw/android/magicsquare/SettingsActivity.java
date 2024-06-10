package uksw.android.magicsquare;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static uksw.android.magicsquare.GameActivity.SQUARE_SIZE;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    public static final String EXTRA_NICKNAME = "Settings.Extra.NickName";
    public static final String EXTRA_LEVEL = "Settings.Extra.Level";
    public static final int DEFAULT_LEVEL = 5;

    private EditText nicknameInput;
    private Spinner levelsInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.btn_cancel).setOnClickListener(v -> cancel());
        findViewById(R.id.btn_save).setOnClickListener(v -> save());

        nicknameInput = findViewById(R.id.input_nickname);
        levelsInput = findViewById(R.id.input_levels);
        ArrayAdapter<Integer> levelsAdapter = new ArrayAdapter<>(
                this,
                R.layout.custom_spinner,
                range(1, 1 + SQUARE_SIZE * SQUARE_SIZE).boxed().collect(toList()));
        levelsInput.setAdapter(levelsAdapter);

        Intent intent = getIntent();
        int level = intent.getIntExtra(EXTRA_LEVEL, DEFAULT_LEVEL);
        String nickname = intent.getStringExtra(EXTRA_NICKNAME);

        if (nickname == null) {
            nicknameInput.getText().clear();
        } else {
            nicknameInput.setText(nickname);
        }
        levelsInput.setSelection(levelsAdapter.getPosition(level));
    }

    private void save() {
        String nickname = nicknameInput.getText().toString();
        int level = (Integer) levelsInput.getSelectedItem();
        Intent result = new Intent();
        result.putExtra(EXTRA_LEVEL, level);
        if (!nickname.isEmpty()) {
            result.putExtra(EXTRA_NICKNAME, nickname);
        }
        setResult(RESULT_OK, result);
        finish();
    }

    private void cancel() {
        setResult(RESULT_CANCELED);
        finish();
    }
}