package uksw.android.smartdocs.shared;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ListDirRequestMessage extends Message {
    public static ListDirRequestMessage read(DataInputStream in) throws IOException {
        int length = in.readInt();
        byte[] buffer = new byte[length];
        in.readFully(buffer, 0, length);
        String dir = new String(buffer, StandardCharsets.UTF_8);
        return new ListDirRequestMessage(dir);
    }

    public final String dir;
    public ListDirRequestMessage(String dir) {
        super(Types.Request.LIST_DIR);
        this.dir = dir;
    }

    public void write(DataOutputStream out) throws IOException {
        super.write(out);
        if (dir == null) {
            out.writeInt(0);
            return;
        }
        byte[] buffer = dir.getBytes(StandardCharsets.UTF_8);
        if (buffer.length > 0) {
            out.writeInt(buffer.length);
        }
        out.write(buffer);
    }
}
