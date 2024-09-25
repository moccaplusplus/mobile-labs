package uksw.android.smartdocs.client;

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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
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
    private final BinderImpl binder = new BinderImpl(this);
    private final Collection<StatusListener> statusListeners = new LinkedHashSet<>();
    private Metadata metadata;
    private File baseDir;
    private UdpClient udpClient;
    private TcpClient tcpClient;

    @Override
    public void onCreate() {
        super.onCreate();
        metadata = Metadata.get(this);
        baseDir = new File(getFilesDir(), ClientProvider.ROOT);
        udpClient = new UdpClient(this, uiHandler,
                this::onHandshake, this::applyUpdate, this::onUdpError);
        tcpClient = new TcpClient(
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
        tcpClient.close();
        tcpClient = null;
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
        tcpClient.session(
                (out, in) -> {
                    out.writeByte(MSG_SYNC_REQ);
                    Set<String> dirtyEntries = metadata.getDirtyEntries();
                    out.writeInt(dirtyEntries.size());
                    for (String name : dirtyEntries) {
                        File file = new File(baseDir, name);
                        FileInfo.writeFileWithContents(out, file);
                    }

                    int type = in.readByte();
                    if (type == MSG_OK) {
                        for (String entry : dirtyEntries) {
                            metadata.unmarkDirty(entry);
                        }
                        int count = in.readInt();
                        Set<String> remoteFiles = new HashSet<>();
                        while (count-- > 0) {
                            FileInfo item = FileInfo.read(in);
                            File file = new File(baseDir, item.name);
                            if (file.exists() || file.createNewFile()) {
                                if (file.setLastModified(item.lastModified)) {
                                    notifyFileChange(file);
                                }
                            }
                            remoteFiles.add(item.name);
                        }

                        String[] localFiles = baseDir.list();
                        if (localFiles != null) {
                            for (String name : localFiles) {
                                File file = new File(baseDir, name);
                                if (!remoteFiles.contains(name)) {
                                    if (file.delete()) {
                                        notifyFileChange(file);
                                    }
                                }
                            }
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
        tcpClient.session(
                (out, in) -> {
                    out.writeByte(MSG_GET_CONTENTS);
                    writeString(out, file.getName());

                    int type = in.readByte();
                    if (type == MSG_OK) {
                        long timestamp = in.readLong();
                        long length = in.readLong();
                        try (
                                AutoCloseOutputStream p = new AutoCloseOutputStream(descriptor);
                                FileOutputStream f = new FileOutputStream(file)) {
                            while (length-- > 0) {
                                int b = in.read();
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
        tcpClient.session(
                (out, in) -> {
                    out.writeByte(MSG_CREATE_FILE);
                    FileInfo.writeFile(out, file);

                    int type = in.readByte();
                    if (type == MSG_OK) {
                        long timestamp = in.readLong();
                        metadata.unmarkDirty(file.getName());
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
    public void updateFile(File file) {
        tcpClient.session(
                (out, in) -> {
                    out.writeByte(MSG_PUSH_FILE);
                    FileInfo.writeFileWithContents(out, file);

                    int type = in.readByte();
                    if (type == MSG_OK) {
                        long timestamp = in.readLong();
                        metadata.unmarkDirty(file.getName());
                        file.setLastModified(timestamp);
                    } else if (type == MSG_CONFLICT) {
                        FileInfo fileInfo = FileInfo.read(in);
                        FileInfo.appendConflictInfo(in, fileInfo, file);
                        notifyFileChange(file);
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
        tcpClient.session(
                (out, in) -> {
                    out.writeByte(MSG_REMOVE_FILE);
                    FileInfo.writeRemoved(out, file.getName(), metadata.getDirtyTimestamp(file.getName()));

                    int type = in.readByte();
                    if (type == MSG_OK) {
                        metadata.removeEntry(file.getName());
                    } else if (type == Tcp.MSG_CONFLICT) {
                        long timestamp = in.readLong();
                        file.createNewFile();
                        metadata.unmarkDirty(file.getName());
                        file.setLastModified(timestamp);
                        notifyFileChange(file);
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
    private void applyUpdate(FileInfo update) {
        File file = new File(baseDir, update.name);
        if (update.length == -1) {
            if (!file.exists() || file.delete()) {
                metadata.removeEntry(file.getName());
                notifyFileChange(file);
            }
        } else {
            if (file.exists()) {
                if (update.lastModified > file.lastModified()) {
                    file.setLastModified(update.lastModified);
                    notifyFileChange(file);
                }
            } else {
                try {
                    file.createNewFile();
                    file.setLastModified(update.lastModified);
                    notifyFileChange(file);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void notifyFileChange(File file) {
        Uri uri = DocumentsContract.buildDocumentUri(ClientProvider.AUTHORITY, ClientProvider.ROOT + ':' + file.getName());
        getContentResolver().notifyChange(uri, null);
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
