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

import javax.annotation.Nullable;

public class OsUtils {

	@Nullable
	private static final String os = System.getProperty("os.name");
	@Nullable
	private static final String vendor = System.getProperty("java.vendor");

	public static boolean isWindows() {
		return os != null && os.contains("Windows");
	}

	public static boolean isMac() {
		return os != null && os.contains("Mac OS");
	}

	public static boolean isLinux() {
		return os != null && os.contains("Linux") && !isAndroid();
	}

	public static boolean isAndroid() {
		return vendor != null && vendor.contains("Android");
	}
}
