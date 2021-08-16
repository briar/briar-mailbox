package org.briarproject.mailbox.core.util;

import static org.briarproject.mailbox.core.util.LogUtils.logException;
import static org.briarproject.mailbox.core.util.LogUtils.warn;
import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import javax.annotation.Nullable;

public class IoUtils {

    private static final Logger LOG = getLogger(IoUtils.class);

    public static void deleteFileOrDir(File f) {
        if (f.isFile()) {
            delete(f);
        } else if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children == null) {
                warn(LOG, () -> "Could not list files in " + f.getAbsolutePath());
            } else {
                for (File child : children) deleteFileOrDir(child);
            }
            delete(f);
        }
    }

    private static void delete(File f) {
        if (!f.delete()) warn(LOG, () -> "Could not delete " + f.getAbsolutePath());
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
            logException(logger, e);
        }
    }

    public static void tryToClose(@Nullable Socket s, Logger logger) {
        try {
            if (s != null) s.close();
        } catch (IOException e) {
            logException(logger, e);
        }
    }

    public static void tryToClose(@Nullable ServerSocket ss, Logger logger) {
        try {
            if (ss != null) ss.close();
        } catch (IOException e) {
            logException(logger, e);
        }
    }

    public static void read(InputStream in, byte[] b) throws IOException {
        int offset = 0;
        while (offset < b.length) {
            int read = in.read(b, offset, b.length - offset);
            if (read == -1) throw new EOFException();
            offset += read;
        }
    }

    // Workaround for a bug in Android 7, see
    // https://android-review.googlesource.com/#/c/271775/
    public static InputStream getInputStream(Socket s) throws IOException {
        try {
            return s.getInputStream();
        } catch (NullPointerException e) {
            throw new IOException(e);
        }
    }

    // Workaround for a bug in Android 7, see
    // https://android-review.googlesource.com/#/c/271775/
    public static OutputStream getOutputStream(Socket s) throws IOException {
        try {
            return s.getOutputStream();
        } catch (NullPointerException e) {
            throw new IOException(e);
        }
    }

    public static boolean isNonEmptyDirectory(File f) {
        if (!f.isDirectory()) return false;
        File[] children = f.listFiles();
        return children != null && children.length > 0;
    }
}
