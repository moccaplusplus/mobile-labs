package uksw.android.smartdocs.shared;

import androidx.annotation.NonNull;
import androidx.core.net.ParseException;

import java.net.InetAddress;

public class HostAndPort {
    public static class ParsingException extends Exception {
        public ParsingException(Exception reason) {
            super(reason);
        }
    }
    public static HostAndPort parse(String serialized) throws ParsingException {
        try {
            String[] hostAndPort = serialized.split(":");
            InetAddress host = InetAddress.getByName(hostAndPort[0]);
            int port = Integer.parseInt(hostAndPort[1]);
            return new HostAndPort(host, port);
        } catch (Exception e) {
            throw new ParsingException(e);
        }
    }

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
