package uksw.android.smartdocs.client;

import static android.net.nsd.NsdManager.PROTOCOL_DNS_SD;
import static uksw.android.smartdocs.shared.SmartDocsNsd.SERVICE_TYPE;

import android.app.Service;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class ClientService extends Service implements NsdManager.DiscoveryListener {
    public class ClientBinder extends Binder {
    }

    private NsdManager nsdManager;

    @Override
    public void onCreate() {
        super.onCreate();
        nsdManager = (NsdManager) getSystemService(NSD_SERVICE);
        nsdManager.discoverServices(
                SERVICE_TYPE, PROTOCOL_DNS_SD, this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new ClientBinder();
    }

    @Override
    public void onDestroy() {
        nsdManager.stopServiceDiscovery(this);
        super.onDestroy();
    }

    @Override
    public void onStartDiscoveryFailed(String serviceType, int errorCode) {

    }

    @Override
    public void onStopDiscoveryFailed(String serviceType, int errorCode) {

    }

    @Override
    public void onDiscoveryStarted(String serviceType) {

    }

    @Override
    public void onDiscoveryStopped(String serviceType) {

    }

    @Override
    public void onServiceFound(NsdServiceInfo serviceInfo) {

    }

    @Override
    public void onServiceLost(NsdServiceInfo serviceInfo) {

    }
}
