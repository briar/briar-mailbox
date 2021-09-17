package org.briarproject.mailbox.core.tor;

import org.slf4j.Logger;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

import javax.inject.Inject;

import static java.util.Collections.list;
import static org.briarproject.mailbox.core.util.LogUtils.logException;
import static org.briarproject.mailbox.core.util.NetworkUtils.getNetworkInterfaces;
import static org.slf4j.LoggerFactory.getLogger;

class JavaCliNetworkManager implements NetworkManager {

    private static final Logger LOG = getLogger(JavaCliNetworkManager.class);

    @Inject
    JavaCliNetworkManager() {
    }

    @Override
    public NetworkStatus getNetworkStatus() {
        boolean connected = false, hasIpv4 = false, hasIpv6Unicast = false;
        try {
            for (NetworkInterface i : getNetworkInterfaces()) {
                if (i.isLoopback() || !i.isUp()) continue;
                for (InetAddress addr : list(i.getInetAddresses())) {
                    connected = true;
                    if (addr instanceof Inet4Address) {
                        hasIpv4 = true;
                    } else if (!addr.isMulticastAddress()) {
                        hasIpv6Unicast = true;
                    }
                }
            }
        } catch (SocketException e) {
            logException(LOG, e);
        }
        return new NetworkStatus(connected, false, !hasIpv4 && hasIpv6Unicast);
    }

}
