package uksw.android.smartdocs.client;

import static android.os.Build.VERSION.SDK_INT;
import static android.provider.DocumentsContract.Document;
import static android.provider.DocumentsContract.Root;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SmartDocsProvider extends DocumentsProvider {
    public static final String DOCUMENT_MIME_TYPE = "text/plain";
    public static final String AUTHORITY = "uksw.android.smartdocs";
    public static final String ROOT = "smart-docs";
    public static final Uri ROOT_DOCUMENT_URI = DocumentsContract.buildDocumentUri(AUTHORITY, ROOT);

    private static final String[] DEFAULT_ROOT_PROJECTION = new String[]{
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_ICON,
            Root.COLUMN_TITLE,
            Root.COLUMN_FLAGS,
            Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_MIME_TYPES,
            Root.COLUMN_AVAILABLE_BYTES
    };

    private static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[]{
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_FLAGS,
            Document.COLUMN_SIZE
    };
    private static final String ROOT_MIME_TYPES = Document.MIME_TYPE_DIR + "\n" + DOCUMENT_MIME_TYPE + "\n";
    private static final int DIR_FLAGS = Document.FLAG_DIR_SUPPORTS_CREATE;
    private static final int DOC_FLAGS = Document.FLAG_SUPPORTS_WRITE | Document.FLAG_SUPPORTS_DELETE;

    private final LinkedList<String> recentDocuments = new LinkedList<>();
    private File baseDir;
    private Handler handler;

    @SuppressWarnings("DataFlowIssue")
    @Override
    public boolean onCreate() {
        Context context = getContext();
        File filesDir = context.getFilesDir();
        baseDir = new File(filesDir, "SmartDocsData");
        if (!baseDir.exists()) {
            if (!baseDir.mkdir()) {
                return false;
            }
        }
        handler = new Handler(context.getMainLooper());
        return true;
    }

    @Override
    public Cursor queryRoots(String[] projection) {
        MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_ROOT_PROJECTION);
        final MatrixCursor.RowBuilder row = result.newRow();

        row.add(Root.COLUMN_ROOT_ID, ROOT);
        row.add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE |
                Root.FLAG_SUPPORTS_RECENTS |
                Root.FLAG_SUPPORTS_IS_CHILD);
        row.add(Root.COLUMN_TITLE, "SmartDocs");
        row.add(Root.COLUMN_MIME_TYPES, ROOT_MIME_TYPES);
        row.add(Root.COLUMN_AVAILABLE_BYTES, baseDir.getFreeSpace());
        row.add(Root.COLUMN_DOCUMENT_ID, getDocIdForFile(baseDir));
        row.add(Root.COLUMN_ICON, R.mipmap.ic_launcher);
        return result;
    }

    @Override
    public DocumentsContract.Path findDocumentPath(@Nullable String parentDocumentId, String childDocumentId) throws FileNotFoundException {
        if (SDK_INT >= Build.VERSION_CODES.O) {
            return new DocumentsContract.Path(ROOT, getPath(parentDocumentId, childDocumentId));
        }
        return null;
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        final MatrixCursor result = new
                MatrixCursor(projection == null ? DEFAULT_DOCUMENT_PROJECTION : projection);
        includeFile(result, documentId, null);
        return result;
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {
        final MatrixCursor result = new
                MatrixCursor(projection == null ? DEFAULT_DOCUMENT_PROJECTION : projection);
        final File parent = getFileForDocId(parentDocumentId);

        for (File file : parent.listFiles()) {
            includeFile(result, null, file);
        }
        return result;
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, @Nullable CancellationSignal signal) throws FileNotFoundException {
        File file = getFileForDocId(documentId);
        int accessMode = ParcelFileDescriptor.parseMode(mode);
        boolean isWrite = (mode.indexOf('w') != -1);
        if (isWrite) {
            // Attach a close listener if the document is opened in write mode.
            try {
                return ParcelFileDescriptor.open(file, accessMode, handler,
                        e -> Log.i("SmartDocs", "A file with id " + documentId + " has been closed!  Time to " +
                                "update the server."));
            } catch (IOException e) {
                throw new FileNotFoundException("Failed to open document with id " + documentId +
                        " and mode " + mode);
            }
        }
        return ParcelFileDescriptor.open(file, accessMode);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public String createDocument(String parentDocumentId, String mimeType, String displayName) throws FileNotFoundException {
        File parent = getFileForDocId(parentDocumentId);
        File file = new File(parent.getPath(), displayName);
        try {
            if (Document.MIME_TYPE_DIR.equals(mimeType)) {
                if (file.mkdir()) {
                    return getDocIdForFile(file);
                }
            } else {
                if (file.createNewFile()) {
                    if (file.setWritable(true) && file.setReadable(true)) {
                        String createdDocumentId = getDocIdForFile(file);
                        addRecentDocument(createdDocumentId);
                        return createdDocumentId;
                    } else {
                        file.delete();
                    }
                }
            }
        } catch (IOException ignored) {
        }
        throw new FileNotFoundException();
    }

    @Override
    public void deleteDocument(String documentId) throws FileNotFoundException {
        File file = getFileForDocId(documentId);
        if (!file.exists() || file.delete()) {
            recentDocuments.remove(documentId);
        }
    }

    @Override
    public Cursor queryRecentDocuments(String rootId, String[] projection) throws FileNotFoundException {
        final MatrixCursor result = new
                MatrixCursor(projection == null ? DEFAULT_DOCUMENT_PROJECTION : projection);
        for (String documentId : recentDocuments) {
            includeFile(result, documentId, null);
        }
        return result;
    }

    @Override
    public boolean isChildDocument(String parentDocumentId, String documentId) {
        try {
            File parent = getFileForDocId(parentDocumentId);
            if (!parent.getAbsolutePath().startsWith(baseDir.getAbsolutePath())) {
                return false;
            }
            File file = getFileForDocId(documentId);
            return file.getAbsolutePath().startsWith(parent.getAbsolutePath());
        } catch (FileNotFoundException ignored) {
        }
        return false;
    }

    private void addRecentDocument(String documentId) {
        recentDocuments.remove(documentId);
        recentDocuments.addFirst(documentId);
        if (recentDocuments.size() > 10) {
            recentDocuments.removeLast();
        }
    }

    private void includeFile(MatrixCursor result, String documentId, File file) throws FileNotFoundException {
        if (documentId == null) {
            documentId = getDocIdForFile(file);
        } else {
            file = getFileForDocId(documentId);
        }

        final String displayName = file.getName();
        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, documentId);
        row.add(Document.COLUMN_DISPLAY_NAME, displayName);
        row.add(Document.COLUMN_SIZE, file.length());
        row.add(Document.COLUMN_LAST_MODIFIED, file.lastModified());
        row.add(Document.COLUMN_ICON, R.mipmap.ic_launcher);

        if (file.isDirectory()) {
            row.add(Document.COLUMN_FLAGS, DIR_FLAGS);
            row.add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR);
        } else {
            row.add(Document.COLUMN_FLAGS, DOC_FLAGS);
            row.add(Document.COLUMN_MIME_TYPE, DOCUMENT_MIME_TYPE);
        }
    }

    private String getDocIdForFile(File file) {
        String path = cutTrailingSlash(file.getAbsolutePath());
        String rootPath = cutTrailingSlash(baseDir.getPath());
        return rootPath.equals(path) ?
                ROOT : ROOT + '/' + path.substring(rootPath.length() + 1);
    }

    private File getFileForDocId(String documentId) throws FileNotFoundException {
        documentId = cutTrailingSlash(documentId);
        File target = baseDir;
        if (documentId.equals(ROOT)) {
            return target;
        }
        if (!documentId.startsWith(ROOT + "/")) {
            throw new FileNotFoundException("Missing root for " + documentId);
        } else {
            final String path = documentId.substring(ROOT.length() + 1);
            target = new File(target, path);
            if (!target.exists()) {
                throw new FileNotFoundException("Missing file for " + documentId + " at " + target);
            }
            return target;
        }
    }

    private List<String> getPath(@Nullable String parentDocumentId, String childDocumentId) {
        childDocumentId = cutTrailingSlash(childDocumentId);
        if (ROOT.equals(childDocumentId)) {
            return Collections.singletonList(ROOT);
        }
        parentDocumentId = parentDocumentId == null ? ROOT : cutTrailingSlash(parentDocumentId);
        int cutOffIndex = parentDocumentId.length() + 1;
        String cutOff = childDocumentId.substring(cutOffIndex);

        String[] items = cutOff.split("/");
        List<String> path = new ArrayList<>();
        String documentId = parentDocumentId;
        path.add(documentId);
        for (String item : items) {
            documentId += "/" + item;
            path.add(documentId);
        }
        return path;
    }

    private static String cutTrailingSlash(String documentId) {
        return documentId.endsWith("/") ?
                documentId.substring(documentId.length() - 1) : documentId;
    }
}