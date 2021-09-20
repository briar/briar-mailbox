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
