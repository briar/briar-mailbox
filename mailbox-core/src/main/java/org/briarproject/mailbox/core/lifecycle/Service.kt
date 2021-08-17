package org.briarproject.mailbox.core.lifecycle

import org.briarproject.mailbox.core.system.Wakeful

interface Service {

    /**
     * Starts the service. This method must not be called concurrently with [stopService].
     */
    @Wakeful
    @Throws(ServiceException::class)
    fun startService()

    /**
     * Stops the service. This method must not be called concurrently with
     * [startService].
     */
    @Wakeful
    @Throws(ServiceException::class)
    fun stopService()

}
