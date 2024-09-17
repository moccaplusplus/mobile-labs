package uksw.android.smartdocs.shared;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.DatagramPacket;

public interface Udp {
    int DEFAULT_PORT = 9876;

    String HEADER = "SmartDocs\n";
    String MSG_HANDSHAKE_CLIENT = HEADER + "Hello From Client\n";
    String MSG_HANDSHAKE_SERVER = HEADER + "Hello From Server\n";
    String MSG_UPDATE_BROADCAST_HEADER = HEADER + "Update Broadcast\n";

    static String getMessage(DatagramPacket packet) {
        return new String(packet.getData(), 0, packet.getLength(), UTF_8);
    }

    static byte[] getBytes(String message) {
        return message.getBytes(UTF_8);
    }
}
