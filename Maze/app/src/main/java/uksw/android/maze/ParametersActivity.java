package uksw.android.maze;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.Set;

import uksw.android.maze.util.Dialogs;
import uksw.android.maze.util.Parameters;
import uksw.android.maze.view.MazeView;

public class ParametersActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final int SIZE_MIN = 10;
    public static final int SIZE_MAX = 30;
    private static final String MAZE_NONE = "NONE";

    private Parameters parameters;
    private NumberPicker sizeInput;
    private NumberPicker mazeSelect;
    private MazeView mazeView;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_parameters);

        mazeSelect = findViewById(R.id.maze_select);
        mazeView = findViewById(R.id.maze_view);

        sizeInput = findViewById(R.id.maze_size);
        sizeInput.setMinValue(SIZE_MIN);
        sizeInput.setMaxValue(SIZE_MAX);
        sizeInput.setDisplayedValues(null);

        findViewById(R.id.btn_edit).setOnClickListener(v -> editSelected());
        findViewById(R.id.btn_remove).setOnClickListener(v -> removeSelected());
        findViewById(R.id.btn_generate).setOnClickListener(v -> generateNew());
        findViewById(R.id.btn_quit).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        parameters = Parameters.get(this);
        parameters.prefs.registerOnSharedPreferenceChangeListener(this);
        sizeInput.setValue(parameters.getMazeSize());
        sizeInput.setOnValueChangedListener((picker, oldVal, newVal) ->
                parameters.apply(editor -> editor.setMazeSize(newVal)));
        updateMazeSelect();
    }

    @Override
    protected void onPause() {
        parameters.prefs.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Parameters.KEY_MAZE_SIZE.equals(key)) {
            sizeInput.setValue(parameters.getMazeSize());
        } else if (Parameters.KEY_SELECTED_MAZE.equals(key)) {
            updateMazeSelect();
        }
    }

    private void clearMazeSelect() {
        mazeSelect.setDisplayedValues(null);
        mazeSelect.setMinValue(0);
        mazeSelect.setMaxValue(0);
        mazeSelect.setValue(0);
        mazeSelect.setOnValueChangedListener(null);
        mazeView.setMaze(null);
    }

    private void updateMazeSelect() {
        clearMazeSelect();
        Set<String> names = parameters.getMazeNames();
        if (names.isEmpty()) {
            mazeSelect.setDisplayedValues(new String[]{MAZE_NONE});
        } else {
            String selectedName = parameters.getSelectedName();
            String[] displayed = names.stream().sorted().toArray(String[]::new);
            mazeSelect.setDisplayedValues(null);
            mazeSelect.setMinValue(0);
            mazeSelect.setMaxValue(displayed.length - 1);
            mazeSelect.setValue(Arrays.asList(displayed).indexOf(selectedName));
            mazeSelect.setDisplayedValues(displayed);
            mazeSelect.setOnValueChangedListener((picker, oldVal, newVal) -> {
                String selected = displayed[newVal];
                parameters.apply(editor -> editor.select(selected));
                mazeView.setMaze(parameters.getMaze(selected));
            });
            mazeView.setMaze(parameters.getMaze(selectedName));
        }
    }

    private void generateNew() {
        Intent intent = new Intent(this, GeneratorActivity.class);
        startActivity(intent);
    }

    private void editSelected() {
        String selectedName = parameters.getSelectedName();
        if (selectedName == null) {
            Dialogs.alert(this, R.string.no_maze_selected);
            return;
        }
        Intent intent = new Intent(this, GeneratorActivity.class);
        intent.putExtra(Parameters.KEY_SELECTED_MAZE, selectedName);
        startActivity(intent);
    }

    private void removeSelected() {
        String selectedName = parameters.getSelectedName();
        if (selectedName == null) {
            Dialogs.alert(this, R.string.no_maze_selected);
            return;
        }
        boolean removed = parameters.commit(editor -> editor.removeMaze(selectedName));
        if (removed) {
            updateMazeSelect();
        } else {
            Toast.makeText(this, R.string.prefs_save_error, Toast.LENGTH_SHORT).show();
        }
    }
}