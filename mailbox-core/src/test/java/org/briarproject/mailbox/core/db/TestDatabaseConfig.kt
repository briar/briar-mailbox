package org.briarproject.mailbox.core.db

import java.io.File

class TestDatabaseConfig(testDir: File) : DatabaseConfig {

    private val dbDir: File = File(testDir, "db")

    override fun getDatabaseDirectory(): File {
        return dbDir
    }

}
