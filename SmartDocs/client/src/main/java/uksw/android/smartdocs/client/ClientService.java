package uksw.android.smartdocs.client;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.concurrent.CancellationException;

import uksw.android.smartdocs.shared.HostAndPort;

public class ClientService extends Service {
    public interface Listener {
        void onServerDiscoveryStarted();

        void onServerDiscovered(HostAndPort hostAndPort);

        void onUdpError(Exception error);
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final BinderImpl<ClientService> binder = new BinderImpl<>(this);
    private final Collection<Listener> clientListeners = new LinkedHashSet<>();
    private UdpClient udpClient;
    private TcpClientSessions tcpClientSessions;

    @Override
    public void onCreate() {
        super.onCreate();
        udpClient = new UdpClient(this, mainHandler,
                this::onServerDiscovered, this::onUpdateNotification, this::onUdpError);
        tcpClientSessions = new TcpClientSessions(() -> new TcpClient(udpClient.ensureDiscoveryResult()));
    }

    @Nullable
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
        clientListeners.clear();
        super.onDestroy();
    }

    public void addDiscoveryListener(Listener discoveryListener) {
        clientListeners.add(discoveryListener);
    }

    public void removeDiscoverListener(Listener discoveryListener) {
        clientListeners.remove(discoveryListener);
    }

    public void startDiscovery(boolean forceRestart) {
        if (forceRestart || !udpClient.isRunning()) {
            onServerDiscoveryStarted();
            udpClient.start();
        }

    }

    private void onServerDiscoveryStarted() {
        showToast("Running Server Discovery...");
        for (Listener listener : clientListeners) {
            listener.onServerDiscoveryStarted();
        }
    }

    private void onServerDiscovered(HostAndPort hostAndPort) {
        showToast("Server Discovered: " + hostAndPort);
        for (Listener listener : clientListeners) {
            listener.onServerDiscovered(hostAndPort);
        }
    }

    private void onUpdateNotification(Void v) {
        // TODO
    }

    private void onUdpError(Exception error) {
        if (!(error instanceof CancellationException)) {
            showToast("Udp Server Error: " + error.getMessage());
            for (Listener listener : clientListeners) {
                listener.onUdpError(error);
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
