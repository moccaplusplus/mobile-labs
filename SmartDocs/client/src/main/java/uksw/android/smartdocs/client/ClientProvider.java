package uksw.android.smartdocs.client;

import static android.content.Context.BIND_AUTO_CREATE;
import static android.os.ParcelFileDescriptor.MODE_READ_ONLY;
import static android.os.ParcelFileDescriptor.MODE_WRITE_ONLY;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;

import uksw.android.smartdocs.shared.BaseProvider;

public class ClientProvider extends BaseProvider implements ServiceConnection {
    public static final String AUTHORITY = "uksw.android.smartdocs.client";
    public static final String ROOT = "SmartDocsClient";
    public static final Uri ROOT_DOCUMENT_URI = DocumentsContract.buildDocumentUri(AUTHORITY, ROOT);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private ClientService clientService;
    private Metadata metadata;

    public ClientProvider() {
        super(ROOT, DocumentsContract.Root.FLAG_SUPPORTS_CREATE,
                DocumentsContract.Document.FLAG_SUPPORTS_WRITE | DocumentsContract.Document.FLAG_SUPPORTS_DELETE,
                R.mipmap.ic_launcher);
    }

    @Override
    public boolean onCreate() {
        if (super.onCreate()) {
            Context context = getContext();
            if (context != null) {
                metadata = Metadata.get(context);
                Intent intent = new Intent(context.getApplicationContext(), ClientService.class);
                context.bindService(intent, this, BIND_AUTO_CREATE);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        clientService = ((ClientService.BinderImpl) service).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        clientService = null;
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, @Nullable CancellationSignal signal) throws FileNotFoundException {
        try {
            File file = getFileForDocId(documentId);
            String displayName = file.getName();
            boolean isRead = mode.indexOf('w') == -1;
            if (isRead) {
                if (metadata.getLoadedVersion(displayName) == file.lastModified()) {
                    return ParcelFileDescriptor.open(file, MODE_READ_ONLY);
                }
                if (isConnected()) {
                    ParcelFileDescriptor[] pair = ParcelFileDescriptor.createReliablePipe();
                    clientService.loadFile(file, pair[1]);
                    return pair[0];
                }
                throw new FileNotFoundException("Server disconnected and dat ais not pre-fetched yet.");
            }
            // write
            return ParcelFileDescriptor.open(file, MODE_WRITE_ONLY, handler, e -> {
                if (e == null) {
                    metadata.markDirty(displayName);
                    metadata.setLoadedVersion(displayName, file.lastModified());
                    if (isConnected()) {
                        clientService.updateFile(file);
                    }
                }
            });
        } catch (FileNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public String createDocument(String parentDocumentId, String mimeType, String displayName) throws FileNotFoundException {
        File file = new File(baseDir, displayName);
        try {
            if (file.createNewFile()) {
                if (file.setWritable(true) && file.setReadable(true)) {
                    metadata.setLoadedVersion(file.getName(), file.lastModified());
                    metadata.markDirty(file.getName());
                    if (isConnected()) {
                        clientService.createFile(file);
                    }
                    return getDocIdForFile(file);
                } else {
                    file.delete();
                }
            }
        } catch (FileNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new FileNotFoundException(e.getMessage());
        }
        throw new FileNotFoundException("Cannot create file");
    }

    @Override
    public void deleteDocument(String documentId) throws FileNotFoundException {
        File file = getFileForDocId(documentId);
        if (file.exists() && file.delete()) {
            metadata.markDirty(file.getName());
            if (isConnected()) {
                clientService.removeFile(file);
            }
        }
        throw new FileNotFoundException();
    }

    private boolean isConnected() {
        return clientService != null && clientService.isConnected();
    }
}
