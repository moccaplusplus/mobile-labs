package uksw.android.smartdocs.shared;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.DatagramPacket;

public interface Discovery {
    int DEFAULT_PORT = 11111;
    int UDP_PORT = 9876;

    String HEADER = "SmartDocs\n";
    String GET_ADDRESS = "GetAddress\n";
    String ADDRESS = "Address\n";
    String DISCOVERY_REQUEST = HEADER + GET_ADDRESS;

    static String discoveryResponse(String address) {
        return HEADER + ADDRESS + address + "\n";
    }

    static String getMessage(DatagramPacket packet) {
        return new String(packet.getData(), 0, packet.getLength(), UTF_8);
    }

    static byte[] getBytes(String message) {
        return message.getBytes(UTF_8);
    }
}
