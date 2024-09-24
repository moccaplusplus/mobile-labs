package uksw.android.smartdocs.client;

import static uksw.android.smartdocs.shared.Inet.getBroadcastAddresses;
import static uksw.android.smartdocs.shared.Inet.getLocalAddress;
import static uksw.android.smartdocs.shared.Udp.HANDSHAKE_CLIENT_HEADER;
import static uksw.android.smartdocs.shared.Udp.HANDSHAKE_SERVER_HEADER;
import static uksw.android.smartdocs.shared.Udp.UPDATE_BROADCAST_HEADER;
import static uksw.android.smartdocs.shared.Udp.checkHeader;
import static uksw.android.smartdocs.shared.Udp.payloadStream;

import android.Manifest;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.RequiresPermission;
import androidx.core.util.Consumer;

import java.io.DataInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;

import uksw.android.smartdocs.shared.FileInfo;
import uksw.android.smartdocs.shared.Settings;

public class UdpClient {
    private static final int DISCOVERY_TIMEOUT_MILLIS = 7500;

    private final byte[] responseBuffer = new byte[32_768];
    private final Context context;
    private final Handler handler;
    private final Consumer<Pair<InetAddress, Integer>> handshakeListener;
    private final Consumer<FileInfo> updateListener;
    private final Consumer<Exception> errorListener;
    private Thread udpThread;
    private DatagramSocket udpSocket;
    private CountDownLatch countDownLatch;
    private Exception handshakeError;
    private InetAddress host;
    private int tcpPort = -1;

    @RequiresPermission(Manifest.permission.ACCESS_WIFI_STATE)
    public UdpClient(
            Context context, Handler handler, Consumer<Pair<InetAddress, Integer>> handshakeListener,
            Consumer<FileInfo> updateListener, Consumer<Exception> errorListener) {
        this.context = context;
        this.handler = handler;
        this.handshakeListener = handshakeListener;
        this.updateListener = updateListener;
        this.errorListener = errorListener;
    }

    public boolean isRunning() {
        return isConnecting() || isConnected();
    }

    public boolean isConnecting() {
        return countDownLatch != null && countDownLatch.getCount() > 0;
    }

    public boolean isConnected() {
        return host != null && udpSocket != null && !udpSocket.isClosed();
    }

    public void ensureHandshake() throws Exception {
        if (countDownLatch == null || handshakeError != null) {
            start();
        }
        countDownLatch.await();
        if (handshakeError != null) {
            throw handshakeError;
        }
    }

    public InetAddress getHost() {
        return host;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    public void start() {
        stop();
        int udpPort = Settings.get(context).getUdpServerPort();
        try {
            udpSocket = new DatagramSocket(udpPort + 1); // plus 1 only for emulator
            udpSocket.setBroadcast(true);
        } catch (SocketException e) {
            handler.post(() -> errorListener.accept(e));
            return;
        }
        countDownLatch = new CountDownLatch(1);
        udpThread = new Thread(() -> {
            if (handshake(udpSocket, udpPort, countDownLatch)) {
                listenForUpdates(udpSocket);
            }
        });
        udpThread.setDaemon(true);
        udpThread.start();
    }

    public void stop() {
        host = null;
        tcpPort = -1;
        handshakeError = null;
        countDownLatch = null;
        if (udpThread != null) {
            udpThread.interrupt();
            udpThread = null;
        }
        if (udpSocket != null) {
            udpSocket.close();
            udpSocket = null;
        }
    }

    private boolean handshake(DatagramSocket socket, int udpPort, CountDownLatch latch) {
        try {
            socket.setSoTimeout(DISCOVERY_TIMEOUT_MILLIS);

            List<InetAddress> broadcastAddresses = getBroadcastAddresses(getLocalAddress(context));
            for (InetAddress broadcastAddress : broadcastAddresses) {
                DatagramPacket packet = new DatagramPacket(
                        HANDSHAKE_CLIENT_HEADER, HANDSHAKE_CLIENT_HEADER.length, broadcastAddress, udpPort);
                socket.send(packet);
            }
            while (true) {
                DatagramPacket response = new DatagramPacket(responseBuffer, responseBuffer.length);
                socket.receive(response);
                if (checkHeader(HANDSHAKE_SERVER_HEADER, response)) {
                    host = response.getAddress();
                    try (DataInputStream payload = payloadStream(HANDSHAKE_SERVER_HEADER, response)) {
                        tcpPort = payload.readInt();
                    }
                    latch.countDown();
                    handler.post(() -> handshakeListener.accept(new Pair<>(host, tcpPort)));
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e("SmartDocs", "Handshake error", e);
            Exception error = Thread.currentThread().isInterrupted() ? new CancellationException() : e;
            handshakeError = error;
            latch.countDown();
            handler.post(() -> errorListener.accept(error));
            udpSocket.close();
            return false;
        }
    }

    @SuppressWarnings("InfiniteLoopStatement")
    private void listenForUpdates(DatagramSocket socket) {
        try {
            socket.setSoTimeout(0);
            while (true) {
                DatagramPacket response = new DatagramPacket(responseBuffer, responseBuffer.length);
                socket.receive(response);
                if (checkHeader(UPDATE_BROADCAST_HEADER, response)) {
                    try (DataInputStream in = payloadStream(UPDATE_BROADCAST_HEADER, response)) {
                        FileInfo payload = FileInfo.read(in);
                        updateListener.accept(payload);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("SmartDocs", "Udp loop error", e);
            Exception error = Thread.currentThread().isInterrupted() ? new CancellationException() : e;
            handler.post(() -> errorListener.accept(error));
        }
    }
}
