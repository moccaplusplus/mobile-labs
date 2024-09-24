package uksw.android.smartdocs.server;

import static uksw.android.smartdocs.shared.Udp.HANDSHAKE_CLIENT_HEADER;
import static uksw.android.smartdocs.shared.Udp.HANDSHAKE_SERVER_HEADER;
import static uksw.android.smartdocs.shared.Udp.UPDATE_BROADCAST_HEADER;
import static uksw.android.smartdocs.shared.Udp.checkHeader;
import static uksw.android.smartdocs.shared.Udp.writeMessage;

import android.util.Log;

import androidx.core.util.Consumer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import uksw.android.smartdocs.shared.FileInfo;

public class UdpServer {
    private final byte[] requestBuffer = new byte[2048];
    private final byte[] handshakeBytes;
    private final DatagramSocket udpSocket;
    private final Thread udpThread;
    private final Consumer<Exception> errorListener;
    private final ExecutorService broadcastPool = Executors.newSingleThreadExecutor();
    private final List<InetAddress> broadcastAddresses;
    private final int udpPort;

    public UdpServer(int udpPort, int tcpPort, List<InetAddress> broadcastAddresses, Consumer<Exception> errorListener) throws IOException {
        udpSocket = new DatagramSocket(udpPort);
        udpSocket.setBroadcast(true);
        handshakeBytes = writeMessage(out -> {
            out.write(HANDSHAKE_SERVER_HEADER);
            out.writeInt(tcpPort);
        });
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

    public void broadcastUpdate(List<FileInfo> updatedFiles) {
        broadcastPool.execute(() -> {
            try {
                doBroadcastUpdate(updatedFiles);
            } catch (Exception e) {
                onSocketError(e);
            }
        });
    }

    private void doBroadcastUpdate(List<FileInfo> updatedFiles) throws IOException {
        byte[] msgBytes = writeMessage(dos -> {
            dos.write(UPDATE_BROADCAST_HEADER);
            dos.writeInt(updatedFiles.size());
            for (FileInfo item : updatedFiles) {
                item.write(dos);
            }
        });
        for (InetAddress address : broadcastAddresses) {
            DatagramPacket packet = new DatagramPacket(msgBytes, 0, msgBytes.length, address, udpPort);
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
        if (checkHeader(HANDSHAKE_CLIENT_HEADER, request)) {
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
