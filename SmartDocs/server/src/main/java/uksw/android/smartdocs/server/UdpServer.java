package uksw.android.smartdocs.server;

import static uksw.android.smartdocs.shared.Udp.MSG_HANDSHAKE_CLIENT;
import static uksw.android.smartdocs.shared.Udp.MSG_HANDSHAKE_SERVER;
import static uksw.android.smartdocs.shared.Udp.MSG_UPDATE_BROADCAST_HEADER;
import static uksw.android.smartdocs.shared.Udp.getBytes;
import static uksw.android.smartdocs.shared.Udp.getMessage;

import android.util.Log;

import androidx.core.util.Consumer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UdpServer {
    private final byte[] requestBuffer = new byte[256];
    private final DatagramSocket udpSocket;
    private final Thread udpThread;
    private final Consumer<Exception> errorListener;
    private final ExecutorService broadcastPool = Executors.newSingleThreadExecutor();
    private final byte[] handshakeBytes;
    private final List<InetAddress> broadcastAddresses;
    private final int udpPort;

    public UdpServer(int udpPort, int tcpPort, List<InetAddress> broadcastAddresses, Consumer<Exception> errorListener) throws SocketException {
        udpSocket = new DatagramSocket(udpPort);
        udpSocket.setBroadcast(true);
        handshakeBytes = getBytes(MSG_HANDSHAKE_SERVER + tcpPort + "\n");
        this.udpPort = udpPort + 1; // add one for emulator only
        this.broadcastAddresses = broadcastAddresses;
        this.errorListener = errorListener;
        udpThread = new Thread(this::udpLoop);
        udpThread.setDaemon(true);
    }

    public void start() {
        udpThread.start();
    }

    public void stop() {
        broadcastPool.shutdown();
        try {
            udpThread.interrupt();
            udpSocket.close();
        } catch (Exception e) {
            Log.e("SmartDocs", "UDP Socket shutdown error", e);
        }
    }

    public void broadcastUpdate(List<String> updatedFiles) {
        broadcastPool.execute(() -> {
            try {
                doBroadcastUpdate(updatedFiles);
            } catch (Exception e) {
                onSocketError(e);
            }
        });
    }

    private void doBroadcastUpdate(List<String> updatedFiles) throws IOException {
        String message = MSG_UPDATE_BROADCAST_HEADER;
        message += String.join("\n", updatedFiles) + "\n";
        byte[] bytes = getBytes(message);
        for (InetAddress address : broadcastAddresses) {
            DatagramPacket packet = new DatagramPacket(bytes, 0, bytes.length, address, udpPort);
            udpSocket.send(packet);
        }
    }

    private void udpLoop() {
        try {
            while (true) {
                handleHandshake();
            }
        } catch (Exception e) {
            onSocketError(e);
        }
    }

    private void handleHandshake() throws IOException {
        DatagramPacket request = new DatagramPacket(
                requestBuffer, 0, requestBuffer.length);
        udpSocket.receive(request);
        if (MSG_HANDSHAKE_CLIENT.equals(getMessage(request))) {
            DatagramPacket response = new DatagramPacket(
                    handshakeBytes, 0, handshakeBytes.length, request.getAddress(),
                    request.getPort());
            udpSocket.send(response);
        }
    }

    private void onSocketError(Exception e) {
        Log.e("SmartDocs", "UDP Socket error", e);
        if (!Thread.currentThread().isInterrupted() && errorListener != null) {
            errorListener.accept(e);
        }
    }
}
