package uksw.android.maze;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import uksw.android.maze.model.Maze;
import uksw.android.maze.util.Dialogs;
import uksw.android.maze.util.Parameters;
import uksw.android.maze.view.MazeView;

public class GeneratorActivity extends AppCompatActivity {
    private Executor uiThreadExecutor;
    private Maze maze;
    private MazeView mazeView;
    private ProgressBar progressBar;
    private TextView nameInput;
    private Parameters parameters;
    private boolean saved;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_generator);
        uiThreadExecutor = ContextCompat.getMainExecutor(this);
        mazeView = findViewById(R.id.maze_view);
        progressBar = findViewById(R.id.progress_bar);
        nameInput = findViewById(R.id.name_input);
        findViewById(R.id.btn_generate).setOnClickListener(v -> generate());
        findViewById(R.id.btn_save).setOnClickListener(v -> save());
        findViewById(R.id.btn_parameters).setOnClickListener(v -> parameters());
        findViewById(R.id.btn_quit).setOnClickListener(v -> quit());
        saved = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        parameters = Parameters.get(this);
        String mazeName = getIntent().getStringExtra(Parameters.KEY_SELECTED_MAZE);
        if (mazeName != null) {
            maze = parameters.getMaze(mazeName);
            mazeView.setMaze(maze);
        }
        nameInput.setText(mazeName);
        nameInput.setEnabled(maze == null);
    }

    private void generate() {
        progressBar.setVisibility(View.VISIBLE);
        mazeView.setVisibility(View.GONE);
        int size = parameters.getMazeSize();
        CompletableFuture
                .supplyAsync(() -> Maze.generate(size))
                .whenCompleteAsync((maze, error) -> {
                    if (error != null) {
                        Toast.makeText(this, error.toString(), Toast.LENGTH_LONG).show();
                    }
                    mazeView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    mazeView.setMaze(this.maze = maze);
                    saved = false;
                }, uiThreadExecutor);
    }

    private void save() {
        String mazeName = nameInput.getText().toString();
        if (maze == null) {
            Dialogs.alert(this, R.string.no_maze_generated);
            return;
        }
        if (mazeName.isEmpty()) {
            Dialogs.alert(this, R.string.name_is_empty);
            return;
        }
        saved = parameters.commit(editor -> editor.setMaze(mazeName, maze));
        Toast.makeText(this, saved ? R.string.prefs_saved : R.string.prefs_save_error, Toast.LENGTH_SHORT).show();
    }

    private void parameters() {
        Intent intent = new Intent(this, ParametersActivity.class);
        startActivity(intent);
    }

    private void quit() {
        if (saved) {
            finish();
        } else {
            Dialogs.confirm(this, R.string.maze_not_saved,
                    R.string.quit_anyway, this::finish);
        }
    }
}