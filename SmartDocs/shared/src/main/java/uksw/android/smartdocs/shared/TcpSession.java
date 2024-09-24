package uksw.android.smartdocs.shared;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class TcpSession implements AutoCloseable {
    private static final int TIMEOUT_MILLIS = 7500;

    public static void closeSilent(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }

    public final Socket socket;
    public final DataOutputStream out;
    public final DataInputStream in;

    public TcpSession(Socket socket) throws IOException {
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
