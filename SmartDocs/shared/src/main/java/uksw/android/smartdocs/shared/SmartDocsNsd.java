package uksw.android.smartdocs.shared;

import android.net.nsd.NsdServiceInfo;

public interface SmartDocsNsd {
    String DEFAULT_NAME = "SmartDocs";
    String SERVICE_TYPE = "_smartdocs._tcp.";

    static NsdServiceInfo serviceInfo(int port) {
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName(DEFAULT_NAME);
        serviceInfo.setServiceType(SERVICE_TYPE);
        serviceInfo.setPort(port);
        return serviceInfo; 
    }
}
