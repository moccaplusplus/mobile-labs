package uksw.android.smartdocs.server;

import static java.lang.String.format;
import static uksw.android.smartdocs.shared.Net.getLocalAddress;
import static uksw.android.smartdocs.shared.UiHelper.runOnUiThread;

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
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import uksw.android.smartdocs.shared.Toasts;
import uksw.android.smartdocs.shared.UiHelper;

public class ServerService extends Service {
    public static final String ACTION_REQUEST_STATUS = "action.smart-docs.request.status";
    public static final String ACTION_BROADCAST_STATUS = "action.smart-docs.broadcast.status";
    public static final String EXTRA_STATUS = "status";
    public static final String EXTRA_STATUS_MSG = "status-msg";
    public static final int STATUS_STARTED = 1;
    public static final int STATUS_STOPPED = 0;
    public static final int STATUS_ERROR = 2;

    private final BroadcastReceiver serverReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_REQUEST_STATUS.equals(intent.getAction())) {
                broadcastStatus();
            }
        }
    };
    private TcpServer tcpServer;
    private UdpServer udpServer;
    private int status = STATUS_STOPPED;
    private String statusInfo;

    @Override
    public void onCreate() {
        super.onCreate();
        int foregroundType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC : 0;
        ServiceCompat.startForeground(this, 1, createNotification(), foregroundType);
        IntentFilter filter = new IntentFilter(ACTION_REQUEST_STATUS);
        ContextCompat.registerReceiver(
                this, serverReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (status == STATUS_STOPPED) {
            try {
                tcpServer = new TcpServer(this::clientHandler, this::onServerError);
                tcpServer.start();

                String hostAndPort = getHost() + ":" + tcpServer.getPort();
                udpServer = new UdpServer(hostAndPort, this::onServerError);
                udpServer.start();

                updateStatus(STATUS_STARTED, "Running at " + hostAndPort);
            } catch (IOException e) {
                onServerError(e);
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (tcpServer != null) {
            tcpServer.stop();
            tcpServer = null;
        }
        if (udpServer != null) {
            udpServer.stop();
            udpServer = null;
        }
        updateStatus(STATUS_STOPPED, "Stopped");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void clientHandler(Socket socket) {
        // TODO
    }

    private void updateStatus(int status, String statusInfo) {
        Log.i("SmartDocs", format("Server status change %d -> %d", this.status, status));
        this.status = status;
        this.statusInfo = statusInfo;
        Toast toast = Toast.makeText(this, getString(R.string.status_toast, statusInfo), Toast.LENGTH_SHORT);
        runOnUiThread(toast::show);
        broadcastStatus();
    }

    private void broadcastStatus() {
        Intent intent = new Intent(ACTION_BROADCAST_STATUS);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_STATUS, status);
        intent.putExtra(EXTRA_STATUS_MSG, statusInfo);
        sendBroadcast(intent);
    }

    private void onServerError(Exception error) {
        updateStatus(STATUS_ERROR, "Error " + error.getMessage());
        stopSelf();
    }

    private Notification createNotification() {
        Notification.Builder builder;
        String name = "SmartDocs Server";
        String description = "SmartDocs Server is running in background";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            String channelId = "SmartDocs Server";
            NotificationChannel channel = notificationManager.getNotificationChannel(channelId);
            if (channel == null) {
                channel = new NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription(description);
                channel.enableLights(true);
                channel.setLightColor(Color.RED);
                channel.enableVibration(true);
                notificationManager.createNotificationChannel(channel);
            }
            builder = new Notification.Builder(this, channelId);
        } else {
            builder = new Notification.Builder(this);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        return builder
                .setContentTitle(name)
                .setContentText(description)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
                .build();
    }

    private String getHost() throws UnknownHostException {
        return getLocalAddress(this).getHostAddress();
    }
}