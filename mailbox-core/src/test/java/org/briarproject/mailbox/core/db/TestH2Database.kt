package org.briarproject.mailbox.core.db

import org.briarproject.mailbox.core.system.Clock

class TestH2Database(
    config: DatabaseConfig,
    clock: Clock,
) : H2Database(config, clock) {

    /**
     * A special version of open() for testing that allows reopening a database that has been closed.
     */
    override fun open(listener: MigrationListener?): Boolean {
        connectionsLock.lock()
        try {
            closed = false
        } finally {
            connectionsLock.unlock()
        }
        return super.open(listener)
    }

}
