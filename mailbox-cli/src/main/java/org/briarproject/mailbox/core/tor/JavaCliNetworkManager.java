/*
 *     Briar Mailbox
 *     Copyright (C) 2021-2022  The Briar Project
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

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
			logException(LOG, e, "Error while getting network status");
		}
		return new NetworkStatus(connected, false, !hasIpv4 && hasIpv6Unicast);
	}

}
