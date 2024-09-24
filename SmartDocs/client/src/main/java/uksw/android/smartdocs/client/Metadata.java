package uksw.android.smartdocs.client;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collection;

public class Metadata {
    public static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(context.getPackageName() + "_metadata", MODE_PRIVATE);
    }

    public static Metadata get(Context context) {
        return new Metadata(getPrefs(context));
    }

    private final SharedPreferences prefs;

    public Metadata(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public void createEntry(String displayName) {

    }

    public void markDirty(String displayName, boolean flag) {

    }

    public void markRemoved(String name, boolean b) {

    }

    public boolean isLoaded(String name) {
        return false;
    }

    public void setLoadedVersion(String name, long timestamp) {

    }

    public void removeEntry(String name) {

    }

    public boolean isDirty(String displayName) {
        return false;
    }

    public Collection<String> getDirty() {
        return null;
    }
}
