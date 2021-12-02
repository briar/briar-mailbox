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

package org.briarproject.mailbox.cli

import com.google.zxing.common.BitMatrix

object QrCodeRenderer {

    private const val SET = "██"
    private const val UNSET = "  "

    internal fun getQrString(bitMatrix: BitMatrix): String = StringBuilder().apply {
        append(System.lineSeparator())
        for (y in 0 until bitMatrix.height) {
            for (x in 0 until bitMatrix.width) {
                append(if (bitMatrix[x, y]) SET else UNSET)
            }
            append(System.lineSeparator())
        }
    }.toString()

}
