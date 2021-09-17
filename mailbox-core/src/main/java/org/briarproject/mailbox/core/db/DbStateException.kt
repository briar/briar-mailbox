package org.briarproject.mailbox.core.db

import java.sql.SQLException

/**
 * Thrown when the database is in an illegal state.
 */
class DbStateException : SQLException()
