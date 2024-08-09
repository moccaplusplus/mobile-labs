package uksw.android.smartdocs.client;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {
    public static final String KEY_HOST = "host";
    public static Settings get(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", MODE_PRIVATE);
        return new Settings(prefs);
    }

    private final SharedPreferences prefs;

    public Settings(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public String getHost() {
        return prefs.getString(KEY_HOST, "");
    }

    public boolean setHost(String host) {
        return prefs.edit().putString(KEY_HOST, host).commit();
    }
}
