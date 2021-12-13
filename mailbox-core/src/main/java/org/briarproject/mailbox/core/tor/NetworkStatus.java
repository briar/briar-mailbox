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

import javax.annotation.concurrent.Immutable;

@Immutable
public class NetworkStatus {

	private final boolean connected, wifi, ipv6Only;

	public NetworkStatus(boolean connected, boolean wifi, boolean ipv6Only) {
		this.connected = connected;
		this.wifi = wifi;
		this.ipv6Only = ipv6Only;
	}

	public boolean isConnected() {
		return connected;
	}

	public boolean isWifi() {
		return wifi;
	}

	public boolean isIpv6Only() {
		return ipv6Only;
	}
}
