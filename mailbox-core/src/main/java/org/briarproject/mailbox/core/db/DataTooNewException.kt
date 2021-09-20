package org.briarproject.mailbox.core.db

/**
 * Thrown when the database uses a newer schema than the current code.
 */
class DataTooNewException : DbException()
