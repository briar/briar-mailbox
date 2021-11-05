package org.briarproject.mailbox.core.settings

import org.briarproject.mailbox.core.db.Database
import org.briarproject.mailbox.core.db.DbException
import org.briarproject.mailbox.core.db.Transaction
import javax.annotation.concurrent.Immutable
import javax.inject.Inject

@Immutable
internal class SettingsManagerImpl @Inject constructor(private val db: Database) : SettingsManager {

    @Throws(DbException::class)
    override fun getSettings(namespace: String): Settings {
        return db.transactionWithResult(true) { txn ->
            db.getSettings(txn, namespace)
        }
    }

    @Throws(DbException::class)
    override fun getSettings(txn: Transaction, namespace: String): Settings {
        return db.getSettings(txn, namespace)
    }

    @Throws(DbException::class)
    override fun mergeSettings(s: Settings, namespace: String) {
        db.write { txn -> db.mergeSettings(txn, s, namespace) }
    }
}
