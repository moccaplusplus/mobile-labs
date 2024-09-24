package uksw.android.smartdocs.shared;

public interface ThrowingConsumer<T, E extends Exception> {
    void accept(T obj) throws E;
}
