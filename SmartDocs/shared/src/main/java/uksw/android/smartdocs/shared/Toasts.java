package uksw.android.smartdocs.shared;

import android.content.Context;
import android.widget.Toast;

public class Toasts {

    public static Toast show(Context context, String text) {
        Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        toast.show();
        return toast;
    }
}
