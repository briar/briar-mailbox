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

import java.util.List;

public interface CircumventionProvider {

	/**
	 * Countries where Tor is blocked, i.e. vanilla Tor connection won't work.
	 * <p>
	 * See https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2
	 * and https://trac.torproject.org/projects/tor/wiki/doc/OONI/censorshipwiki
	 */
	String[] BLOCKED = {"CN", "IR", "EG", "BY", "TR", "SY", "VE"};

	/**
	 * Countries where obfs4 or meek bridge connections are likely to work.
	 * Should be a subset of {@link #BLOCKED}.
	 */
	String[] BRIDGES = {"CN", "IR", "EG", "BY", "TR", "SY", "VE"};

	/**
	 * Countries where obfs4 bridges won't work and meek is needed.
	 * Should be a subset of {@link #BRIDGES}.
	 */
	String[] NEEDS_MEEK = {"CN", "IR"};

	boolean isTorProbablyBlocked(String countryCode);

	boolean doBridgesWork(String countryCode);

	boolean needsMeek(String countryCode);

	List<String> getBridges(boolean meek);

}
