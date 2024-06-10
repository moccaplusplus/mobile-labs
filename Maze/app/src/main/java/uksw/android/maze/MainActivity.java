package uksw.android.maze;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import uksw.android.maze.util.Dialogs;
import uksw.android.maze.util.Parameters;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_generator).setOnClickListener(v -> open(GeneratorActivity.class));
        findViewById(R.id.btn_game).setOnClickListener(v -> {
            if (Parameters.get(this).getSelectedMaze() == null) {
                Dialogs.alert(this, R.string.no_maze_selected, R.string.no_maze_game_message);
                return;
            }
            open(GameActivity.class);
        });
        findViewById(R.id.btn_parameters).setOnClickListener(v -> open(ParametersActivity.class));
    }

    private void open(Class<? extends Activity> activity) {
        startActivity(new Intent(this, activity));
    }
}