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

interface SharedWakeLock {

	/**
	 * Acquires the wake lock. This increments the wake lock's reference count,
	 * so unlike {@link AndroidWakeLock#acquire()} every call to this method
	 * must be followed by a balancing call to {@link #release()}.
	 */
	void acquire();

	/**
	 * Releases the wake lock. This decrements the wake lock's reference count,
	 * so unlike {@link AndroidWakeLock#release()} every call to this method
	 * must follow a balancing call to {@link #acquire()}.
	 */
	void release();

}
