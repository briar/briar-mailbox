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
package org.briarproject.mailbox.android

import com.google.zxing.common.BitMatrix
import android.graphics.Bitmap
import android.graphics.Color

object QrCodeUtils {
    fun renderQrCode(matrix: BitMatrix): Bitmap {
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixels[y * width + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        val qr = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        qr.setPixels(pixels, 0, width, 0, 0, width, height)
        return qr
    }
}
