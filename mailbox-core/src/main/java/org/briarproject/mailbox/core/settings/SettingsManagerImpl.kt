package org.briarproject.mailbox.core.settings

import org.briarproject.mailbox.core.db.Database
import org.briarproject.mailbox.core.db.DbException
import java.sql.Connection
import javax.annotation.concurrent.Immutable
import javax.inject.Inject

@Immutable
internal class SettingsManagerImpl @Inject constructor(private val db: Database) : SettingsManager {

    @Throws(DbException::class)
    override fun getSettings(namespace: String): Settings {
        return db.transactionWithResult(true) { txn: Connection ->
            db.getSettings(txn, namespace)
        }
    }

    @Throws(DbException::class)
    override fun getSettings(txn: Connection, namespace: String): Settings {
        return db.getSettings(txn, namespace)
    }

    @Throws(DbException::class)
    override fun mergeSettings(s: Settings, namespace: String) {
        db.transaction(false) { txn: Connection -> db.mergeSettings(txn, s, namespace) }
    }
}
