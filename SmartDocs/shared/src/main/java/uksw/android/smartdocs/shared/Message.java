package uksw.android.smartdocs.shared;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Message {
    public interface Types {
        interface Request {
            byte LIST_DIR = 1;
            byte SAVE_FILE = 2;
            byte PATCH_FILE = 3;
            byte DELETE_FILE = 4;
            byte DELETE_DIR = 5;
        }
    }
    public static Message read(DataInputStream in) throws IOException {
        byte type = in.readByte();
        int length = in.readInt();
        switch (type) {
            case Types.Request.LIST_DIR:
                return ListDirRequestMessage.read(in);
            default:
                return new Message(type);
        }
    }

    public final int type;

    protected Message(int type) {
        this.type = type;
    }

    protected void write(DataOutputStream out) throws IOException {
        out.writeByte(type);
    }
}
