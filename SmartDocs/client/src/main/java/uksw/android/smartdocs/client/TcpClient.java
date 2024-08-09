package uksw.android.smartdocs.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import uksw.android.smartdocs.shared.HostAndPort;
import uksw.android.smartdocs.shared.Message;

public class TcpClient implements AutoCloseable {
    private static final int TIMEOUT_MILLIS = 7500;
    private final Socket socket;
    private final DataOutputStream out;
    private final DataInputStream in;

    public TcpClient(HostAndPort hostAndPort) throws IOException {
        socket = new Socket(hostAndPort.host, hostAndPort.port);
        socket.setSoTimeout(TIMEOUT_MILLIS);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
    }

    public void sendMessage(Message message) throws IOException {
        message.write(out);
        out.flush();
    }

    /** @noinspection unchecked*/
    public <T extends Message> T receiveMessage() throws IOException {
        return (T) Message.read(in);
    }

    @Override
    public void close() {
        close(in);
        close(out);
        close(socket);
    }

    private static void close(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }
}
