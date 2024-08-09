package uksw.android.smartdocs.server;

import static uksw.android.smartdocs.shared.Discovery.DEFAULT_PORT;

import android.util.Log;

import androidx.core.util.Consumer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpServer {
    private final ServerSocket tcpServerSocket;
    private final ExecutorService tcpClientThreadPool;
    private final Thread tcpThread;
    private final Consumer<Socket> clientHandler;
    private final Consumer<Exception> errorListener;

    public TcpServer(Consumer<Socket> clientHandler, Consumer<Exception> errorListener) throws IOException {
        this.clientHandler = clientHandler;
        this.errorListener = errorListener;
        tcpServerSocket = new ServerSocket(DEFAULT_PORT);
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
        Set<Socket> activeClients = Collections.synchronizedSet(new HashSet<>());
        try {
            while (true) {
                Socket socket = tcpServerSocket.accept();
                activeClients.add(socket);
                tcpClientThreadPool.execute(() -> {
                    clientHandler.accept(socket);
                    activeClients.remove(socket);
                });
            }
        } catch (Exception e) {
            Log.e("SmartDocs", "TCP Server loop error", e);
            tcpClientThreadPool.shutdownNow();
            for (Socket socket : activeClients) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
            if (!Thread.currentThread().isInterrupted() && errorListener != null) {
                errorListener.accept(e);
            }
        }
    }
}
