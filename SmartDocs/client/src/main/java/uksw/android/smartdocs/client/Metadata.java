package uksw.android.smartdocs.client;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class Metadata {
    public static SharedPreferences getPrefs(Context context, String suffix) {
        return context.getSharedPreferences(context.getPackageName() + suffix, MODE_PRIVATE);
    }

    public static Metadata get(Context context) {
        return new Metadata(getPrefs(context, "_meta_loaded"), getPrefs(context, "_meta_dirty"));
    }

    private final SharedPreferences loaded;
    private final SharedPreferences dirty;

    public Metadata(SharedPreferences loaded, SharedPreferences dirty) {
        this.loaded = loaded;
        this.dirty = dirty;
    }

    public void removeEntry(String name) {
        unmarkDirty(name);
        loaded.edit().remove(name).apply();
    }

    public void markDirty(String name) {
        dirty.edit().putBoolean(name, true).apply();
    }

    public void unmarkDirty(String name) {
        dirty.edit().remove(name).apply();
    }

    public long getLoadedVersion(String name) {
        return loaded.getLong(name, -1L);
    }

    public void setLoadedVersion(String name, long timestamp) {
        loaded.edit().putLong(name, timestamp).apply();
    }

    public Set<String> getDirtyEntries() {
        return new HashSet<>(dirty.getAll().keySet());
    }
}
