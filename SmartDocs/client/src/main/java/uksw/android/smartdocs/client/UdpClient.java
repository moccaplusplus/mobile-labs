package uksw.android.smartdocs.client;

import static uksw.android.smartdocs.shared.Discovery.ADDRESS;
import static uksw.android.smartdocs.shared.Discovery.DISCOVERY_REQUEST;
import static uksw.android.smartdocs.shared.Discovery.HEADER;
import static uksw.android.smartdocs.shared.Discovery.UDP_PORT;
import static uksw.android.smartdocs.shared.Discovery.getBytes;
import static uksw.android.smartdocs.shared.Discovery.getMessage;
import static uksw.android.smartdocs.shared.Net.getLocalAddress;

import android.Manifest;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.RequiresPermission;
import androidx.core.util.Consumer;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;

import uksw.android.smartdocs.shared.HostAndPort;

public class UdpClient {
    private static final int DISCOVERY_TIMEOUT_MILLIS = 7500;
    private final Context context;
    private final Handler handler;
    private final Consumer<HostAndPort> discoveryListener;
    private final Consumer<Void> updateListener;
    private final Consumer<Exception> errorListener;
    private Thread udpThread;
    private DatagramSocket udpSocket;
    private CountDownLatch countDownLatch;
    private HostAndPort hostAndPort;
    private Exception error;

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
        return isDiscovering() || isListeningForUpdates();
    }

    public boolean isDiscovering() {
        return countDownLatch != null && countDownLatch.getCount() > 0;
    }

    public boolean isListeningForUpdates() {
        return hostAndPort != null && udpSocket != null && !udpSocket.isClosed();
    }

    public HostAndPort ensureDiscoveryResult() throws Exception {
        if (countDownLatch == null || error != null) {
            start();
        }
        countDownLatch.await();
        if (error != null) {
            throw error;
        }
        return hostAndPort;
    }

    public void start() {
        stop();
        try {
            udpSocket = new DatagramSocket(UDP_PORT);
        } catch (SocketException e) {
            handler.post(() -> errorListener.accept(e));
            return;
        }
        countDownLatch = new CountDownLatch(1);
        udpThread = new Thread(() -> {
            if (discover(udpSocket, countDownLatch)) {
                listenForUpdates(udpSocket);
            }
        });
        udpThread.setDaemon(true);
        udpThread.start();
    }

    public void stop() {
        hostAndPort = null;
        error = null;
        if (udpThread != null) {
            udpThread.interrupt();
            udpThread = null;
        }
        if (udpSocket != null) {
            udpSocket.close();
            udpSocket = null;
        }
    }

    private boolean discover(DatagramSocket socket, CountDownLatch latch) {
        try {
            socket.setBroadcast(true);
            socket.setSoTimeout(DISCOVERY_TIMEOUT_MILLIS);

            List<InetAddress> broadcastAddresses = getBroadcastAddresses(getLocalAddress(context));
            for (InetAddress broadcastAddress : broadcastAddresses) {
                socket.send(broadcastRequest(broadcastAddress));
            }
            byte[] responseBuffer = new byte[128];
            while (true) {
                DatagramPacket response = new DatagramPacket(responseBuffer, responseBuffer.length);
                socket.receive(response);
                String message = getMessage(response);
                if (isDiscoveryResponseMessage(message)) {
                    HostAndPort hostAndPort = getHostAndPort(message);
                    if (hostAndPort != null) {
                        this.hostAndPort = hostAndPort;
                        latch.countDown();
                        handler.post(() -> discoveryListener.accept(hostAndPort));
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.e("SmartDocs", "Discovery error", e);
            Exception error = Thread.currentThread().isInterrupted() ? new CancellationException() : e;
            this.error = error;
            latch.countDown();
            handler.post(() -> errorListener.accept(error));
            udpSocket.close();
            return false;
        }
    }

    @SuppressWarnings("InfiniteLoopStatement")
    private void listenForUpdates(DatagramSocket socket) {
        try {
            socket.setBroadcast(false);
            socket.setSoTimeout(0);
            byte[] responseBuffer = new byte[128];
            while (true) {
                DatagramPacket response = new DatagramPacket(responseBuffer, responseBuffer.length);
                socket.receive(response);
                String message = getMessage(response);
                if (isUpdateNotificationMessage(message)) {
                    updateListener.accept(null); // TODO: add update info
                }
            }
        } catch (Exception e) {
            Log.e("SmartDocs", "Udp loop error", e);
            if (!Thread.currentThread().isInterrupted()) {
                handler.post(() -> errorListener.accept(e));
            }
        }
    }

    private HostAndPort getHostAndPort(String message) {
        try {
            int headersLength = HEADER.length() + ADDRESS.length();
            String payload = message.substring(headersLength).trim();
            return HostAndPort.parse(payload);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isUpdateNotificationMessage(String message) {
        return false;
    }

    private boolean isDiscoveryResponseMessage(String message) {
        return message.startsWith(HEADER + ADDRESS);
    }

    private DatagramPacket broadcastRequest(InetAddress broadcastAddress) {
        byte[] bytes = getBytes(DISCOVERY_REQUEST);
        return new DatagramPacket(
                bytes, 0, bytes.length, broadcastAddress, UDP_PORT);
    }

    private static List<InetAddress> getBroadcastAddresses(InetAddress localAddress) throws SocketException {
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(localAddress);
        List<InterfaceAddress> addresses = networkInterface.getInterfaceAddresses();
        List<InetAddress> broadcastAddresses = new ArrayList<>();
        for (InterfaceAddress address : addresses) {
            InetAddress broadcastAddress = address.getBroadcast();
            if (broadcastAddress != null) {
                broadcastAddresses.add(broadcastAddress);
            }
        }
        return broadcastAddresses;
    }
}
