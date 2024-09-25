package uksw.android.smartdocs.shared;

import static java.nio.charset.StandardCharsets.UTF_8;
import static uksw.android.smartdocs.shared.Tcp.readString;
import static uksw.android.smartdocs.shared.Tcp.writeString;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

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

    public static void writeRemoved(DataOutputStream out, String name, long dirtyTimestamp) throws IOException {
        writeString(out, name);
        out.writeLong(dirtyTimestamp);
        out.writeLong(-1L);
    }

    public static void writeFileWithContents(DataOutputStream out, File file) throws IOException {
        writeFile(out, file);
        writeContents(out, file);
    }

    public static void readContents(DataInputStream in, long length, File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            while (length-- > 0) {
                fos.write(in.read());
            }
        }
    }

    public static void appendConflictInfo(DataInputStream in, FileInfo fileInfo, File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file, true)) {
            fos.write(String.format("\n=== CONFLICT\n=== VERSION: $1%s\n",
                    new Date(fileInfo.lastModified)).getBytes(UTF_8));
            for (int i = 0; i < fileInfo.length; i++) {
                fos.write(in.read());
            }
        }
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
}
