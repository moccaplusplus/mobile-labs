package uksw.android.smartdocs.shared;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class UiHelper {
    private static final class MainHandlerHolder {
        static final Handler mainHandler = new Handler(Looper.getMainLooper());
    }

    public static boolean isUiThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

    public static void runOnUiThread(Runnable runnable) {
        if (isUiThread()) {
            runnable.run();
        } else {
            MainHandlerHolder.mainHandler.post(runnable);
        }
    }

    public static void toast(Context context, String text) {
        Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        runOnUiThread(toast::show);
    }
}
