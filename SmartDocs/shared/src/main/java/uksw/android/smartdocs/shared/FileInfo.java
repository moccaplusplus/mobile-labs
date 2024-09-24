package uksw.android.smartdocs.shared;

import static uksw.android.smartdocs.shared.Tcp.readString;
import static uksw.android.smartdocs.shared.Tcp.writeString;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileInfo {
    public static FileInfo read(DataInputStream in) throws IOException {
        String name = readString(in);
        long lastModified = in.readLong();
        return new FileInfo(name, lastModified);
    }

    public static void writeFileWithContents(DataOutputStream out, File file) throws IOException {
        if (file.exists()) {
            writeString(out, file.getName());
            out.writeLong(file.lastModified());
            out.writeLong(file.length());
            try (FileInputStream fis = new FileInputStream(file)) {
                int b;
                while ((b = fis.read()) != -1) {
                    out.write(b);
                }
            }
        } else {
            writeString(out, file.getName());
            out.writeLong(-1);
        }
    }

    public final String name;
    public final long lastModified;

    public FileInfo(String name, long lastModified) {
        this.name = name;
        this.lastModified = lastModified;
    }

    public void write(DataOutputStream out) throws IOException {
        writeString(out, name);
        out.writeLong(lastModified);
    }
}
