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

package org.briarproject.mailbox.core.db

import org.briarproject.mailbox.core.settings.Settings
import java.util.concurrent.TimeUnit.DAYS

interface DatabaseConstants {

    companion object {

        /**
         * The namespace of the [Settings] where the database schema version
         * is stored.
         */
        const val DB_SETTINGS_NAMESPACE = "db"

        /**
         * The [Settings] key under which the database schema version is
         * stored.
         */
        const val SCHEMA_VERSION_KEY = "schemaVersion"

        /**
         * The [Settings] key under which the time of the last database
         * compaction is stored.
         */
        const val LAST_COMPACTED_KEY = "lastCompacted"

        /**
         * The maximum time between database compactions in milliseconds. When the
         * database is opened it will be compacted if more than this amount of time
         * has passed since the last compaction.
         */
        var MAX_COMPACTION_INTERVAL_MS = DAYS.toMillis(30)

        /**
         * The [Settings] key under which the flag is stored indicating
         * whether the database is marked as dirty.
         */
        const val DIRTY_KEY = "dirty"

    }

}
