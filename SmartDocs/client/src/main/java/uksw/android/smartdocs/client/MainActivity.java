package uksw.android.smartdocs.client;

import static uksw.android.smartdocs.client.ClientService.STATUS_CONNECTED;
import static uksw.android.smartdocs.client.ClientService.STATUS_CONNECTING;
import static uksw.android.smartdocs.client.ClientService.STATUS_DISCONNECTED;
import static uksw.android.smartdocs.shared.Dialogs.alert;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.DocumentsContract;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.FileNotFoundException;

import uksw.android.smartdocs.shared.Pickers;
import uksw.android.smartdocs.shared.SettingsView;

public class MainActivity extends AppCompatActivity implements ServiceConnection, ClientService.StatusListener {
    private static final int REQUEST_CODE_EDIT = 111;
    private static final int REQUEST_CODE_CREATE = 112;
    private static final int REQUEST_CODE_REMOVE = 113;

    private SettingsView settingsView;
    private TextView statusInfo;
    private Button connectButton;
    private Button disconnectButton;
    private Button syncButton;
    private Button createButton;
    private Button editButton;
    private Button removeButton;
    private ClientService clientService;
    private int connectionStatus = STATUS_DISCONNECTED;

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        clientService = ((ClientService.BinderImpl) service).getService();
        clientService.addStatusListener(this);
        updateUiState();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        clientService = null;
        updateUiState();
    }

    @Override
    public void onConnectionStatus(int status) {
        connectionStatus = status;
        updateUiState();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        settingsView = findViewById(R.id.settings_view);
        statusInfo = findViewById(R.id.textview_status);
        connectButton = findViewById(R.id.button_connect);
        disconnectButton = findViewById(R.id.button_disconnect);
        syncButton = findViewById(R.id.button_sync);
        createButton = findViewById(R.id.button_create);
        editButton = findViewById(R.id.button_edit);
        removeButton = findViewById(R.id.button_remove);
        connectButton.setOnClickListener(v -> connect());
        disconnectButton.setOnClickListener(v -> disconnect());
        syncButton.setOnClickListener(v -> requestSync());
        createButton.setOnClickListener(v -> createFile());
        editButton.setOnClickListener(v -> editFile());
        removeButton.setOnClickListener(v -> removeFile());
        updateUiState();
        bindService(new Intent(getApplicationContext(), ClientService.class), this, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        clientService.removeStatusListener(this);
        unbindService(this);
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        switch (requestCode) {
            case REQUEST_CODE_EDIT:
            case REQUEST_CODE_CREATE:
                editFile(data.getData());
                break;
            case REQUEST_CODE_REMOVE:
                removeFile(data.getData());
                break;
        }
    }

    private void updateUiState() {
        if (clientService == null) {
            statusInfo.setText(R.string.service_not_available);
            settingsView.setEnabled(true);
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(false);
            syncButton.setEnabled(false);
            createButton.setEnabled(false);
            editButton.setEnabled(false);
            removeButton.setEnabled(false);
        } else {
            switch (connectionStatus) {
                case STATUS_CONNECTED:
                    statusInfo.setText(R.string.connected);
                    break;
                case STATUS_CONNECTING:
                    statusInfo.setText(R.string.connecting);
                    break;
                case STATUS_DISCONNECTED:
                    statusInfo.setText(R.string.disconnected);
                    break;
            }
            settingsView.setEnabled(connectionStatus == STATUS_DISCONNECTED);
            connectButton.setEnabled(connectionStatus == STATUS_DISCONNECTED);
            disconnectButton.setEnabled(connectionStatus == STATUS_CONNECTED);
            syncButton.setEnabled(connectionStatus == STATUS_CONNECTED);
            createButton.setEnabled(true);
            editButton.setEnabled(true);
            removeButton.setEnabled(true);
        }
    }

    private void connect() {
        clientService.connectServer();
    }

    private void disconnect() {
        clientService.disconnectServer();
    }

    private void requestSync() {
        clientService.requestSync();
    }

    private void editFile(Uri uri) {
        Intent intent = new Intent(this, EditorActivity.class);
        intent.setData(uri);
        startActivity(intent);
    }

    private void removeFile(Uri uri) {
        try {
            ContentResolver resolver = getContentResolver();
            DocumentsContract.deleteDocument(resolver, uri);
            resolver.notifyChange(uri, null);
            alert(this, R.string.file_removed);
        } catch (FileNotFoundException e) {
            alert(this, R.string.error, R.string.failed_remove_file);
        }
    }

    private void createFile() {
        Pickers.create(this, ClientProvider.ROOT_DOCUMENT_URI, REQUEST_CODE_CREATE);
    }

    private void editFile() {
        Pickers.open(this, ClientProvider.ROOT_DOCUMENT_URI, REQUEST_CODE_EDIT);
    }

    private void removeFile() {
        Pickers.open(this, ClientProvider.ROOT_DOCUMENT_URI, REQUEST_CODE_REMOVE);
    }
}