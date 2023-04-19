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

package org.briarproject.mailbox.core.system;

public interface Clock {
	long currentTimeMillis();

	/**
	 * The minimum reasonable value for the system clock, in milliseconds
	 * since the Unix epoch.
	 * <p/>
	 * 1 Jan 2023, 00:00:00 UTC
	 */
	long MIN_REASONABLE_TIME_MS = 1_672_531_200_000L;

	/**
	 * The maximum reasonable value for the system clock, in milliseconds
	 * since the Unix epoch.
	 * <p/>
	 * 1 Jan 2121, 00:00:00 UTC
	 */
	long MAX_REASONABLE_TIME_MS = 4_765_132_800_000L;
}
