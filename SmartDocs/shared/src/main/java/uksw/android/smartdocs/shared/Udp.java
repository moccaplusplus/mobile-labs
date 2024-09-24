package uksw.android.smartdocs.shared;

import static java.nio.charset.StandardCharsets.UTF_8;

import androidx.core.util.Consumer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;

public interface Udp {
    int DEFAULT_PORT = 9876;

    String HEADER = "SmartDocs\n";
    byte[] HANDSHAKE_CLIENT_HEADER = (HEADER + "Hello From Client\n").getBytes(UTF_8);
    byte[] HANDSHAKE_SERVER_HEADER = (HEADER + "Hello From Server\n").getBytes(UTF_8);
    byte[] UPDATE_BROADCAST_HEADER = (HEADER + "Update Broadcast\n").getBytes(UTF_8);

    static boolean checkHeader(byte[] header, DatagramPacket packet) {
        return checkHeader(header, packet.getData(), packet.getLength());
    }

    static boolean checkHeader(byte[] header, byte[] data, int dataLength) {
        if (dataLength < header.length) {
            return false;
        }
        for (int i = 0; i < header.length; i++) {
            if (data[i] != header[i]) {
                return false;
            }
        }
        return true;
    }

    static DataInputStream payloadStream(int headerLength, DatagramPacket packet) {
        return new DataInputStream(new ByteArrayInputStream(packet.getData(), headerLength, packet.getLength() - headerLength));
    }

    static DataInputStream payloadStream(byte[] headerBytes, DatagramPacket packet) {
        return payloadStream(headerBytes.length, packet);
    }

    static byte[] writeMessage(ThrowingConsumer<DataOutputStream, IOException> consumer) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(baos)) {
            consumer.accept(dos);
        }
        return baos.toByteArray();
    }
}
