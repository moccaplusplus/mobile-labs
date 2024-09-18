package uksw.android.smartdocs.client;

import static uksw.android.smartdocs.shared.Net.getBroadcastAddresses;
import static uksw.android.smartdocs.shared.Net.getLocalAddress;
import static uksw.android.smartdocs.shared.Udp.MSG_HANDSHAKE_CLIENT;
import static uksw.android.smartdocs.shared.Udp.MSG_HANDSHAKE_SERVER;
import static uksw.android.smartdocs.shared.Udp.MSG_UPDATE_BROADCAST_HEADER;
import static uksw.android.smartdocs.shared.Udp.getBytes;
import static uksw.android.smartdocs.shared.Udp.getMessage;

import android.Manifest;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.RequiresPermission;
import androidx.core.util.Consumer;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;

import uksw.android.smartdocs.shared.Settings;

public class UdpClient {
    private static final int DISCOVERY_TIMEOUT_MILLIS = 7500;
    private final byte[] responseBuffer = new byte[2054];
    private final Context context;
    private final Handler handler;
    private final Consumer<HostAndPort> discoveryListener;
    private final Consumer<Void> updateListener;
    private final Consumer<Exception> errorListener;
    private Thread udpThread;
    private DatagramSocket udpSocket;
    private CountDownLatch countDownLatch;
    private HostAndPort hostAndPort;
    private Exception handshakeError;

    @RequiresPermission(Manifest.permission.ACCESS_WIFI_STATE)
    public UdpClient(
            Context context, Handler handler, Consumer<HostAndPort> discoveryListener,
            Consumer<Void> updateListener, Consumer<Exception> errorListener) {
        this.context = context;
        this.handler = handler;
        this.discoveryListener = discoveryListener;
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
        return hostAndPort != null && udpSocket != null && !udpSocket.isClosed();
    }

    public HostAndPort ensureHandshake() throws Exception {
        if (countDownLatch == null || handshakeError != null) {
            start();
        }
        countDownLatch.await();
        if (handshakeError != null) {
            throw handshakeError;
        }
        return hostAndPort;
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
        hostAndPort = null;
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
            byte[] handshakeBytes = getBytes(MSG_HANDSHAKE_CLIENT);
            for (InetAddress broadcastAddress : broadcastAddresses) {
                DatagramPacket packet = new DatagramPacket(
                        handshakeBytes, 0, handshakeBytes.length, broadcastAddress, udpPort);
                socket.send(packet);
            }
            byte[] responseBuffer = new byte[128];
            while (true) {
                DatagramPacket response = new DatagramPacket(responseBuffer, responseBuffer.length);
                socket.receive(response);
                String message = getMessage(response);
                if (message.startsWith(MSG_HANDSHAKE_SERVER)) {
                    String payload = message.substring(MSG_HANDSHAKE_SERVER.length()).trim();
                    int tcpPort = Integer.parseInt(payload);
                    hostAndPort = new HostAndPort(response.getAddress(), tcpPort);
                    latch.countDown();
                    handler.post(() -> discoveryListener.accept(hostAndPort));
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
                String message = getMessage(response);
                if (message.startsWith(MSG_UPDATE_BROADCAST_HEADER)) {
                    updateListener.accept(null); // TODO: add update info
                }
            }
        } catch (Exception e) {
            Log.e("SmartDocs", "Udp loop error", e);
            Exception error = Thread.currentThread().isInterrupted() ? new CancellationException() : e;
            handler.post(() -> errorListener.accept(error));
        }
    }
}
