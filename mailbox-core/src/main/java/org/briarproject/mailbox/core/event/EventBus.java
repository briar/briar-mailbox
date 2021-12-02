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

package org.briarproject.mailbox.core.event;

public interface EventBus {

	/**
	 * Adds a listener to be notified when events occur.
	 */
	void addListener(EventListener l);

	/**
	 * Removes a listener.
	 */
	void removeListener(EventListener l);

	/**
	 * Asynchronously notifies all listeners of an event. Listeners are
	 * notified on the {@link EventExecutor}.
	 * <p>
	 * This method can safely be called while holding a lock.
	 */
	void broadcast(Event e);
}
