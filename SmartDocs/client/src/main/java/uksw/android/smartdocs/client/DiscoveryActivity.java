package uksw.android.smartdocs.client;

import static android.net.nsd.NsdManager.PROTOCOL_DNS_SD;
import static uksw.android.smartdocs.shared.SmartDocsNsd.SERVICE_TYPE;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class DiscoveryActivity extends AppCompatActivity implements NsdManager.DiscoveryListener {
    private NsdManager nsdManager;
    private ServiceListAdapter serviceListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discovery);
        nsdManager = (NsdManager) getSystemService(NSD_SERVICE);
        serviceListAdapter = new ServiceListAdapter();
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setAdapter(serviceListAdapter);
    }

    @Override
    protected void onResume() {
        startDiscovery();
        super.onResume();
    }

    @Override
    protected void onStop() {
        nsdManager.stopServiceDiscovery(this);
        super.onStop();
    }

    private void startDiscovery() {
        serviceListAdapter.clear();
        nsdManager.discoverServices(SERVICE_TYPE, PROTOCOL_DNS_SD, this);
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
        if (SERVICE_TYPE.equals(serviceInfo.getServiceType())) {
            serviceListAdapter.addServiceInfo(serviceInfo);
        }
    }

    @Override
    public void onServiceLost(NsdServiceInfo serviceInfo) {
        if (SERVICE_TYPE.equals(serviceInfo.getServiceType())) {
            serviceListAdapter.removeServiceInfo(serviceInfo);
        }
    }
}