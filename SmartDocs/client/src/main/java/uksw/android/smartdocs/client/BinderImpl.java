package uksw.android.smartdocs.client;

import android.app.Service;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;

public class BinderImpl<T extends Service> extends Binder {
    @FunctionalInterface
    public interface Connection<T extends Service> extends ServiceConnection {
        void onServiceConnected(ComponentName name, T service);

        @SuppressWarnings("unchecked")
        @Override
        default void onServiceConnected(ComponentName name, IBinder service) {
            onServiceConnected(name, ((BinderImpl<T>) service).getService());
        }

        @Override
        default void onServiceDisconnected(ComponentName name) {
        }
    }

    private final T service;

    public BinderImpl(T service) {
        this.service = service;
    }

    public T getService() {
        return service;
    }
}
