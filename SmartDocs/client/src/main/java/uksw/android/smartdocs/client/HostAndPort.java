package uksw.android.smartdocs.client;

import java.net.InetAddress;

public class HostAndPort {
    public final InetAddress host;
    public final int port;

    public HostAndPort(InetAddress host, int port) {
        this.host = host;
        this.port = port;
    }
}
