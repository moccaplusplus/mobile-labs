package uksw.android.smartdocs.client;

import android.os.Handler;

import androidx.core.util.Consumer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpSessions implements AutoCloseable {
    private static final int TIMEOUT_MILLIS = 7500;

    public interface SessionHandler {
        void handleSession(DataOutputStream out, DataInputStream in) throws Exception;
    }

    public static class Session implements AutoCloseable {
        final Socket socket;
        final DataOutputStream out;
        final DataInputStream in;

        public Session(Socket socket) throws IOException {
            this.socket = socket;
            socket.setSoTimeout(TIMEOUT_MILLIS);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        }

        @Override
        public void close() {
            closeSilent(out);
            closeSilent(in);
            closeSilent(socket);
        }
    }


    private final Handler errorHandler;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final Set<Session> activeSessions = Collections.synchronizedSet(new HashSet<>());
    private final Callable<Socket> socketFactory;

    public TcpSessions(Handler errorHandler, Callable<Socket> socketFactory) {
        this.errorHandler = errorHandler;
        this.socketFactory = socketFactory;
    }

    public void session(SessionHandler handler, Consumer<Exception> errorListener) {
        threadPool.submit(() -> {
            Session session = null;
            try {
                session = new Session(socketFactory.call());
                activeSessions.add(session);
                handler.handleSession(session.out, session.in);
            } catch (Exception e) {
                if (!Thread.currentThread().isInterrupted()) {
                    errorHandler.post(() -> errorListener.accept(e));
                }
            } finally {
                if (session != null) {
                    closeSilent(session);
                    activeSessions.remove(session);
                }
            }
        });
    }

    @Override
    public void close() {
        threadPool.shutdownNow();
        for (Session socket : new ArrayList<>(activeSessions)) {
            closeSilent(socket);
        }
        activeSessions.clear();
    }

    static void closeSilent(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }
}
