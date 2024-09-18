package uksw.android.smartdocs.client;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.concurrent.CancellationException;

public class ClientService extends Service {
    public static class BinderImpl extends Binder {
        private final ClientService clientService;

        public BinderImpl(ClientService clientService) {
            this.clientService = clientService;
        }

        public ClientService getService() {
            return clientService;
        }
    }

    public static final int STATUS_DISCONNECTED = 0;
    public static final int STATUS_CONNECTED = 1;
    public static final int STATUS_CONNECTING = 2;

    public interface StatusListener {
        void onConnectionStatus(int status);
    }

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final BinderImpl binder = new BinderImpl(this);
    private final Collection<StatusListener> statusListeners = new LinkedHashSet<>();
    private UdpClient udpClient;
    private TcpClientSessions tcpClientSessions;

    @Override
    public void onCreate() {
        super.onCreate();
        udpClient = new UdpClient(this, uiHandler,
                this::onHandshake, this::onUpdateNotification, this::onUdpError);
        tcpClientSessions = new TcpClientSessions(() -> new TcpClient(udpClient.ensureHandshake()));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        udpClient.stop();
        udpClient = null;
        tcpClientSessions.close();
        tcpClientSessions = null;
        statusListeners.clear();
        super.onDestroy();
    }

    public void addStatusListener(StatusListener discoveryListener) {
        discoveryListener.onConnectionStatus(inferConnectionStatus());
        statusListeners.add(discoveryListener);
    }

    public void removeStatusListener(StatusListener discoveryListener) {
        statusListeners.remove(discoveryListener);
    }

    public void connectServer() {
        if (!udpClient.isRunning()) {
            notifyConnectionStatusChange(STATUS_CONNECTING);
            udpClient.start();
        }
    }

    public void disconnectServer() {
        if (udpClient.isRunning()) {
            udpClient.stop();
        }
    }

    public void requestSync() {
        // TODO
    }

    private void onHandshake(HostAndPort hostAndPort) {
        showToast("Server Discovered: " + hostAndPort);
        notifyConnectionStatusChange(STATUS_CONNECTED);
    }

    private void onUpdateNotification(Void v) {
        // TODO
    }

    private void onUdpError(Exception error) {
        if (!(error instanceof CancellationException)) {
            showToast("Udp Server Error: " + error.getMessage());
        }
        notifyConnectionStatusChange(STATUS_DISCONNECTED);
    }

    private void notifyConnectionStatusChange(int status) {
        for (StatusListener listener : statusListeners) {
            listener.onConnectionStatus(status);
        }
    }

    private int inferConnectionStatus() {
        return udpClient.isConnected() ?
                STATUS_CONNECTED : udpClient.isConnecting() ?
                STATUS_CONNECTING : STATUS_DISCONNECTED;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
