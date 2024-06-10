package uksw.android.maze;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import uksw.android.maze.model.Maze;
import uksw.android.maze.util.Dialogs;
import uksw.android.maze.util.Parameters;
import uksw.android.maze.view.GameBoard;

public class GameActivity extends AppCompatActivity implements SensorEventListener {
    private static final float MOVE_THRESHOLD = 0.2f;
    private static final long DELAY_MILLIS = 150;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private GameBoard gameBoard;
    private long stamp;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_game);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Maze maze = Parameters.get(this).getSelectedMaze();
        if (maze == null) {
            finish();
            return;
        }
        gameBoard = findViewById(R.id.game_board);
        gameBoard.setMaze(maze);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        sensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long now = System.currentTimeMillis();
        if (now - stamp > DELAY_MILLIS) {
            float x = event.values[0];
            float y = event.values[1];
            if (move(y, x)) {
                stamp = now;
                if (gameBoard.hasReachedTarget()) {
                    finishGame();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private boolean move(float y, float x) {
        float fy = Math.abs(y);
        float fx = Math.abs(x);
        if (fx > fy) {
            return move(fx, 0, -sign(x)) || move(fy, sign(y), 0);
        } else {
            return move(fy, sign(y), 0) || move(fx, 0, -sign(x));
        }
    }

    private boolean move(float f, int y, int x) {
        return f > MOVE_THRESHOLD && gameBoard.move(y, x);
    }

    private int sign(float f) {
        return f < 0 ? -1 : 1;
    }

    private void finishGame() {
        sensorManager.unregisterListener(this);
        Dialogs.alert(this, R.string.congratulations, R.string.congratulations_message);
    }
}