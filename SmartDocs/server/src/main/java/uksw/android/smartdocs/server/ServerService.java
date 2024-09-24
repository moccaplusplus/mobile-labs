package uksw.android.smartdocs.server;

import static java.lang.String.format;
import static uksw.android.smartdocs.shared.FileInfo.readContents;
import static uksw.android.smartdocs.shared.FileInfo.writeContents;
import static uksw.android.smartdocs.shared.Inet.getBroadcastAddresses;
import static uksw.android.smartdocs.shared.Inet.getLocalAddress;
import static uksw.android.smartdocs.shared.Tcp.MSG_CONFLICT;
import static uksw.android.smartdocs.shared.Tcp.MSG_CREATE_FILE;
import static uksw.android.smartdocs.shared.Tcp.MSG_ERROR;
import static uksw.android.smartdocs.shared.Tcp.MSG_GET_CONTENTS;
import static uksw.android.smartdocs.shared.Tcp.MSG_OK;
import static uksw.android.smartdocs.shared.Tcp.MSG_PUSH_FILE;
import static uksw.android.smartdocs.shared.Tcp.MSG_REMOVE_FILE;
import static uksw.android.smartdocs.shared.Tcp.MSG_SYNC_REQ;
import static uksw.android.smartdocs.shared.Tcp.readString;
import static uksw.android.smartdocs.shared.Tcp.writeString;

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
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import uksw.android.smartdocs.shared.FileInfo;

public class ServerService extends Service {
    public static final String ACTION_REQUEST_STATUS = "action.smart-docs.request.status";
    public static final String ACTION_BROADCAST_STATUS = "action.smart-docs.broadcast.status";
    public static final String EXTRA_STATUS = "status";
    public static final String EXTRA_STATUS_MSG = "status-msg";
    public static final String EXTRA_UDP_PORT = "udp-port";
    public static final int STATUS_STARTED = 1;
    public static final int STATUS_STARTING = 2;
    public static final int STATUS_STOPPED = 0;
    public static final int STATUS_ERROR = -1;

    private static final int FOREGROUND_ID = 12345;

    private final BroadcastReceiver serverReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_REQUEST_STATUS.equals(intent.getAction())) {
                broadcastStatus();
            }
        }
    };
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final File baseDir = new File(getFilesDir(), ServerProvider.ROOT);
    private TcpServer tcpServer;
    private UdpServer udpServer;
    private int status = STATUS_STOPPED;
    private String statusInfo;

    @Override
    public void onCreate() {
        super.onCreate();
        int foregroundType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC : 0;
        ServiceCompat.startForeground(this, FOREGROUND_ID, createNotification(), foregroundType);
        IntentFilter filter = new IntentFilter(ACTION_REQUEST_STATUS);
        ContextCompat.registerReceiver(
                this, serverReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (status == STATUS_STOPPED) {
            updateStatus(STATUS_STARTING, "Starting server...");
            int udpServerPort = intent.getIntExtra(EXTRA_UDP_PORT, 0);
            try {
                List<InetAddress> broadcastAddresses = getBroadcastAddresses(getLocalAddress(this));
                tcpServer = new TcpServer(this::sessionCallback, this::onServerError);
                tcpServer.start();

                udpServer = new UdpServer(udpServerPort, tcpServer.getPort(), broadcastAddresses, this::onServerError);
                udpServer.start();

                updateStatus(STATUS_STARTED, "Server is running");
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
        updateStatus(STATUS_STOPPED, "Server is stopped");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sessionCallback(DataOutputStream out, DataInputStream in) {
        try {
            int type = in.readByte();
            switch (type) {
                case MSG_CREATE_FILE:
                    createFile(out, in);
                    break;
                case MSG_PUSH_FILE:
                    pushFile(out, in);
                    break;
                case MSG_REMOVE_FILE:
                    removeFile(out, in);
                    break;
                case MSG_GET_CONTENTS:
                    getContents(out, in);
                    break;
                case MSG_SYNC_REQ:
                    syncRequest(out, in);
                    break;
            }
        } catch (Exception e) {
            uiHandler.post(() -> Toast.makeText(
                    this, getString(R.string.status_toast, e.getMessage()), Toast.LENGTH_SHORT).show());
            try {
                out.writeByte(MSG_ERROR);
                writeString(out, e.getMessage());
            } catch (Exception ignored) {
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void createFile(DataOutputStream out, DataInputStream in) throws Exception {
        FileInfo fileInfo = FileInfo.read(in);
        File file = new File(baseDir, fileInfo.name);
        if (file.exists()) {
            out.writeByte(MSG_OK);
            out.writeLong(file.lastModified());
        } else {
            if (file.createNewFile()) {
                file.setLastModified(fileInfo.lastModified);
                out.writeByte(MSG_OK);
                out.writeLong(fileInfo.lastModified);
                notifyFileChange(file);
            } else {
                throw new IllegalStateException("Cannot create file");
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void pushFile(DataOutputStream out, DataInputStream in) throws Exception {
        FileInfo fileInfo = FileInfo.read(in);
        File file = new File(baseDir, fileInfo.name);
        if (!file.exists() || file.lastModified() < fileInfo.lastModified) {
            readContents(in, file);
            file.setLastModified(fileInfo.lastModified);
            out.writeByte(MSG_OK);
            out.writeLong(fileInfo.lastModified);
            notifyFileChange(file);
        } else {
            // TODO: apply patch
            out.writeByte(MSG_OK);
            out.writeLong(file.lastModified());
        }
    }

    private void removeFile(DataOutputStream out, DataInputStream in) throws Exception {
        FileInfo fileInfo = FileInfo.read(in);
        File file = new File(baseDir, fileInfo.name);
        if (file.exists()) {
            if (file.lastModified() < fileInfo.lastModified) {
                if (file.delete()) {
                    out.writeByte(MSG_OK);
                    notifyFileChange(file);
                } else {
                    throw new IllegalStateException("Cannot remove file");
                }
            } else {
                out.writeByte(MSG_CONFLICT);
                out.writeLong(file.lastModified());
            }
        } else {
            out.writeByte(MSG_OK);
        }
    }

    private void getContents(DataOutputStream out, DataInputStream in) throws Exception {
        String name = readString(in);
        File file = new File(baseDir, name);
        if (!file.exists()) {
            throw new IllegalStateException("File does not exist");
        }
        out.writeByte(MSG_OK);
        out.writeLong(file.lastModified());
        writeContents(out, file);
    }

    private void syncRequest(DataOutputStream out, DataInputStream in) throws Exception {
        int count = in.readInt();
        while (count-- > 0) {
            FileInfo fileInfo = FileInfo.read(in);
            File file = new File(baseDir, fileInfo.name);
            if (file.lastModified() < fileInfo.lastModified) {
                if (!FileInfo.readContents(in, file) || file.delete()) {
                    notifyFileChange(file);
                }
            } else {
                // TODO: apply patch - ignore for now.
            }
        }
        out.writeByte(MSG_OK);
        String[] list = baseDir.list();
        if (list == null || list.length == 0) {
            out.writeInt(0);
        } else {
            out.writeInt(list.length);
            for (String name : list) {
                FileInfo.writeFile(out, new File(baseDir, name));
            }
        }
    }

    private void updateStatus(int status, String statusInfo) {
        Log.i("SmartDocs", format("Server status change %d -> %d", this.status, status));
        this.status = status;
        this.statusInfo = statusInfo;
        uiHandler.post(() -> Toast.makeText(
                this, getString(R.string.status_toast, statusInfo), Toast.LENGTH_SHORT).show());
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

    private void notifyFileChange(File file) {
        Uri uri = DocumentsContract.buildDocumentUri(ServerProvider.AUTHORITY, ServerProvider.ROOT + ':' + file.getName());
        getContentResolver().notifyChange(uri, null);
        udpServer.broadcastUpdate(file);
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
}