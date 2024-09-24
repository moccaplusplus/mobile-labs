package uksw.android.smartdocs.server;

import android.net.Uri;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;

import androidx.annotation.Nullable;

import java.io.FileNotFoundException;

import uksw.android.smartdocs.shared.BaseProvider;

public class ServerProvider extends BaseProvider {
    public static final String AUTHORITY = "uksw.android.smartdocs.server";
    public static final String ROOT = "SmartDocsServer";
    public static final Uri ROOT_DOCUMENT_URI = DocumentsContract.buildDocumentUri(AUTHORITY, ROOT);

    public ServerProvider() {
        super(ROOT, 0, 0, R.mipmap.ic_launcher);
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, @Nullable CancellationSignal signal) throws FileNotFoundException {
        return ParcelFileDescriptor.open(getFileForDocId(documentId), ParcelFileDescriptor.MODE_READ_ONLY);
    }
}
