package org.briarproject.mailbox.core.db;

import javax.annotation.Nullable;

/**
 * Encapsulates the database implementation and exposes high-level operations
 * to other components.
 */
public interface DatabaseComponent {

    /**
     * Opens the database and returns true if the database already existed.
     */
    boolean open(@Nullable MigrationListener listener);

    /**
     * Waits for any open transactions to finish and closes the database.
     */
    void close();

}
