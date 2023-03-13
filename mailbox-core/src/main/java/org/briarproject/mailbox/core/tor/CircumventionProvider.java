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

import org.briarproject.mailbox.core.lifecycle.IoExecutor;
import org.briarproject.nullsafety.NotNullByDefault;

import java.util.List;

@NotNullByDefault
public interface CircumventionProvider {

	enum BridgeType {
		DEFAULT_OBFS4,
		NON_DEFAULT_OBFS4,
		VANILLA,
		MEEK,
		SNOWFLAKE
	}

	/**
	 * Countries where Tor is blocked, i.e. vanilla Tor connection won't work.
	 * <p>
	 * See https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2
	 * and https://trac.torproject.org/projects/tor/wiki/doc/OONI/censorshipwiki
	 */
	String[] BLOCKED = {"BY", "CN", "EG", "IR", "RU", "TM", "VE"};

	/**
	 * Countries where bridge connections are likely to work.
	 * Should be a subset of {@link #BLOCKED} and the union of
	 * {@link #DEFAULT_BRIDGES}, {@link #NON_DEFAULT_BRIDGES} and
	 * {@link #DPI_BRIDGES}.
	 */
	String[] BRIDGES = {"BY", "CN", "EG", "IR", "RU", "TM", "VE"};

	/**
	 * Countries where default obfs4 or vanilla bridges are likely to work.
	 * Should be a subset of {@link #BRIDGES}.
	 */
	String[] DEFAULT_BRIDGES = {"EG", "VE"};

	/**
	 * Countries where non-default obfs4 or vanilla bridges are likely to work.
	 * Should be a subset of {@link #BRIDGES}.
	 */
	String[] NON_DEFAULT_BRIDGES = {"BY", "RU"};

	/**
	 * Countries where vanilla bridges are blocked via DPI but non-default
	 * obfs4 bridges, meek and snowflake may work. Should be a subset of
	 * {@link #BRIDGES}.
	 */
	String[] DPI_BRIDGES = {"CN", "IR", "TM"};

	/**
	 * Returns true if bridge connections of some type work in the given
	 * country.
	 */
	boolean doBridgesWork(String countryCode);

	/**
	 * Returns the types of bridge connection that are suitable for the given
	 * country, or {@link #DEFAULT_BRIDGES} if no bridge type is known
	 * to work.
	 */
	List<BridgeType> getSuitableBridgeTypes(String countryCode);

	@IoExecutor
	List<String> getBridges(BridgeType type, String countryCode,
			boolean letsEncrypt);
}
