package org.briarproject.mailbox.core.tor;

import com.sun.jna.Library;
import com.sun.jna.Native;

import org.briarproject.mailbox.core.settings.SettingsManager;
import org.briarproject.mailbox.core.system.Clock;
import org.briarproject.mailbox.core.system.LocationUtils;
import org.briarproject.mailbox.core.system.ResourceProvider;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;

public class JavaTorPlugin extends TorPlugin {

    JavaTorPlugin(Executor ioExecutor,
                  SettingsManager settingsManager,
                  NetworkManager networkManager,
                  LocationUtils locationUtils,
                  Clock clock,
                  ResourceProvider resourceProvider,
                  CircumventionProvider circumventionProvider,
                  Backoff backoff,
                  @Nullable String architecture,
                  File torDirectory) {
        super(ioExecutor, settingsManager, networkManager, locationUtils, clock, resourceProvider,
                circumventionProvider, backoff, architecture, torDirectory);
    }

    @Override
    protected long getLastUpdateTime() {
        CodeSource codeSource =
                getClass().getProtectionDomain().getCodeSource();
        if (codeSource == null) throw new AssertionError("CodeSource null");
        try {
            URI path = codeSource.getLocation().toURI();
            File file = new File(path);
            return file.lastModified();
        } catch (URISyntaxException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    protected int getProcessId() {
        return CLibrary.INSTANCE.getpid();
    }

    private interface CLibrary extends Library {

        CLibrary INSTANCE = Native.load("c", CLibrary.class);

        int getpid();
    }
}
