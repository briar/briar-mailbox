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

package org.briarproject.mailbox.core.util;

import org.slf4j.Logger;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.Nullable;

import static org.briarproject.mailbox.core.util.LogUtils.logException;
import static org.briarproject.mailbox.core.util.LogUtils.warn;
import static org.slf4j.LoggerFactory.getLogger;

public class IoUtils {

	private static final Logger LOG = getLogger(IoUtils.class);

	public static void deleteFileOrDir(File f) {
		if (f.isFile()) {
			delete(f);
		} else if (f.isDirectory()) {
			File[] children = f.listFiles();
			if (children == null) {
				warn(LOG,
						() -> "Could not list files in " + f.getAbsolutePath());
			} else {
				for (File child : children) deleteFileOrDir(child);
			}
			delete(f);
		}
	}

	private static void delete(File f) {
		if (!f.delete())
			warn(LOG, () -> "Could not delete " + f.getAbsolutePath());
	}

	public static void copyAndClose(InputStream in, OutputStream out) {
		byte[] buf = new byte[4096];
		try {
			while (true) {
				int read = in.read(buf);
				if (read == -1) break;
				out.write(buf, 0, read);
			}
			in.close();
			out.flush();
			out.close();
		} catch (IOException e) {
			tryToClose(in, LOG);
			tryToClose(out, LOG);
		}
	}

	public static void tryToClose(@Nullable Closeable c, Logger logger) {
		try {
			if (c != null) c.close();
		} catch (IOException e) {
			logException(logger, e, () -> "Error while closing " +
					c.getClass().getSimpleName());
		}
	}

	public static boolean isNonEmptyDirectory(File f) {
		if (!f.isDirectory()) return false;
		File[] children = f.listFiles();
		return children != null && children.length > 0;
	}
}
