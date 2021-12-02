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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import javax.annotation.Nullable;

import static org.briarproject.mailbox.core.util.StringUtils.isNullOrEmpty;
import static org.briarproject.mailbox.core.util.StringUtils.isValidMac;
import static org.briarproject.mailbox.core.util.StringUtils.toHexString;

public class PrivacyUtils {

	public static String scrubOnion(String onion) {
		// keep first three characters of onion address
		return onion.substring(0, 3) + "[scrubbed]";
	}

	@Nullable
	public static String scrubMacAddress(@Nullable String address) {
		if (isNullOrEmpty(address) || !isValidMac(address)) return address;
		// this is a fake address we need to know about
		if (address.equals("02:00:00:00:00:00")) return address;
		// keep first and last octet of MAC address
		return address.substring(0, 3) + "[scrubbed]"
				+ address.substring(14, 17);
	}

	public static String scrubInetAddress(InetAddress address) {
		if (address instanceof Inet4Address) {
			// Don't scrub local IPv4 addresses
			if (address.isLoopbackAddress() || address.isLinkLocalAddress() ||
					address.isSiteLocalAddress()) {
				return address.getHostAddress();
			}
			// Keep first and last octet of non-local IPv4 addresses
			return scrubIpv4Address(address.getAddress());
		} else {
			// Keep first and last octet of IPv6 addresses
			return scrubIpv6Address(address.getAddress());
		}
	}

	private static String scrubIpv4Address(byte[] ipv4) {
		return (ipv4[0] & 0xFF) + ".[scrubbed]." + (ipv4[3] & 0xFF);
	}

	private static String scrubIpv6Address(byte[] ipv6) {
		String hex = toHexString(ipv6).toLowerCase();
		return hex.substring(0, 2) + "[scrubbed]" + hex.substring(30);
	}

	public static String scrubSocketAddress(InetSocketAddress address) {
		return scrubInetAddress(address.getAddress());
	}

	public static String scrubSocketAddress(SocketAddress address) {
		if (address instanceof InetSocketAddress)
			return scrubSocketAddress((InetSocketAddress) address);
		return "[scrubbed]";
	}
}
