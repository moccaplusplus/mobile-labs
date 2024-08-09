package uksw.android.smartdocs.client;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import uksw.android.smartdocs.shared.HostAndPort;

public class MainActivity extends AppCompatActivity implements BinderImpl.Connection<ClientService>, ClientService.Listener {
    private TextView serverInfo;
    private Button saveButton;
    private Button syncButton;
    private Button createButton;
    private Button editButton;
    private Button colLabButton;
    private Button removeButton;
    private ClientService clientService;

    @Override
    public void onServiceConnected(ComponentName name, ClientService service) {
        clientService = service;
        clientService.addDiscoveryListener(this);
        updateButtonStates();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        clientService = null;
        updateButtonStates();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        serverInfo = findViewById(R.id.input_host);
        saveButton = findViewById(R.id.button_save);
        syncButton = findViewById(R.id.button_sync);
        createButton = findViewById(R.id.button_create);
        editButton = findViewById(R.id.button_edit);
        colLabButton = findViewById(R.id.button_col_lab);
        removeButton = findViewById(R.id.button_remove);
        syncButton.setOnClickListener(v -> requestSync());
        createButton.setOnClickListener(v -> createFile());
        editButton.setOnClickListener(v -> editFile());
        colLabButton.setOnClickListener(v -> initColLab());
        removeButton.setOnClickListener(v -> removeFile());
        updateButtonStates();
        bindService(new Intent(this, ClientService.class), this, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        clientService.removeDiscoverListener(this);
        unbindService(this);
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateButtonStates() {
        boolean connected = clientService != null;
        saveButton.setEnabled(connected);
        syncButton.setEnabled(connected);
        createButton.setEnabled(connected);
        editButton.setEnabled(connected);
        colLabButton.setEnabled(connected);
        removeButton.setEnabled(connected);
    }

    private void requestSync() {
        clientService.startDiscovery(false);
    }

    private void createFile() {

    }

    private void editFile() {

    }

    private void initColLab() {

    }

    private void removeFile() {

    }

    @Override
    public void onServerDiscoveryStarted() {
        serverInfo.setText("");
    }

    @Override
    public void onServerDiscovered(HostAndPort hostAndPort) {

    }

    @Override
    public void onUdpError(Exception exception) {

    }
}