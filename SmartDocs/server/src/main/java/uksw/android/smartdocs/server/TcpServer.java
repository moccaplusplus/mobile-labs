package uksw.android.smartdocs.server;

import static uksw.android.smartdocs.shared.TcpSession.closeSilent;

import android.util.Log;

import androidx.core.util.Consumer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import uksw.android.smartdocs.shared.TcpSession;

public class TcpServer {
    public interface SessionCallback {
        void handleSession(DataOutputStream out, DataInputStream in);
    }

    private final ServerSocket tcpServerSocket;
    private final ExecutorService tcpClientThreadPool;
    private final Thread tcpThread;
    private final SessionCallback sessionHandler;
    private final Consumer<Exception> errorListener;

    public TcpServer(SessionCallback sessionHandler, Consumer<Exception> errorListener) throws IOException {
        this.sessionHandler = sessionHandler;
        this.errorListener = errorListener;
        tcpServerSocket = new ServerSocket(0);
        tcpClientThreadPool = Executors.newCachedThreadPool();
        tcpThread = new Thread(this::tcpLoop);
        tcpThread.setDaemon(true);
    }

    public int getPort() {
        return tcpServerSocket.getLocalPort();
    }

    public void start() {
        tcpThread.start();
    }

    public void stop() {
        try {
            tcpThread.interrupt();
            tcpServerSocket.close();
        } catch (Exception e) {
            Log.e("SmartDocs", "TCP Server socket shutdown error", e);
        }
    }

    private void tcpLoop() {
        Set<TcpSession> activeSessions = Collections.synchronizedSet(new HashSet<>());
        try {
            while (true) {
                TcpSession session = new TcpSession(tcpServerSocket.accept());
                activeSessions.add(session);
                tcpClientThreadPool.execute(() -> {
                    try {
                        sessionHandler.handleSession(session.out, session.in);
                    } finally {
                        activeSessions.remove(session);
                    }
                });
            }
        } catch (Exception e) {
            Log.e("SmartDocs", "TCP Server loop error", e);
            tcpClientThreadPool.shutdownNow();
            for (TcpSession session : activeSessions) {
                closeSilent(session);
            }
            if (!Thread.currentThread().isInterrupted() && errorListener != null) {
                errorListener.accept(e);
            }
        }
    }
}
