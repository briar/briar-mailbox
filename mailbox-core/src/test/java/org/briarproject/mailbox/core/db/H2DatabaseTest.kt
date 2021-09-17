package org.briarproject.mailbox.core.db

import org.briarproject.mailbox.core.system.Clock

class H2DatabaseTest : JdbcDatabaseTest() {

    override fun createDatabase(config: DatabaseConfig, clock: Clock): JdbcDatabase {
        return H2Database(config, clock)
    }

}
