/*
 *     Briar Mailbox
 *     Copyright (C) 2021-2022  The Briar Project
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package org.briarproject.mailbox.core.files

import java.io.File

interface FileProvider {
    /**
     * The root files directory.
     * Attention: This is not guaranteed to be the parent of other files on all platforms.
     *            Also this directory and all of its content are deleted during wipe,
     *            so make sure this is a directory where this doesn't do any harm,
     *            i.e. a directory used for the mailbox only.
     */
    val root: File
    val folderRoot: File
    fun getTemporaryFile(fileId: String): File
    fun getFolder(folderId: String): File
    fun getFile(folderId: String, fileId: String): File
}
