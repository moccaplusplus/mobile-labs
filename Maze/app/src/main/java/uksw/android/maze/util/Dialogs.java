package uksw.android.maze.util;

import android.app.AlertDialog;
import android.content.Context;

import androidx.annotation.StringRes;

import uksw.android.maze.R;

public class Dialogs {
    public static void alert(Context context, @StringRes int title) {
        builder(context, context.getString(title), null).show();
    }

    public static void alert(Context context, @StringRes int title, @StringRes int message) {
        builder(context, context.getString(title), context.getString(message)).show();
    }

    public static void confirm(Context context, @StringRes int title, @StringRes int message, Runnable onConfirm) {
        builder(context, context.getString(title), context.getString(message))
                .setNegativeButton(R.string.no, (dialog, which) -> dialog.cancel())
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    onConfirm.run();
                    dialog.dismiss();
                })
                .show();
    }

    private static AlertDialog.Builder builder(Context context, String title, String message) {
        return new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(true)
                .setNegativeButton(R.string.close, (dialog, which) -> dialog.cancel());
    }
}
