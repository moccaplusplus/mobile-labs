package uksw.android.smartdocs.server;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerLoop implements Runnable {
    public interface ErrorListener {
        void onError(Exception error);
    }

    private final Context context;
    private final ServerSocket serverSocket;
    private final ExecutorService threadPool;
    private ErrorListener errorListener;
    private boolean stopped;

    public ServerLoop(Context context, ServerSocket serverSocket) {
        this.context = context;
        this.serverSocket = serverSocket;
        threadPool = Executors.newCachedThreadPool();
    }

    public void setErrorListener(ErrorListener errorListener) {
        this.errorListener = errorListener;
    }

    public void start() {
        Thread serverThread = new Thread(this);
        serverThread.setDaemon(true);
        serverThread.start();
    }

    public void stop() {
        stopped = true;
        try {
            serverSocket.close();
        } catch (IOException e) {
            Log.e("SmartDocs", "Server socket shutdown error", e);
        }
        threadPool.shutdownNow();
    }

    @Override
    public void run() {
        try {
            Socket socket = serverSocket.accept();
            threadPool.execute(new ClientHandler(context, socket));
        } catch (IOException e) {
            if (!stopped) {
                stop();
                if (errorListener != null) {
                    errorListener.onError(e);
                }
            }
        }
    }
}
