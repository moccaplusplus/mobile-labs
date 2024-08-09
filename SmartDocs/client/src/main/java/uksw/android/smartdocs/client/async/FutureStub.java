package uksw.android.smartdocs.client.async;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class FutureStub<T> implements Future<T> {
    private final T result;
    private final Exception exception;

    public FutureStub(T result) {
        this.result = result;
        this.exception = null;
    }

    public FutureStub(Exception exception) {
        this.result = null;
        this.exception = exception;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return exception instanceof CancellationException;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public T get() throws ExecutionException {
        if (exception != null) {
            throw new ExecutionException(exception);
        }
        return result;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws ExecutionException {
        return get();
    }
}
