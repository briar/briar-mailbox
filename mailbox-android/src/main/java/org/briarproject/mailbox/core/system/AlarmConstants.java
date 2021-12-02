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

interface AlarmConstants {

	/**
	 * Request code for the broadcast intent attached to the periodic alarm.
	 */
	int REQUEST_ALARM = 1;

	/**
	 * Key for storing the process ID in the extras of the periodic alarm's
	 * intent. This allows us to ignore alarms scheduled by dead processes.
	 */
	String EXTRA_PID = "org.briarproject.bramble.EXTRA_PID";
}
