package uksw.android.smartdocs.server;

import android.content.Context;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Context context;
    private final Socket socket;

    public ClientHandler(Context context, Socket socket) {
        this.context = context;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            session(
                    new DataInputStream(socket.getInputStream()),
                    new DataOutputStream(socket.getOutputStream()));
        } catch (IOException e) {
            Log.e("SmartDocs", "Client session error", e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e("SmartDocs", "Client socket shutdown error", e);
            }
        }
    }

    private void session(DataInputStream in, DataOutputStream out) throws IOException {
        // TODO
    }
}
