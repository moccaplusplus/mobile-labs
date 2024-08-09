package uksw.android.smartdocs.client.async;

import android.os.Handler;

import androidx.core.util.Consumer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

public class AsyncResult<T> {
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    private final Collection<Consumer<T>> successCallbacks = new ArrayList<>();
    private final Collection<Consumer<Exception>> errorCallbacks = new ArrayList<>();
    private final Handler callbackHandler;
    private T result;
    private Exception error;

    public AsyncResult(Handler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }

    public boolean isFinished() {
        return countDownLatch.getCount() == 0;
    }

    public void waitUntilFinished() throws InterruptedException {
        countDownLatch.await();
    }

    public boolean isError() {
        return error != null;
    }

    public Exception getError() {
        return error;
    }

    public T getResult() {
        return result;
    }

    public T get() throws Exception {
        countDownLatch.await();
        if (error != null) {
            throw error;
        }
        return result;
    }

    public void onSuccess(Consumer<T> callback) {
        if (isFinished()) {
            if (!isError()) {
                callbackHandler.post(() -> callback.accept(result));
            }
        } else {
            synchronized (successCallbacks) {
                successCallbacks.add(callback);
            }
        }
    }

    public void onError(Consumer<Exception> callback) {
        if (isFinished()) {
            if (isError()) {
                callbackHandler.post(() -> callback.accept(error));
            }
        } else {
            synchronized (errorCallbacks) {
                errorCallbacks.add(callback);
            }
        }
    }

    public void setResult(T result) {
        if (isFinished()) {
            return;
        }
        this.result = result;
        countDownLatch.countDown();
        Collection<Consumer<T>> callbacks;
        synchronized (successCallbacks) {
            callbacks = new ArrayList<>(successCallbacks);
            successCallbacks.clear();
        }
        for (Consumer<T> callback : callbacks) {
            callbackHandler.post(() -> callback.accept(result));
        }
    }

    public void setError(Exception error) {
        if (isFinished()) {
            return;
        }
        this.error = new ExecutionException(error);
        countDownLatch.countDown();
        Collection<Consumer<Exception>> callbacks;
        synchronized (errorCallbacks) {
            callbacks = new ArrayList<>(errorCallbacks);
            errorCallbacks.clear();
        }
        for (Consumer<Exception> callback : callbacks) {
            callbackHandler.post(() -> callback.accept(error));
        }
    }
}
