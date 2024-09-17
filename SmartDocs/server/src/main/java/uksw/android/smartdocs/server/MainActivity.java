package uksw.android.smartdocs.server;

import static android.Manifest.permission.POST_NOTIFICATIONS;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static uksw.android.smartdocs.server.ServerService.STATUS_STARTED;
import static uksw.android.smartdocs.server.ServerService.STATUS_STOPPED;
import static uksw.android.smartdocs.shared.Dialogs.alert;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import uksw.android.smartdocs.shared.Dialogs;
import uksw.android.smartdocs.shared.Settings;
import uksw.android.smartdocs.shared.SettingsView;

public class MainActivity extends AppCompatActivity {
    private final BroadcastReceiver serverStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(ServerService.EXTRA_STATUS, STATUS_STOPPED);
            String statusMsg = intent.getStringExtra(ServerService.EXTRA_STATUS_MSG);
            onStatusUpdate(status, statusMsg);
        }
    };

    private Button startButton;
    private Button stopButton;
    private TextView statusTextView;
    private SettingsView settingsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        settingsView = findViewById(R.id.settings_view);
        statusTextView = findViewById(R.id.textview_status);
        startButton = findViewById(R.id.button_start);
        startButton.setOnClickListener(v -> startServer());
        stopButton = findViewById(R.id.button_stop);
        stopButton.setOnClickListener(v -> stopServer());
        findViewById(R.id.button_list_files).setOnClickListener(v -> listFiles());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{POST_NOTIFICATIONS}, 0);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter(ServerService.ACTION_BROADCAST_STATUS);
        ActivityCompat.registerReceiver(
                this, serverStateReceiver, intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED);
        Intent intent = new Intent(ServerService.ACTION_REQUEST_STATUS);
        intent.setPackage(getPackageName());
        onStatusUpdate(STATUS_STOPPED, "Stopped");
        sendBroadcast(intent);
    }

    @Override
    protected void onStop() {
        unregisterReceiver(serverStateReceiver);
        super.onStop();
    }

    private void onStatusUpdate(int status, String statusMsg) {
        statusTextView.setText(getString(R.string.status_label, statusMsg));
        settingsView.setEnabled(status != STATUS_STARTED);
        startButton.setEnabled(status != STATUS_STARTED);
        stopButton.setEnabled(status == STATUS_STARTED);
    }

    private void startServer() {
        int udpPort = settingsView.getUdpServerPort();
        if (udpPort == -1) {
            alert(this, R.string.invalid_port, R.string.invalid_port_msg);
            return;
        }
        Intent intent = new Intent(this, ServerService.class);
        intent.putExtra(ServerService.EXTRA_UDP_PORT, udpPort);
        ActivityCompat.startForegroundService(this, intent);
    }

    private void stopServer() {
        stopService(new Intent(getApplicationContext(), ServerService.class));
    }

    private void listFiles() {
        // TODO
    }
}