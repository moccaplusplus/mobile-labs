package uksw.android.smartdocs.shared;

import static uksw.android.smartdocs.shared.Tcp.readString;
import static uksw.android.smartdocs.shared.Tcp.writeString;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileInfo {
    public static FileInfo read(DataInputStream in) throws IOException {
        String name = readString(in);
        long lastModified = in.readLong();
        long length = in.readLong();
        return new FileInfo(name, lastModified, length);
    }

    public static void writeFile(DataOutputStream out, File file) throws IOException {
        writeString(out, file.getName());
        out.writeLong(file.lastModified());
        out.writeLong(file.exists() ? file.length() : -1L);
    }

    public static void writeFileWithContents(DataOutputStream out, File file) throws IOException {
        writeFile(out, file);
        writeContents(out, file);
    }

    public static boolean readContents(DataInputStream in, File file) throws IOException {
        long length = in.readLong();
        if (length == -1) {
            return false;
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            while (length-- > 0) {
                fos.write(in.read());
            }
        }
        return true;
    }

    public static void writeContents(DataOutputStream out, File file) throws IOException {
        if (file.exists() && file.length() > 0) {
            try (FileInputStream fis = new FileInputStream(file)) {
                int b;
                while ((b = fis.read()) != -1) {
                    out.write(b);
                }
            }
        }
    }

    public final String name;
    public final long lastModified;
    public final long length;

    public FileInfo(String name, long lastModified, long length) {
        this.name = name;
        this.lastModified = lastModified;
        this.length = length;
    }

    public void write(DataOutputStream out) throws IOException {
        writeString(out, name);
        out.writeLong(lastModified);
        out.writeLong(length);
    }
}
