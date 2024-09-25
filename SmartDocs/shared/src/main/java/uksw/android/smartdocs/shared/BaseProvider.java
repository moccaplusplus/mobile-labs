package uksw.android.smartdocs.shared;

import static android.os.Build.VERSION.SDK_INT;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsProvider;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

public abstract class BaseProvider extends DocumentsProvider {
    public static final String DOCUMENT_MIME_TYPE = "text/plain";

    protected static final String[] DEFAULT_ROOT_PROJECTION = new String[]{
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_ICON,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            DocumentsContract.Root.COLUMN_MIME_TYPES,
            DocumentsContract.Root.COLUMN_AVAILABLE_BYTES
    };

    protected static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[]{
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_FLAGS,
            Document.COLUMN_SIZE
    };

    protected final String root;
    protected final int rootFlags;
    protected final int documentFlags;
    @DrawableRes
    protected final int iconResId;

    protected File baseDir;

    public BaseProvider(String root, int rootFlags, int documentFlags, int iconResId) {
        this.root = root;
        this.rootFlags = rootFlags;
        this.documentFlags = documentFlags;
        this.iconResId = iconResId;
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context != null) {
            File filesDir = context.getFilesDir();
            baseDir = new File(filesDir, root);
            return baseDir.exists() || baseDir.mkdir();
        }
        return false;
    }

    @Override
    public Cursor queryRoots(String[] projection) {
        MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_ROOT_PROJECTION);
        result.newRow()
                .add(DocumentsContract.Root.COLUMN_ROOT_ID, root)
                .add(DocumentsContract.Root.COLUMN_FLAGS, rootFlags)
                .add(DocumentsContract.Root.COLUMN_TITLE, root)
                .add(DocumentsContract.Root.COLUMN_MIME_TYPES, Document.MIME_TYPE_DIR + "\n" + DOCUMENT_MIME_TYPE)
                .add(DocumentsContract.Root.COLUMN_AVAILABLE_BYTES, baseDir.getFreeSpace())
                .add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, root)
                .add(DocumentsContract.Root.COLUMN_ICON, iconResId);
        return result;
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        MatrixCursor result = new MatrixCursor(projection == null ? DEFAULT_DOCUMENT_PROJECTION : projection);
        includeFile(result, documentId, getFileForDocId(documentId));
        return result;
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {
        MatrixCursor result = new MatrixCursor(projection == null ? DEFAULT_DOCUMENT_PROJECTION : projection);
        File parent = getFileForDocId(parentDocumentId);
        File[] files = parent.listFiles();
        if (files != null) {
            for (File file : files) {
                includeFile(result, getDocIdForFile(file), file);
            }
        }
        return result;
    }

    @Override
    public DocumentsContract.Path findDocumentPath(@Nullable String parentDocumentId, String childDocumentId) {
        if (SDK_INT >= Build.VERSION_CODES.O) {
            return new DocumentsContract.Path(root, getPath(childDocumentId));
        }
        return null;
    }

    protected void includeFile(MatrixCursor result, String documentId, File file) {
        MatrixCursor.RowBuilder row = result.newRow()
                .add(Document.COLUMN_DOCUMENT_ID, documentId)
                .add(Document.COLUMN_DISPLAY_NAME, file.getName())
                .add(Document.COLUMN_SIZE, file.length())
                .add(Document.COLUMN_LAST_MODIFIED, file.lastModified())
                .add(Document.COLUMN_ICON, iconResId);

        if (file.isDirectory()) {
            row.add(Document.COLUMN_FLAGS, Document.FLAG_DIR_SUPPORTS_CREATE);
            row.add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR);
        } else {
            row.add(Document.COLUMN_FLAGS, documentFlags);
            row.add(Document.COLUMN_MIME_TYPE, DOCUMENT_MIME_TYPE);
        }
    }

    protected String getDocIdForFile(File file) {
        return baseDir.equals(file) ? root : root + ':' + file.getName();
    }

    protected File getFileForDocId(String documentId) throws FileNotFoundException {
        documentId = cutTrailingSlash(documentId);
        if (documentId.equals(root)) {
            return baseDir;
        }
        if (documentId.startsWith(root + ':')) {
            final String path = documentId.substring(root.length() + 1);
            return new File(baseDir, path);
        }
        throw new FileNotFoundException("Missing root for " + documentId);
    }

    protected List<String> getPath(String childDocumentId) {
        childDocumentId = cutTrailingSlash(childDocumentId);
        return root.equals(childDocumentId) ? List.of(root) : List.of(root, childDocumentId);
    }

    protected static String cutTrailingSlash(String documentId) {
        return documentId.endsWith("/") ?
                documentId.substring(documentId.length() - 1) : documentId;
    }
}
