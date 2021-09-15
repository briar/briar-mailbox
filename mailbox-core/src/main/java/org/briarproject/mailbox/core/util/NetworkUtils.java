package org.briarproject.mailbox.core.util;

import org.slf4j.Logger;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.list;
import static org.briarproject.mailbox.core.util.LogUtils.logException;
import static org.slf4j.LoggerFactory.getLogger;

public class NetworkUtils {

	private static final Logger LOG = getLogger(NetworkUtils.class.getName());

	public static List<NetworkInterface> getNetworkInterfaces() {
		try {
			Enumeration<NetworkInterface> ifaces =
					NetworkInterface.getNetworkInterfaces();
			// Despite what the docs say, the return value can be null
			//noinspection ConstantConditions
			return ifaces == null ? emptyList() : list(ifaces);
		} catch (SocketException e) {
			logException(LOG, e);
			return emptyList();
		}
	}
}
