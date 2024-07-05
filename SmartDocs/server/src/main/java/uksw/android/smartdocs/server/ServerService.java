package uksw.android.smartdocs.server;

import static android.net.nsd.NsdManager.PROTOCOL_DNS_SD;

import static java.lang.String.format;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.net.ServerSocket;

import uksw.android.smartdocs.shared.SmartDocsNsd;

public class ServerService extends Service implements NsdManager.RegistrationListener {
    public static final String ACTION_REQUEST_STOP = "action.smart-docs.request.stop";
    public static final String ACTION_REQUEST_STATUS = "action.smart-docs.request.status";
    public static final String ACTION_BROADCAST_STATUS = "action.smart-docs.broadcast.status";
    public static final String EXTRA_STATUS = "status";
    public static final int STATUS_STARTING = 1;
    public static final int STATUS_STARTED = 2;
    public static final int STATUS_STOPPING = 3;
    public static final int STATUS_STOPPED = 0;
    public static final int STATUS_ERROR = -1;

    private final BroadcastReceiver serverReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_REQUEST_STOP.equals(intent.getAction())) {
                requestServerStop();
            } else if (ACTION_REQUEST_STATUS.equals(intent.getAction())) {
                broadcastStatus();
            }
        }
    };

    private NsdManager nsdManager;
    private ServerLoop server;
    private int status = STATUS_STOPPED;

    @Override
    public void onCreate() {
        super.onCreate();
        int foregroundType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC : 0;
        ServiceCompat.startForeground(this, 2262743, createNotification(), foregroundType);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_REQUEST_STOP);
        filter.addAction(ACTION_REQUEST_STATUS);
        ContextCompat.registerReceiver(this, serverReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (status == STATUS_STOPPED) {
            startServer();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (server != null) {
            server.stop();
            server = null;
        }
        nsdManager = null;
        updateStatus(STATUS_STOPPED);
        unregisterReceiver(serverReceiver);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
        stopSelf();
    }

    @Override
    public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
        updateStatus(server == null ? STATUS_ERROR : STATUS_STARTED);
    }

    @Override
    public void onServiceRegistered(NsdServiceInfo serviceInfo) {
        updateStatus(STATUS_STARTED);
    }

    @Override
    public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
        stopSelf();
    }

    private Notification createNotification() {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            String channelId = "SmartDocs Server";
            NotificationChannel channel = notificationManager.getNotificationChannel(channelId);
            if (channel == null) {
                channel = new NotificationChannel(channelId,
                        "SmartDocs Server channel", NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription("SmartDocs Server channel");
                channel.enableLights(true);
                channel.setLightColor(Color.RED);
                channel.enableVibration(true);
                notificationManager.createNotificationChannel(channel);
            }
            builder = new Notification.Builder(this, channelId);
        } else {
            builder = new Notification.Builder(this);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        return builder
                .setContentTitle("Endless Service")
                .setContentText("This is your favorite endless service working")
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker("Ticker text")
                .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
                .build();
    }

    private void startServer() {
        updateStatus(STATUS_STARTING);
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(0);
        } catch (IOException e) {
            stopSelf();
            return;
        }

        server = new ServerLoop(this, serverSocket);
        server.setErrorListener(error -> {
            server = null;
            updateStatus(STATUS_ERROR);
            requestServerStop();
        });
        server.start();

        NsdServiceInfo serviceInfo = SmartDocsNsd.serviceInfo(serverSocket.getLocalPort());
        nsdManager = (NsdManager) getSystemService(NSD_SERVICE);
        nsdManager.registerService(serviceInfo, PROTOCOL_DNS_SD, this);
    }

    private void requestServerStop() {
        if (status == STATUS_STARTED || status == STATUS_ERROR) {
            updateStatus(STATUS_STOPPING);
            nsdManager.unregisterService(this);
        }
    }

    private void updateStatus(int status) {
        Log.i("SmartDocs", format("Server status change %d -> %d", this.status, status));
        this.status = status;
        broadcastStatus();
    }

    private void broadcastStatus() {
        Intent intent = new Intent(ACTION_BROADCAST_STATUS);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_STATUS, status);
        sendBroadcast(intent);
    }
}