package uksw.android.smartdocs.server;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private final BroadcastReceiver serverStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(ServerService.EXTRA_STATUS, ServerService.STATUS_STOPPED);
            switch (status) {
                case ServerService.STATUS_STARTING:
                case ServerService.STATUS_STARTED:
                case ServerService.STATUS_STOPPING:
                case ServerService.STATUS_STOPPED:
                case ServerService.STATUS_ERROR:
                    break;
            }
        }
    };
    private Button startButton;
    private Button stopButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startButton = findViewById(R.id.button_start);
        startButton.setOnClickListener(v -> startServer());
        stopButton = findViewById(R.id.button_stop);
        stopButton.setOnClickListener(v -> stopServer());
        findViewById(R.id.button_list_files).setOnClickListener(v -> listFiles());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ActivityCompat.registerReceiver(this, serverStateReceiver,
                new IntentFilter(ServerService.ACTION_BROADCAST_STATUS),
                ContextCompat.RECEIVER_NOT_EXPORTED);
        sendBroadcast(new Intent(ServerService.ACTION_REQUEST_STATUS));
    }

    @Override
    protected void onStop() {
        unregisterReceiver(serverStateReceiver);
        super.onStop();
    }

    private void startServer() {
        Intent intent = new Intent(this, ServerService.class);
        ActivityCompat.startForegroundService(this, intent);
    }

    private void stopServer() {
        Intent intent = new Intent(ServerService.ACTION_REQUEST_STOP);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    private void listFiles() {
        // TODO
    }
}