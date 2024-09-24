package uksw.android.smartdocs.shared;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface Tcp {
    byte MSG_OK = 1;
    byte MSG_ERROR = 2;
    byte MSG_CONFLICT = 3;
    byte MSG_CREATE_FILE = 11;
    byte MSG_SEND_FILE = 12;
    byte MSG_REMOVE_FILE = 13;
    byte MSG_GET_CONTENTS = 14;
    byte MSG_SYNC_REQ = 15;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    static String readString(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length == 0) {
            return null;
        }
        byte[] buf = new byte[length];
        in.read(buf);
        return new String(buf, UTF_8);
    }

    static void writeString(DataOutputStream out, String s) throws IOException {
        if (s == null) {
            out.writeInt(0);
            return;
        }
        byte[] buf = s.getBytes(UTF_8);
        out.writeInt(buf.length);
        if (buf.length > 0) {
            out.write(buf);
        }
    }
}
