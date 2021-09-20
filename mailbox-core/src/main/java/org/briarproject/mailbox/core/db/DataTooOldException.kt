package org.briarproject.mailbox.core.db

/**
 * Thrown when the database uses an older schema than the current code and
 * cannot be migrated.
 */
class DataTooOldException : DbException()
