package uksw.android.smartdocs.shared;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {
    public static final String KEY_UDP_SERVER_PORT = "UDP_SERVER_PORT";

    public static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(context.getPackageName() + "_preferences", MODE_PRIVATE);
    }

    public static Settings get(Context context) {
        return new Settings(getPrefs(context));
    }

    private final SharedPreferences prefs;

    public Settings(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public int getUdpServerPort() {
        return prefs.getInt(KEY_UDP_SERVER_PORT, Udp.DEFAULT_PORT);
    }

    public void setUdpServerPort(int port) {
        prefs.edit().putInt(KEY_UDP_SERVER_PORT, port).apply();
    }
}
