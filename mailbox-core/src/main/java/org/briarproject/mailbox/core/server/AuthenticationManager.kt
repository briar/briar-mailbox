package org.briarproject.mailbox.core.server

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthenticationManager @Inject constructor() {

    fun canOwnerAccess(credentials: Credentials): Boolean {
        // TODO check credentials:
        //  * token must be from owner
        //  * if credentials.folderId is not null, must have accessType right to folderId
        return credentials.token == "test123"
    }

    fun canOwnerOrContactAccess(credentials: Credentials): Boolean {
        require(credentials.folderId != null)
        // TODO check credentials:
        //  * token must have credentials.accessType right to folderId
        return credentials.token == "test123" || credentials.token == "test124"
    }

}
