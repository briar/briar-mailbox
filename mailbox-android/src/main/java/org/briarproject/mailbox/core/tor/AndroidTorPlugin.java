package org.briarproject.mailbox.core.tor;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

import org.briarproject.mailbox.core.settings.SettingsManager;
import org.briarproject.mailbox.core.system.AndroidWakeLock;
import org.briarproject.mailbox.core.system.AndroidWakeLockManager;
import org.briarproject.mailbox.core.system.Clock;
import org.briarproject.mailbox.core.system.LocationUtils;
import org.briarproject.mailbox.core.system.ResourceProvider;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.Nullable;

import static android.os.Build.VERSION.SDK_INT;
import static java.util.Arrays.asList;
import static org.briarproject.mailbox.core.util.LogUtils.info;
import static org.slf4j.LoggerFactory.getLogger;

public class AndroidTorPlugin extends TorPlugin {

    private static final List<String> LIBRARY_ARCHITECTURES =
            asList("armeabi-v7a", "arm64-v8a", "x86", "x86_64");

    private static final String TOR_LIB_NAME = "libtor.so";
    private static final String OBFS4_LIB_NAME = "libobfs4proxy.so";

    private static final Logger LOG = getLogger(AndroidTorPlugin.class);

    private final Context ctx;
    private final AndroidWakeLock wakeLock;
    private final File torLib, obfs4Lib;

    AndroidTorPlugin(Executor ioExecutor,
                     Context ctx,
                     SettingsManager settingsManager,
                     NetworkManager networkManager,
                     LocationUtils locationUtils,
                     Clock clock,
                     ResourceProvider resourceProvider,
                     CircumventionProvider circumventionProvider,
                     AndroidWakeLockManager wakeLockManager,
                     Backoff backoff,
                     @Nullable String architecture,
                     File torDirectory) {
        super(ioExecutor, settingsManager, networkManager, locationUtils, clock, resourceProvider, circumventionProvider, backoff, architecture, torDirectory);
        this.ctx = ctx;
        wakeLock = wakeLockManager.createWakeLock("TorPlugin");
        String nativeLibDir = ctx.getApplicationInfo().nativeLibraryDir;
        torLib = new File(nativeLibDir, TOR_LIB_NAME);
        obfs4Lib = new File(nativeLibDir, OBFS4_LIB_NAME);
    }

    @Override
    protected int getProcessId() {
        return android.os.Process.myPid();
    }

    @Override
    protected long getLastUpdateTime() {
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), 0);
            return pi.lastUpdateTime;
        } catch (NameNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    protected void enableNetwork(boolean enable) throws IOException {
        if (enable) wakeLock.acquire();
        super.enableNetwork(enable);
        if (!enable) wakeLock.release();
    }

    @Override
    public void stopService() {
        super.stopService();
        wakeLock.release();
    }

    @Override
    protected File getTorExecutableFile() {
        return torLib.exists() ? torLib : super.getTorExecutableFile();
    }

    @Override
    protected File getObfs4ExecutableFile() {
        return obfs4Lib.exists() ? obfs4Lib : super.getObfs4ExecutableFile();
    }

    @Override
    protected void installTorExecutable() throws IOException {
        File extracted = super.getTorExecutableFile();
        if (torLib.exists()) {
            // If an older version left behind a Tor binary, delete it
            if (extracted.exists()) {
                if (extracted.delete()) LOG.info("Deleted Tor binary");
                else LOG.info("Failed to delete Tor binary");
            }
        } else if (SDK_INT < 29) {
            // The binary wasn't extracted at install time. Try to extract it
            extractLibraryFromApk(TOR_LIB_NAME, extracted);
        } else {
            // No point extracting the binary, we won't be allowed to execute it
            throw new FileNotFoundException(torLib.getAbsolutePath());
        }
    }

    @Override
    protected void installObfs4Executable() throws IOException {
        File extracted = super.getObfs4ExecutableFile();
        if (obfs4Lib.exists()) {
            // If an older version left behind an obfs4 binary, delete it
            if (extracted.exists()) {
                if (extracted.delete()) LOG.info("Deleted obfs4 binary");
                else LOG.info("Failed to delete obfs4 binary");
            }
        } else if (SDK_INT < 29) {
            // The binary wasn't extracted at install time. Try to extract it
            extractLibraryFromApk(OBFS4_LIB_NAME, extracted);
        } else {
            // No point extracting the binary, we won't be allowed to execute it
            throw new FileNotFoundException(obfs4Lib.getAbsolutePath());
        }
    }

    private void extractLibraryFromApk(String libName, File dest)
            throws IOException {
        File sourceDir = new File(ctx.getApplicationInfo().sourceDir);
        if (sourceDir.isFile()) {
            // Look for other APK files in the same directory, if we're allowed
            File parent = sourceDir.getParentFile();
            if (parent != null) sourceDir = parent;
        }
        List<String> libPaths = getSupportedLibraryPaths(libName);
        for (File apk : findApkFiles(sourceDir)) {
            ZipInputStream zin = new ZipInputStream(new FileInputStream(apk));
            for (ZipEntry e = zin.getNextEntry(); e != null;
                 e = zin.getNextEntry()) {
                if (libPaths.contains(e.getName())) {
                    String ex = e.getName();
                    info(LOG, () -> "Extracting " + ex + " from " + apk.getAbsolutePath());
                    extract(zin, dest); // Zip input stream will be closed
                    return;
                }
            }
            zin.close();
        }
        throw new FileNotFoundException(libName);
    }

    /**
     * Returns all files with the extension .apk or .APK under the given root.
     */
    private List<File> findApkFiles(File root) {
        List<File> files = new ArrayList<>();
        findApkFiles(root, files);
        return files;
    }

    private void findApkFiles(File f, List<File> files) {
        if (f.isFile() && f.getName().toLowerCase().endsWith(".apk")) {
            files.add(f);
        } else if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File child : children) findApkFiles(child, files);
            }
        }
    }

    /**
     * Returns the paths at which libraries with the given name would be found
     * inside an APK file, for all architectures supported by the device, in
     * order of preference.
     */
    private List<String> getSupportedLibraryPaths(String libName) {
        List<String> architectures = new ArrayList<>();
        for (String abi : getSupportedArchitectures()) {
            if (LIBRARY_ARCHITECTURES.contains(abi)) {
                architectures.add("lib/" + abi + "/" + libName);
            }
        }
        return architectures;
    }

    static Collection<String> getSupportedArchitectures() {
        List<String> abis = new ArrayList<>();
        if (SDK_INT >= 21) {
            abis.addAll(asList(Build.SUPPORTED_ABIS));
        } else {
            abis.add(Build.CPU_ABI);
            if (Build.CPU_ABI2 != null) abis.add(Build.CPU_ABI2);
        }
        return abis;
    }
}
