package uksw.android.multiplication;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class MainActivity extends AppCompatActivity implements TextWatcher, View.OnClickListener {
    private final Random random = new Random();
    private int expected;
    private EditText guessField;
    private TextView riddleText;
    private Button checkButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        riddleText = findViewById(R.id.tv_operation);
        guessField = findViewById(R.id.et_guess);
        checkButton = findViewById(R.id.bt_check);

        checkButton.setOnClickListener(this);
        guessField.addTextChangedListener(this);

        nextRiddle();
    }

    private void nextRiddle() {
        int operand1 = nextIntInRange();
        int operand2 = nextIntInRange();
        expected = operand1 * operand2;
        riddleText.setText(getString(R.string.operation, operand1, operand2));
    }

    private int nextIntInRange() {
        return 2 + random.nextInt(8);
    }

    @Override
    public void onClick(View v) {
        try {
            int guess = Integer.parseInt(guessField.getText().toString().trim());
            if (guess == expected) {
                showMessage(getString(R.string.success));
                nextRiddle();
            } else {
                showMessage(getString(R.string.wrong));
            }
        } catch (NumberFormatException e) {
            showMessage(e.getMessage());
        }
    }

    private void showMessage(CharSequence message) {
        new AlertDialog.Builder(this)
                .setTitle(message)
                .setCancelable(false)
                .setPositiveButton(R.string.close, (dialog, which) -> guessField.getText().clear())
                .show();
    }

    @Override
    public void afterTextChanged(Editable s) {
        checkButton.setEnabled(!s.toString().isEmpty());
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }
}
