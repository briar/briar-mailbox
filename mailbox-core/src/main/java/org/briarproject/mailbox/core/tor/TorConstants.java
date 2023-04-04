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

public interface TorConstants {

	// Settings
	String SETTINGS_NAMESPACE = "Tor";
	String HS_PRIVATE_KEY_V3 = "onionPrivKey3";
	String HS_ADDRESS_V3 = "onionAddress3";
	/**
	 * Whether circumvention bridge handling should be handled automatically.
	 */
	String BRIDGE_AUTO = "bridgeAuto";
	/**
	 * Whether bridges should be used for circumvention.
	 * Only consider when {@link #BRIDGE_AUTO} is false.
	 */
	String BRIDGE_USE = "bridgeUse";
	String BRIDGE_USE_SNOWFLAKE = "bridgeUseSnowflake";
	String BRIDGE_USE_MEEK = "bridgeUseMeek";
	String BRIDGE_USE_OBFS4 = "bridgeUseObfs4";
	String BRIDGE_USE_OBFS4_DEFAULT = "bridgeUseObfs4Default";
	String BRIDGE_USE_VANILLA = "bridgeUseVanilla";

	int SOCKS_PORT = 59054;
	int CONTROL_PORT = 59055;

}
