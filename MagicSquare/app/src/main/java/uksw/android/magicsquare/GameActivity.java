package uksw.android.magicsquare;

import static android.os.SystemClock.elapsedRealtime;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.IntStream.range;
import static uksw.android.magicsquare.SettingsActivity.DEFAULT_LEVEL;
import static uksw.android.magicsquare.SettingsActivity.EXTRA_LEVEL;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.PluralsRes;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class GameActivity extends AppCompatActivity {
    public static final int SQUARE_SIZE = 3;
    private static final String STATE_NUMBERS = "State.Numbers";
    private static final String STATE_Y_SUMS = "State.Y.Sums";
    private static final String STATE_X_SUMS = "State.X.Sums";
    private static final String STATE_ERRORS = "State.Errors";
    private static final String STATE_MESSAGE = "State.Message";
    private static final String STATE_GAME = "State.Game";
    private static final String STATE_TIME = "State.Time";

    private final Random random = new Random();
    private final int[] numbers = new int[SQUARE_SIZE * SQUARE_SIZE];
    private final int[] xSums = new int[SQUARE_SIZE];
    private final int[] ySums = new int[SQUARE_SIZE];
    private String errors;
    private SquareView squareView;
    private Button submitButton;
    private Button newButton;
    private Button helpButton;
    private TextView messageView;
    private Chronometer chronometer;
    private int level;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        squareView = findViewById(R.id.square_view);

        submitButton = findViewById(R.id.btn_submit);
        submitButton.setOnClickListener(v -> submit());

        newButton = findViewById(R.id.btn_new);
        newButton.setOnClickListener(v -> newGame());

        helpButton = findViewById(R.id.btn_help);
        helpButton.setOnClickListener(v -> help());

        findViewById(R.id.btn_exit).setOnClickListener(v -> finish());

        messageView = findViewById(R.id.tv_message);
        messageView.setMovementMethod(new ScrollingMovementMethod());

        chronometer = findViewById(R.id.chronometer);
        chronometer.setFormat("Time: %s");

        level = getIntent().getIntExtra(EXTRA_LEVEL, DEFAULT_LEVEL);

        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        } else {
            newGame();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = squareView.getInputValue(i / SQUARE_SIZE, i % SQUARE_SIZE);
        }
        outState.putIntArray(STATE_NUMBERS, numbers);
        outState.putIntArray(STATE_Y_SUMS, ySums);
        outState.putIntArray(STATE_X_SUMS, xSums);
        outState.putString(STATE_ERRORS, errors);
        outState.putString(STATE_MESSAGE, messageView.getText().toString());
        if (submitButton.isEnabled()) {
            outState.putBoolean(STATE_GAME, true);
            outState.putLong(STATE_TIME, chronometer.getBase());
        } else {
            outState.putBoolean(STATE_GAME, false);
            outState.putString(STATE_TIME, chronometer.getText().toString());
        }
    }

    private void restoreState(Bundle savedInstanceState) {
        System.arraycopy(savedInstanceState.getIntArray(STATE_NUMBERS), 0, numbers, 0, numbers.length);
        System.arraycopy(savedInstanceState.getIntArray(STATE_Y_SUMS), 0, ySums, 0, ySums.length);
        System.arraycopy(savedInstanceState.getIntArray(STATE_X_SUMS), 0, xSums, 0, xSums.length);
        writeSums();
        writeSquare();
        errors = savedInstanceState.getString(STATE_ERRORS);
        messageView.setText(savedInstanceState.getString(STATE_MESSAGE));
        if (savedInstanceState.getBoolean(STATE_GAME)) {
            submitButton.setEnabled(true);
            newButton.setEnabled(false);
            chronometer.setBase(savedInstanceState.getLong(STATE_TIME));
            chronometer.start();
        } else {
            submitButton.setEnabled(false);
            newButton.setEnabled(true);
            chronometer.setText(savedInstanceState.getString(STATE_TIME));
        }
        helpButton.setEnabled(errors != null);
    }

    private void newGame() {
        chronometer.setBase(elapsedRealtime());
        chronometer.start();
        messageView.setText(null);
        newButton.setEnabled(false);
        submitButton.setEnabled(true);
        initSquare();
        writeSums();
        hideRandomNumbers();
        writeSquare();
    }

    private void initSquare() {
        // reset numbers
        for (int i = 0; i < numbers.length; ++i) {
            numbers[i] = i + 1;
        }
        shuffle(numbers);
        // calc sums
        for (int i = 0; i < SQUARE_SIZE; i++) {
            xSums[i] = ySums[i] = 0;
            for (int j = 0; j < SQUARE_SIZE; j++) {
                xSums[i] += numbers[i * SQUARE_SIZE + j];
                ySums[i] += numbers[j * SQUARE_SIZE + i];
            }
        }
    }

    private void writeSums() {
        for (int i = 0; i < SQUARE_SIZE; i++) squareView.setXSum(i, xSums[i]);
        for (int i = 0; i < SQUARE_SIZE; i++) squareView.setYSum(i, ySums[i]);
    }

    private void writeSquare() {
        for (int i = 0; i < numbers.length; i++) {
            squareView.setInputValue(i / SQUARE_SIZE, i % SQUARE_SIZE, numbers[i]);
        }
    }

    private void shuffle(int[] numbers) {
        for (int i = 0; i < numbers.length; i++) {
            int r = random.nextInt(numbers.length);
            int e = numbers[r];
            numbers[r] = numbers[i];
            numbers[i] = e;
        }
    }

    private void hideRandomNumbers() {
        if (level > numbers.length / 2) {
            Set<Integer> indexesToShow = new HashSet<>(getUniqueRandomInts(numbers.length, numbers.length - level));
            range(0, numbers.length).filter(i -> !indexesToShow.contains(i)).forEach(i -> numbers[i] = 0);
        } else {
            getUniqueRandomInts(numbers.length, level).forEach(i -> numbers[i] = 0);
        }
    }

    private List<Integer> getUniqueRandomInts(int toExcl, int count) {
        List<Integer> indexes = range(0, toExcl).boxed().collect(toList());
        while (indexes.size() > count) {
            indexes.remove(random.nextInt(indexes.size()));
        }
        return indexes;
    }

    private void submit() {
        List<String> errorList = new ArrayList<>();
        if (!allDigitsFilled()) {
            errorList.add(getString(R.string.error_not_filled));
        }
        checkDigits(errorList);
        checkSums(errorList);
        if (errorList.isEmpty()) {
            chronometer.stop();
            errors = null;
            messageView.setText(R.string.correct_answer);
            newButton.setEnabled(true);
            submitButton.setEnabled(false);
            helpButton.setEnabled(false);
        } else {
            errors = String.join("\n", errorList);
            messageView.setText(R.string.wrong_answer);
            helpButton.setEnabled(true);
        }
    }

    private void checkDigits(List<String> errorList) {
        Set<Integer> digits = range(1, 1 + SQUARE_SIZE * SQUARE_SIZE).boxed().collect(toSet());
        for (int i = 0; i < SQUARE_SIZE; i++) {
            for (int j = 0; j < SQUARE_SIZE; j++) {
                digits.remove(squareView.getInputValue(i, j));
            }
        }
        if (!digits.isEmpty()) {
            errorList.add(getQuantityErrorMsg(R.plurals.unused_error, digits));
        }
    }

    private void checkSums(List<String> errorList) {
        List<Integer> wrongColumns = new ArrayList<>();
        List<Integer> wrongRows = new ArrayList<>();
        for (int i = 0; i < SQUARE_SIZE; i++) {
            int xSum = 0;
            int ySum = 0;
            for (int j = 0; j < SQUARE_SIZE; j++) {
                xSum += squareView.getInputValue(i, j);
                ySum += squareView.getInputValue(j, i);
            }
            if (xSum != xSums[i]) {
                wrongRows.add(i + 1);
            }
            if (ySum != ySums[i]) {
                wrongColumns.add(i + 1);

            }
        }
        if (!wrongColumns.isEmpty()) {
            errorList.add(getQuantityErrorMsg(R.plurals.col_error, wrongColumns));
        }
        if (!wrongRows.isEmpty()) {
            errorList.add(getQuantityErrorMsg(R.plurals.row_error, wrongRows));
        }
    }

    private String getQuantityErrorMsg(@PluralsRes int id, Collection<Integer> items) {
        return getResources().getQuantityString(id, items.size(), items.stream()
                .map(String::valueOf).sorted().collect(joining(", ")));
    }

    private boolean allDigitsFilled() {
        for (int i = 0; i < SQUARE_SIZE; i++) {
            for (int j = 0; j < SQUARE_SIZE; j++) {
                if (squareView.getInputValue(i, j) == 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private void help() {
        messageView.setText(errors);
        helpButton.setEnabled(false);
        errors = null;
    }
}