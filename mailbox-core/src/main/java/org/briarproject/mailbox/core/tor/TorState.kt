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

package org.briarproject.mailbox.core.tor

sealed class TorState(private val id: Int) {
    operator fun compareTo(other: TorState): Int {
        return id.compareTo(other.id)
    }

    /**
     * The plugin has not finished starting or has been stopped.
     */
    object StartingStopping : TorState(0)

    /**
     * The plugin is being enabled and can't yet make or receive
     * connections.
     */
    object Enabling : TorState(1)

    /**
     * The plugin is enabled and can make or receive connections.
     */
    object Active : TorState(2)

    /**
     * The plugin has published the onion service.
     */
    object Published : TorState(3)

    /**
     * The plugin is enabled but can't make or receive connections
     */
    object Inactive : TorState(4)
}
