package uksw.android.smartdocs.shared;

import android.app.AlertDialog;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public class Dialogs {
    public static void alert(Context context, @StringRes int title) {
        alert(context, context.getString(title), null);
    }

    public static void alert(Context context, @StringRes int title, @StringRes int message) {
        alert(context, context.getString(title), context.getString(message));
    }

    public static void alert(Context context, @StringRes int title, @StringRes int message, @NonNull Runnable onClose) {
        alert(context, context.getString(title), context.getString(message), onClose);
    }

    public static void alert(@NonNull Context context, @NonNull String title, @Nullable String message) {
        alert(context, title, message, () -> {
        });
    }

    public static void alert(@NonNull Context context, @NonNull String title, @Nullable String message, @NonNull Runnable onClose) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setCancelable(true)
                .setNegativeButton(R.string.ok, (dialog, which) -> dialog.cancel())
                .setMessage(message)
                .setOnCancelListener(dialog -> onClose.run())
                .show();
    }

    public static void confirm(@NonNull Context context, @StringRes int title, @StringRes int message, @NonNull Runnable onConfirm) {
        confirm(context, context.getString(title), context.getString(message), onConfirm);
    }

    public static void confirm(@NonNull Context context, @NonNull String title, @Nullable String message, @NonNull Runnable onConfirm) {
        confirm(context, title, message, onConfirm, () -> {
        });
    }

    public static void confirm(@NonNull Context context, @NonNull String title, @Nullable String message, @NonNull Runnable onConfirm, @NonNull Runnable onReject) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setCancelable(true)
                .setNegativeButton(R.string.no, (dialog, which) -> dialog.cancel())
                .setPositiveButton(R.string.yes, (dialog, which) -> onConfirm.run())
                .setMessage(message)
                .setOnCancelListener(dialog -> onReject.run())
                .show();
    }
}
