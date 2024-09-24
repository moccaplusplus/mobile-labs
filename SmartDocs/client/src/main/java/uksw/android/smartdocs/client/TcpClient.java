package uksw.android.smartdocs.client;

import static uksw.android.smartdocs.shared.TcpSession.closeSilent;

import android.os.Handler;

import androidx.core.util.Consumer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import uksw.android.smartdocs.shared.TcpSession;

public class TcpClient implements AutoCloseable {
    public interface SessionCallback {
        void handleSession(DataOutputStream out, DataInputStream in) throws Exception;
    }

    private final Handler errorHandler;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final Set<TcpSession> activeSessions = Collections.synchronizedSet(new HashSet<>());
    private final Callable<Socket> socketFactory;

    public TcpClient(Handler errorHandler, Callable<Socket> socketFactory) {
        this.errorHandler = errorHandler;
        this.socketFactory = socketFactory;
    }

    public void session(SessionCallback sessionCallback, Consumer<Exception> errorListener) {
        threadPool.submit(() -> {
            TcpSession session = null;
            try {
                session = new TcpSession(socketFactory.call());
                activeSessions.add(session);
                sessionCallback.handleSession(session.out, session.in);
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
        for (TcpSession socket : new ArrayList<>(activeSessions)) {
            closeSilent(socket);
        }
        activeSessions.clear();
    }
}
