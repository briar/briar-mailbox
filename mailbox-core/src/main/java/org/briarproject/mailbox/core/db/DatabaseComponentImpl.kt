package org.briarproject.mailbox.core.db

import javax.inject.Inject

class DatabaseComponentImpl<T> @Inject constructor(database: Database<T>) : DatabaseComponent {

    private val db: Database<T>? = null

    override fun open(listener: MigrationListener?): Boolean {
        // TODO: implement this
        return true
    }

    override fun close() {
        // TODO: implement this
    }

}
