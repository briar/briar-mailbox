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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static org.briarproject.mailbox.core.tor.CircumventionProvider.BridgeType.DEFAULT_OBFS4;
import static org.briarproject.mailbox.core.tor.CircumventionProvider.BridgeType.MEEK;
import static org.briarproject.mailbox.core.tor.CircumventionProvider.BridgeType.NON_DEFAULT_OBFS4;
import static org.briarproject.mailbox.core.tor.CircumventionProvider.BridgeType.VANILLA;

@Immutable
class CircumventionProviderImpl implements CircumventionProvider {

	private final static String BRIDGE_FILE_NAME = "bridges";

	private static final Set<String> BRIDGE_COUNTRIES =
			new HashSet<>(asList(BRIDGES));
	private static final Set<String> DEFAULT_OBFS4_BRIDGE_COUNTRIES =
			new HashSet<>(asList(DEFAULT_BRIDGES));
	private static final Set<String> NON_DEFAULT_BRIDGE_COUNTRIES =
			new HashSet<>(asList(NON_DEFAULT_BRIDGES));
	private static final Set<String> DPI_COUNTRIES =
			new HashSet<>(asList(DPI_BRIDGES));

	@Inject
	CircumventionProviderImpl() {
	}

	@Override
	public boolean doBridgesWork(String countryCode) {
		return BRIDGE_COUNTRIES.contains(countryCode);
	}

	@Override
	public List<BridgeType> getSuitableBridgeTypes(String countryCode) {
		if (DEFAULT_OBFS4_BRIDGE_COUNTRIES.contains(countryCode)) {
			return asList(DEFAULT_OBFS4, VANILLA);
		} else if (NON_DEFAULT_BRIDGE_COUNTRIES.contains(countryCode)) {
			return asList(NON_DEFAULT_OBFS4, VANILLA);
		} else if (DPI_COUNTRIES.contains(countryCode)) {
			return asList(NON_DEFAULT_OBFS4, MEEK);
		} else {
			return asList(DEFAULT_OBFS4, VANILLA);
		}
	}

	@Override
	@IoExecutor
	public List<String> getBridges(BridgeType type) {
		InputStream is = requireNonNull(getClass().getClassLoader()
				.getResourceAsStream(BRIDGE_FILE_NAME));
		Scanner scanner = new Scanner(is);

		List<String> bridges = new ArrayList<>();
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if ((type == DEFAULT_OBFS4 && line.startsWith("d ")) ||
					(type == NON_DEFAULT_OBFS4 && line.startsWith("n ")) ||
					(type == VANILLA && line.startsWith("v ")) ||
					(type == MEEK && line.startsWith("m "))) {
				bridges.add(line.substring(2));
			}
		}
		scanner.close();
		return bridges;
	}

}
