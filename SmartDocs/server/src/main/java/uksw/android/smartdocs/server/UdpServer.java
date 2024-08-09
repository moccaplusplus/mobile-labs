package uksw.android.smartdocs.server;

import static uksw.android.smartdocs.shared.Discovery.DISCOVERY_REQUEST;
import static uksw.android.smartdocs.shared.Discovery.getMessage;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import uksw.android.smartdocs.shared.Discovery;

public class UdpServer {
    private final byte[] requestBuffer = new byte[128];
    private final String discoveryResponse;
    private final DatagramSocket udpSocket;
    private final Thread udpThread;
    private final Consumer<Exception> errorListener;

    public UdpServer(@NonNull String tcpServerAddress, @Nullable Consumer<Exception> errorListener) throws SocketException {
        udpSocket = new DatagramSocket(Discovery.UDP_PORT);
        this.errorListener = errorListener;
        discoveryResponse = Discovery.discoveryResponse(tcpServerAddress);
        udpThread = new Thread(this::udpLoop);
        udpThread.setDaemon(true);
    }

    public void start() {
        udpThread.start();
    }

    public void stop() {
        try {
            udpThread.interrupt();
            udpSocket.close();
        } catch (Exception e) {
            Log.e("SmartDocs", "UDP Socket shutdown error", e);
        }
    }

    private void udpLoop() {
        try {
            while (true) {
                DatagramPacket packet = new DatagramPacket(requestBuffer, 0, requestBuffer.length);
                udpSocket.receive(packet);
                if (isDiscoveryRequest(packet)) {
                    udpSocket.send(discoveryResponse(packet));
                }
            }
        } catch (Exception e) {
            Log.e("SmartDocs", "UDP Socket error", e);
            if (!Thread.currentThread().isInterrupted() && errorListener != null) {
                errorListener.accept(e);
            }
        }
    }

    private DatagramPacket discoveryResponse(DatagramPacket request) {
        byte[] bytes = Discovery.getBytes(discoveryResponse);
        return new DatagramPacket(bytes, 0, bytes.length, request.getAddress(), request.getPort());
    }

    private boolean isDiscoveryRequest(DatagramPacket request) {
        return DISCOVERY_REQUEST.equals(getMessage(request));
    }
}
