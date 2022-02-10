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
			logException(LOG, e,
					"Error while retrieving list of network interfaces");
			return emptyList();
		}
	}
}
