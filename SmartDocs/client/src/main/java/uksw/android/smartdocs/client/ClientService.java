package uksw.android.smartdocs.client;

import static uksw.android.smartdocs.shared.Tcp.MSG_CREATE_FILE;
import static uksw.android.smartdocs.shared.Tcp.MSG_ERROR;
import static uksw.android.smartdocs.shared.Tcp.MSG_GET_CONTENTS;
import static uksw.android.smartdocs.shared.Tcp.MSG_OK;
import static uksw.android.smartdocs.shared.Tcp.MSG_REMOVE_FILE;
import static uksw.android.smartdocs.shared.Tcp.MSG_SEND_FILE;
import static uksw.android.smartdocs.shared.Tcp.MSG_SYNC_REQ;
import static uksw.android.smartdocs.shared.Tcp.readString;
import static uksw.android.smartdocs.shared.Tcp.writeString;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.AutoCloseOutputStream;
import android.provider.DocumentsContract;
import android.util.Pair;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CancellationException;

import uksw.android.smartdocs.shared.FileInfo;
import uksw.android.smartdocs.shared.Tcp;

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
    private final Metadata metadata = Metadata.get(this);
    private final File baseDir = new File(getFilesDir(), ClientProvider.ROOT);
    private final BinderImpl binder = new BinderImpl(this);
    private final Collection<StatusListener> statusListeners = new LinkedHashSet<>();
    private UdpClient udpClient;
    private TcpSessions tcpSessions;

    @Override
    public void onCreate() {
        super.onCreate();
        udpClient = new UdpClient(this, uiHandler,
                this::onHandshake, this::applyUpdate, this::onUdpError);
        tcpSessions = new TcpSessions(
                uiHandler,
                () -> {
                    udpClient.ensureHandshake();
                    return new Socket(udpClient.getHost(), udpClient.getTcpPort());
                });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        udpClient.stop();
        udpClient = null;
        tcpSessions.close();
        tcpSessions = null;
        statusListeners.clear();
        super.onDestroy();
    }

    public boolean isConnected() {
        return udpClient.isConnected();
    }

    public int getConnectionStatus() {
        return udpClient.isConnected() ?
                STATUS_CONNECTED : udpClient.isConnecting() ?
                STATUS_CONNECTING : STATUS_DISCONNECTED;
    }

    public void addStatusListener(StatusListener discoveryListener) {
        discoveryListener.onConnectionStatus(getConnectionStatus());
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
        tcpSessions.session(
                (out, in) -> {
                    out.writeByte(MSG_SYNC_REQ);
                    Collection<String> dirtyEntries = metadata.getDirty();
                    out.writeInt(dirtyEntries.size());
                    for (String name : dirtyEntries) {
                        File file = new File(baseDir, name);
                        FileInfo.writeFileWithContents(out, file);
                    }

                    int type = in.readByte();
                    if (type == MSG_OK) {
                        for (String entry : dirtyEntries) {
                            metadata.markDirty(entry, false);
                        }

                        int count = in.readInt();
                        if (count > 0) {
                            List<FileInfo> update = new ArrayList<>(count);
                            while (count-- > 0) {
                                update.add(FileInfo.read(in));
                            }
                            applyUpdate(update);
                        }

                    } else if (type == MSG_ERROR) {
                        throw new IOException(readString(in));
                    } else {
                        throw new IOException("Unknown error");
                    }
                },
                e -> showToast(e.getMessage()));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void loadFile(File file, ParcelFileDescriptor descriptor) {
        tcpSessions.session(
                (out, in) -> {
                    out.writeByte(MSG_GET_CONTENTS);
                    writeString(out, file.getName());

                    int type = in.readByte();
                    if (type == MSG_OK) {
                        long timestamp = in.readLong();
                        try (
                                AutoCloseOutputStream p = new AutoCloseOutputStream(descriptor);
                                FileOutputStream f = new FileOutputStream(file)) {
                            int b;
                            while ((b = in.read()) != -1) {
                                f.write(b);
                                p.write(b);
                            }
                        }
                        file.setLastModified(timestamp);
                        metadata.setLoadedVersion(file.getName(), timestamp);
                    } else if (type == MSG_ERROR) {
                        throw new IOException(readString(in));
                    } else {
                        throw new IOException("Unknown error");
                    }
                },
                e -> {
                    try {
                        descriptor.closeWithError(e.getMessage());
                    } catch (IOException ignored) {
                    }
                    showToast(e.getMessage());
                });
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void createFile(File file) {
        tcpSessions.session(
                (out, in) -> {
                    out.writeByte(MSG_CREATE_FILE);
                    out.writeLong(file.lastModified());
                    writeString(out, file.getName());

                    int type = in.readByte();
                    if (type == MSG_OK) {
                        long timestamp = in.readLong();
                        metadata.markDirty(file.getName(), false);
                        file.setLastModified(timestamp);
                    } else if (type == MSG_ERROR) {
                        throw new IOException(readString(in));
                    } else {
                        throw new IOException("Unknown error");
                    }
                },
                e -> showToast(e.getMessage()));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void sendFile(File file) {
        tcpSessions.session(
                (out, in) -> {
                    out.writeByte(MSG_SEND_FILE);
                    out.writeLong(file.lastModified());
                    writeString(out, file.getName());

                    int type = in.readByte();
                    if (type == MSG_OK) {
                        long timestamp = in.readLong();
                        metadata.markDirty(file.getName(), false);
                        file.setLastModified(timestamp);
                    } else if (type == MSG_ERROR) {
                        throw new IOException(readString(in));
                    } else {
                        throw new IOException("Unknown error");
                    }
                },
                e -> showToast(e.getMessage()));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void removeFile(File file) {
        tcpSessions.session(
                (out, in) -> {
                    out.writeByte(MSG_REMOVE_FILE);
                    writeString(out, file.getName());

                    int type = in.readByte();
                    if (type == MSG_OK) {
                        metadata.removeEntry(file.getName());
                    } else if (type == Tcp.MSG_CONFLICT) {
                        long timestamp = in.readLong();
                        file.createNewFile();
                        metadata.markRemoved(file.getName(), false);
                        file.setLastModified(timestamp);
                    } else if (type == MSG_ERROR) {
                        throw new IOException(readString(in));
                    } else {
                        throw new IOException("Unknown error");
                    }
                },
                e -> showToast(e.getMessage()));
    }

    private void onHandshake(Pair<InetAddress, Integer> hostAndPort) {
        showToast("Server Connected: " + hostAndPort.first + ":" + hostAndPort.second);
        notifyConnectionStatusChange(STATUS_CONNECTED);
        requestSync();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void applyUpdate(Collection<FileInfo> update) {
        for (FileInfo item : update) {
            File file = new File(baseDir, item.name);
            if (item.lastModified == -1) {
                if (!file.exists() || file.delete()) {
                    metadata.removeEntry(file.getName());
                }
            } else {
                if (file.exists()) {
                    if (item.lastModified > file.lastModified()) {
                        file.setLastModified(item.lastModified);
                    }
                } else {
                    try {
                        file.createNewFile();
                        file.setLastModified(item.lastModified);
                        metadata.createEntry(item.name);
                    } catch (IOException ignored) {
                    }
                }
            }
            Uri uri = DocumentsContract.buildDocumentUri(ClientProvider.AUTHORITY, item.name);
            getContentResolver().notifyChange(uri, null);
        }
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

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
