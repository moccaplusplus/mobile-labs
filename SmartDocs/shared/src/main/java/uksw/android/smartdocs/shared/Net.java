package uksw.android.smartdocs.shared;

import static android.content.Context.WIFI_SERVICE;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.charset.StandardCharsets.UTF_8;

import android.Manifest;
import android.content.Context;
import android.net.wifi.WifiManager;

import androidx.annotation.RequiresPermission;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public interface Net {

    @RequiresPermission(Manifest.permission.ACCESS_WIFI_STATE)
    static InetAddress getLocalAddress(Context context) throws UnknownHostException {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
        return getLocalAddress(wifiManager);
    }

    @RequiresPermission(Manifest.permission.ACCESS_WIFI_STATE)
    static InetAddress getLocalAddress(WifiManager wifiManager) throws UnknownHostException {
        int addressIp = wifiManager.getConnectionInfo().getIpAddress();
        byte[] addressBytes = ByteBuffer.allocate(4).order(LITTLE_ENDIAN).putInt(addressIp).array();
        return InetAddress.getByAddress(addressBytes);
    }
}
