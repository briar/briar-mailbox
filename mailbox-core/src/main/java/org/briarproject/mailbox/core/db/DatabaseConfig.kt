package org.briarproject.mailbox.core.db

import java.io.File

interface DatabaseConfig {

    /**
     * Returns the directory where the database stores its data.
     */
    fun getDatabaseDirectory(): File

}
