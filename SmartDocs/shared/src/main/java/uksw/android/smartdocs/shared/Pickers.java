package uksw.android.smartdocs.shared;

import static uksw.android.smartdocs.shared.Dialogs.alert;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;

public interface Pickers {
    static void create(Activity activity, Uri initialUri, int requestCode) {
        picker(activity, Intent.ACTION_CREATE_DOCUMENT, BaseProvider.DOCUMENT_MIME_TYPE, initialUri, requestCode);
    }

    static void open(Activity activity, Uri initialUri, int requestCode) {
        picker(activity, Intent.ACTION_OPEN_DOCUMENT, BaseProvider.DOCUMENT_MIME_TYPE, initialUri, requestCode);
    }

    static void picker(Activity activity, String action, String mimeType, Uri initialUri, int requestCode) {
        Intent intent = new Intent(action);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri);
            activity.startActivityForResult(intent, requestCode);
        } else {
            alert(activity, R.string.legacy_dialog, R.string.legacy_dialog_msg,
                    () -> activity.startActivityForResult(intent, requestCode));
        }
    }
}
