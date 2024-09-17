package uksw.android.smartdocs.shared;

import androidx.annotation.NonNull;

import java.net.InetAddress;

public class HostAndPort {
    public final InetAddress host;
    public final int port;

    public HostAndPort(InetAddress host, int port) {
        this.host = host;
        this.port = port;
    }

    @NonNull
    @Override
    public String toString() {
        return host.getHostAddress() + ":" + port;
    }
}
