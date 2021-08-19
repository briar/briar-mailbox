package org.briarproject.mailbox.core.tor;

import static net.freehaven.tor.control.TorControlCommands.HS_ADDRESS;
import static net.freehaven.tor.control.TorControlCommands.HS_PRIVKEY;
import static org.briarproject.mailbox.core.tor.TorConstants.CONTROL_PORT;
import static org.briarproject.mailbox.core.tor.TorPlugin.State.ACTIVE;
import static org.briarproject.mailbox.core.tor.TorPlugin.State.DISABLED;
import static org.briarproject.mailbox.core.tor.TorPlugin.State.ENABLING;
import static org.briarproject.mailbox.core.tor.TorPlugin.State.INACTIVE;
import static org.briarproject.mailbox.core.tor.TorPlugin.State.STARTING_STOPPING;
import static org.briarproject.mailbox.core.util.IoUtils.copyAndClose;
import static org.briarproject.mailbox.core.util.IoUtils.tryToClose;
import static org.briarproject.mailbox.core.util.LogUtils.info;
import static org.briarproject.mailbox.core.util.LogUtils.logException;
import static org.briarproject.mailbox.core.util.LogUtils.warn;
import static org.briarproject.mailbox.core.util.PrivacyUtils.scrubOnion;
import static org.slf4j.LoggerFactory.getLogger;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;

import net.freehaven.tor.control.EventHandler;
import net.freehaven.tor.control.TorControlConnection;

import org.briarproject.mailbox.core.PoliteExecutor;
import org.briarproject.mailbox.core.lifecycle.Service;
import org.briarproject.mailbox.core.lifecycle.ServiceException;
import org.briarproject.mailbox.core.server.WebServerManager;
import org.briarproject.mailbox.core.system.Clock;
import org.briarproject.mailbox.core.system.LocationUtils;
import org.briarproject.mailbox.core.system.ResourceProvider;
import org.slf4j.Logger;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipInputStream;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

abstract class TorPlugin implements Service, EventHandler {

    private static final Logger LOG = getLogger(TorPlugin.class);

    private static final String[] EVENTS = {
            "CIRC", "ORCONN", "HS_DESC", "NOTICE", "WARN", "ERR"
    };
    private static final String OWNER = "__OwningControllerProcess";
    private static final int COOKIE_TIMEOUT_MS = 3000;
    private static final int COOKIE_POLLING_INTERVAL_MS = 200;

    private final Executor ioExecutor;
    private final Executor connectionStatusExecutor;
    private final NetworkManager networkManager;
    private final LocationUtils locationUtils;
    private final Clock clock;
    private final Backoff backoff;
    @Nullable
    private final String architecture;
    private final CircumventionProvider circumventionProvider;
    private final ResourceProvider resourceProvider;
    private final File torDirectory, geoIpFile, configFile;
    private final File doneFile, cookieFile;
    private final AtomicBoolean used = new AtomicBoolean(false);

    protected final PluginState state = new PluginState();

    private volatile Socket controlSocket = null;
    private volatile TorControlConnection controlConnection = null;

    protected abstract int getProcessId();

    protected abstract long getLastUpdateTime();

    TorPlugin(Executor ioExecutor,
              NetworkManager networkManager,
              LocationUtils locationUtils,
              Clock clock,
              ResourceProvider resourceProvider,
              CircumventionProvider circumventionProvider,
              Backoff backoff,
              @Nullable String architecture,
              File torDirectory) {
        this.ioExecutor = ioExecutor;
        this.networkManager = networkManager;
        this.locationUtils = locationUtils;
        this.clock = clock;
        this.resourceProvider = resourceProvider;
        this.circumventionProvider = circumventionProvider;
        this.backoff = backoff;
        this.architecture = architecture;
        this.torDirectory = torDirectory;
        geoIpFile = new File(torDirectory, "geoip");
        configFile = new File(torDirectory, "torrc");
        doneFile = new File(torDirectory, "done");
        cookieFile = new File(torDirectory, ".tor/control_auth_cookie");
        // Don't execute more than one connection status check at a time
        connectionStatusExecutor =
                new PoliteExecutor("TorPlugin", ioExecutor, 1);
    }

    protected File getTorExecutableFile() {
        return new File(torDirectory, "tor");
    }

    protected File getObfs4ExecutableFile() {
        return new File(torDirectory, "obfs4proxy");
    }

    @Override
    public void startService() throws ServiceException {
        if (used.getAndSet(true)) throw new IllegalStateException();
        if (!torDirectory.exists()) {
            if (!torDirectory.mkdirs()) {
                LOG.warn("Could not create Tor directory.");
                throw new ServiceException();
            }
        }
        // Install or update the assets if necessary
        if (!assetsAreUpToDate()) installAssets();
        if (cookieFile.exists() && !cookieFile.delete())
            LOG.warn("Old auth cookie not deleted");
        // Start a new Tor process
        LOG.info("Starting Tor");
        File torFile = getTorExecutableFile();
        String torPath = torFile.getAbsolutePath();
        String configPath = configFile.getAbsolutePath();
        String pid = String.valueOf(getProcessId());
        Process torProcess;
        ProcessBuilder pb =
                new ProcessBuilder(torPath, "-f", configPath, OWNER, pid);
        Map<String, String> env = pb.environment();
        env.put("HOME", torDirectory.getAbsolutePath());
        pb.directory(torDirectory);
        try {
            torProcess = pb.start();
        } catch (SecurityException | IOException e) {
            throw new ServiceException(e);
        }
        // Log the process's standard output until it detaches
        if (LOG.isInfoEnabled()) {
            Scanner stdout = new Scanner(torProcess.getInputStream());
            Scanner stderr = new Scanner(torProcess.getErrorStream());
            while (stdout.hasNextLine() || stderr.hasNextLine()) {
                if (stdout.hasNextLine()) {
                    LOG.info(stdout.nextLine());
                }
                if (stderr.hasNextLine()) {
                    LOG.info(stderr.nextLine());
                }
            }
            stdout.close();
            stderr.close();
        }
        try {
            // Wait for the process to detach or exit
            int exit = torProcess.waitFor();
            if (exit != 0) {
                warn(LOG, () -> "Tor exited with value " + exit);
                throw new ServiceException();
            }
            // Wait for the auth cookie file to be created/updated
            long start = clock.currentTimeMillis();
            while (cookieFile.length() < 32) {
                if (clock.currentTimeMillis() - start > COOKIE_TIMEOUT_MS) {
                    LOG.warn("Auth cookie not created");
                    if (LOG.isInfoEnabled()) listFiles(torDirectory);
                    throw new ServiceException();
                }
                //noinspection BusyWait
                Thread.sleep(COOKIE_POLLING_INTERVAL_MS);
            }
            LOG.info("Auth cookie created");
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while starting Tor");
            Thread.currentThread().interrupt();
            throw new ServiceException();
        }
        try {
            // Open a control connection and authenticate using the cookie file
            controlSocket = new Socket("127.0.0.1", CONTROL_PORT);
            controlConnection = new TorControlConnection(controlSocket);
            controlConnection.authenticate(read(cookieFile));
            // Tell Tor to exit when the control connection is closed
            controlConnection.takeOwnership();
            controlConnection.resetConf(singletonList(OWNER));
            // Register to receive events from the Tor process
            controlConnection.setEventHandler(this);
            controlConnection.setEvents(asList(EVENTS));
            // Check whether Tor has already bootstrapped
            String phase = controlConnection.getInfo("status/bootstrap-phase");
            if (phase != null && phase.contains("PROGRESS=100")) {
                LOG.info("Tor has already bootstrapped");
                state.setBootstrapped();
            }
        } catch (IOException e) {
            throw new ServiceException(e);
        }
        state.setStarted();
        // Check whether we're online
        updateConnectionStatus(networkManager.getNetworkStatus());
        // Create a hidden service if necessary
        ioExecutor.execute(() -> publishHiddenService(String.valueOf(WebServerManager.PORT)));
    }

    private boolean assetsAreUpToDate() {
        return doneFile.lastModified() > getLastUpdateTime();
    }

    private void installAssets() throws ServiceException {
        if (architecture == null)
            throw new ServiceException("Tor not supported on this architecture");
        try {
            // The done file may already exist from a previous installation
            //noinspection ResultOfMethodCallIgnored
            doneFile.delete();
            installTorExecutable();
            installObfs4Executable();
            extract(getGeoIpInputStream(), geoIpFile);
            extract(getConfigInputStream(), configFile);
            if (!doneFile.createNewFile())
                LOG.warn("Failed to create done file");
        } catch (IOException e) {
            throw new ServiceException(e);
        }
    }

    protected void extract(InputStream in, File dest) throws IOException {
        OutputStream out = new FileOutputStream(dest);
        copyAndClose(in, out);
    }

    protected void installTorExecutable() throws IOException {
        info(LOG, () -> "Installing Tor binary for " + architecture);
        File torFile = getTorExecutableFile();
        extract(getTorInputStream(), torFile);
        if (!torFile.setExecutable(true, true)) throw new IOException();
    }

    protected void installObfs4Executable() throws IOException {
        info(LOG, () -> "Installing obfs4proxy binary for " + architecture);
        File obfs4File = getObfs4ExecutableFile();
        extract(getObfs4InputStream(), obfs4File);
        if (!obfs4File.setExecutable(true, true)) throw new IOException();
    }

    private InputStream getTorInputStream() throws IOException {
        InputStream in = resourceProvider
                .getResourceInputStream("tor_" + architecture, ".zip");
        ZipInputStream zin = new ZipInputStream(in);
        if (zin.getNextEntry() == null) throw new IOException();
        return zin;
    }

    private InputStream getGeoIpInputStream() throws IOException {
        InputStream in = resourceProvider.getResourceInputStream("geoip",
                ".zip");
        ZipInputStream zin = new ZipInputStream(in);
        if (zin.getNextEntry() == null) throw new IOException();
        return zin;
    }

    private InputStream getObfs4InputStream() throws IOException {
        InputStream in = resourceProvider
                .getResourceInputStream("obfs4proxy_" + architecture, ".zip");
        ZipInputStream zin = new ZipInputStream(in);
        if (zin.getNextEntry() == null) throw new IOException();
        return zin;
    }

    private InputStream getConfigInputStream() {
        ClassLoader cl = getClass().getClassLoader();
        return requireNonNull(cl.getResourceAsStream("torrc"));
    }

    private void listFiles(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) for (File child : children) listFiles(child);
        } else {
            LOG.info(f.getAbsolutePath() + " " + f.length());
        }
    }

    private byte[] read(File f) throws IOException {
        byte[] b = new byte[(int) f.length()];
        FileInputStream in = new FileInputStream(f);
        try {
            int offset = 0;
            while (offset < b.length) {
                int read = in.read(b, offset, b.length - offset);
                if (read == -1) throw new EOFException();
                offset += read;
            }
            return b;
        } finally {
            tryToClose(in, LOG);
        }
    }

    private void publishHiddenService(String port) {
        if (!state.isTorRunning()) return;
        // TODO get stored key
        String privKey3 = null;
        publishV3HiddenService(port, privKey3);
    }

    private void publishV3HiddenService(String port, @Nullable String privKey) {
        LOG.info("Creating v3 hidden service");
        Map<Integer, String> portLines = singletonMap(80, "127.0.0.1:" + port);
        Map<String, String> response;
        try {
            // Use the control connection to set up the hidden service
            if (privKey == null) {
                response = controlConnection.addOnion("NEW:ED25519-V3",
                        portLines, null);
            } else {
                response = controlConnection.addOnion(privKey, portLines);
            }
        } catch (IOException e) {
            logException(LOG, e);
            return;
        }
        if (!response.containsKey(HS_ADDRESS)) {
            LOG.warn("Tor did not return a hidden service address");
            return;
        }
        if (privKey == null && !response.containsKey(HS_PRIVKEY)) {
            LOG.warn("Tor did not return a private key");
            return;
        }
        String onion3 = response.get(HS_ADDRESS);
        info(LOG, () -> "V3 hidden service " + scrubOnion(onion3));
        // TODO remove
        LOG.warn("V3 hidden service: http://" + onion3 + ".onion");
        if (privKey == null) {
            // TODO Save the hidden service's onion hostname
//			p.put(PROP_ONION_V3, onion3);
            // TODO Save the hidden service's private key for next time
//			s.put(HS_PRIVATE_KEY_V3, response.get(HS_PRIVKEY));
//			s.put(HS_V3_CREATED, String.valueOf(clock.currentTimeMillis()));
        }
    }

    protected void enableNetwork(boolean enable) throws IOException {
        state.enableNetwork(enable);
        controlConnection.setConf("DisableNetwork", enable ? "0" : "1");
    }

    private void enableBridges(boolean enable, boolean needsMeek)
            throws IOException {
        if (enable) {
            Collection<String> conf = new ArrayList<>();
            conf.add("UseBridges 1");
            File obfs4File = getObfs4ExecutableFile();
            if (needsMeek) {
                conf.add("ClientTransportPlugin meek_lite exec " +
                        obfs4File.getAbsolutePath());
            } else {
                conf.add("ClientTransportPlugin obfs4 exec " +
                        obfs4File.getAbsolutePath());
            }
            conf.addAll(circumventionProvider.getBridges(needsMeek));
            controlConnection.setConf(conf);
        } else {
            controlConnection.setConf("UseBridges", "0");
        }
    }

    @Override
    public void stopService() {
        ServerSocket ss = state.setStopped();
        tryToClose(ss, LOG);
        if (controlSocket != null && controlConnection != null) {
            try {
                LOG.info("Stopping Tor");
                controlConnection.setConf("DisableNetwork", "1");
                controlConnection.shutdownTor("TERM");
                controlSocket.close();
            } catch (IOException e) {
                logException(LOG, e);
            }
        }
    }

    @Override
    public void circuitStatus(String status, String id, String path) {
        if (status.equals("BUILT") &&
                state.getAndSetCircuitBuilt()) {
            LOG.info("First circuit built");
            backoff.reset();
        }
    }

    @Override
    public void streamStatus(String status, String id, String target) {
    }

    @Override
    public void orConnStatus(String status, String orName) {
        info(LOG, () -> "OR connection " + status + " " + orName);
        if (status.equals("CLOSED") || status.equals("FAILED")) {
            // Check whether we've lost connectivity
            updateConnectionStatus(networkManager.getNetworkStatus()
            );
        }
    }

    @Override
    public void bandwidthUsed(long read, long written) {
    }

    @Override
    public void newDescriptors(List<String> orList) {
    }

    @Override
    public void message(String severity, String msg) {
        info(LOG, () -> severity + " " + msg);
        if (severity.equals("NOTICE") && msg.startsWith("Bootstrapped 100%")) {
            state.setBootstrapped();
            backoff.reset();
        }
    }

    @Override
    public void unrecognized(String type, String msg) {
        if (type.equals("HS_DESC") && msg.startsWith("UPLOADED")) {
            LOG.info("V3 descriptor uploaded");
        }
    }

    public void onNetworkStatusChanged() {
        updateConnectionStatus(networkManager.getNetworkStatus());
    }

    private void disableNetwork() {
        connectionStatusExecutor.execute(() -> {
            try {
                if (state.isTorRunning()) enableNetwork(false);
            } catch (IOException ex) {
                logException(LOG, ex);
            }
        });
    }

    private void updateConnectionStatus(NetworkStatus status) {
        connectionStatusExecutor.execute(() -> {
            if (!state.isTorRunning()) return;
            boolean online = status.isConnected();
            boolean wifi = status.isWifi();
            boolean ipv6Only = status.isIpv6Only();
            String country = locationUtils.getCurrentCountry();
            boolean blocked =
                    circumventionProvider.isTorProbablyBlocked(country);
            boolean bridgesWork = circumventionProvider.doBridgesWork(country);

            if (LOG.isInfoEnabled()) {
                LOG.info("Online: " + online + ", wifi: " + wifi
                        + ", IPv6 only: " + ipv6Only);
                if (country.isEmpty()) LOG.info("Country code unknown");
                else LOG.info("Country code: " + country);
            }

            int reasonsDisabled = 0;
            boolean enableNetwork = false;
            boolean enableBridges = false;
            boolean useMeek = false;

            if (!online) {
                LOG.info("Disabling network, device is offline");
            } else {
                LOG.info("Enabling network");
                enableNetwork = true;
                if (blocked && bridgesWork) {
                    if (ipv6Only || circumventionProvider.needsMeek(country)) {
                        LOG.info("Using meek bridges");
                        enableBridges = true;
                        useMeek = true;
                    } else {
                        LOG.info("Using obfs4 bridges");
                        enableBridges = true;
                    }
                } else {
                    LOG.info("Not using bridges");
                }
            }
            state.setReasonsDisabled(reasonsDisabled);
            try {
                if (enableNetwork) {
                    enableBridges(enableBridges, useMeek);
                    enableConnectionPadding(true);
                    useIpv6(ipv6Only);
                }
                enableNetwork(enableNetwork);
            } catch (IOException e) {
                logException(LOG, e);
            }
        });
    }

    private void enableConnectionPadding(boolean enable) throws IOException {
        controlConnection.setConf("ConnectionPadding", enable ? "1" : "0");
    }

    private void useIpv6(boolean ipv6Only) throws IOException {
        controlConnection.setConf("ClientUseIPv4", ipv6Only ? "0" : "1");
        controlConnection.setConf("ClientUseIPv6", ipv6Only ? "1" : "0");
    }

    @ThreadSafe
    protected class PluginState {

        @GuardedBy("this")
        private boolean started = false,
                stopped = false,
                networkInitialised = false,
                networkEnabled = false,
                bootstrapped = false,
                circuitBuilt = false,
                settingsChecked = false;

        @GuardedBy("this")
        private int reasonsDisabled = 0;

        @GuardedBy("this")
        @Nullable
        private ServerSocket serverSocket = null;

        synchronized void setStarted() {
            started = true;
//            callback.pluginStateChanged(getState());
        }

        synchronized boolean isTorRunning() {
            return started && !stopped;
        }

        @Nullable
        synchronized ServerSocket setStopped() {
            stopped = true;
            ServerSocket ss = serverSocket;
            serverSocket = null;
//            callback.pluginStateChanged(getState());
            return ss;
        }

        synchronized void setBootstrapped() {
            bootstrapped = true;
//            callback.pluginStateChanged(getState());
        }

        synchronized boolean getAndSetCircuitBuilt() {
            boolean firstCircuit = !circuitBuilt;
            circuitBuilt = true;
//            callback.pluginStateChanged(getState());
            return firstCircuit;
        }

        synchronized void enableNetwork(boolean enable) {
            networkInitialised = true;
            networkEnabled = enable;
            if (!enable) circuitBuilt = false;
//            callback.pluginStateChanged(getState());
        }

        synchronized void setReasonsDisabled(int reasonsDisabled) {
            settingsChecked = true;
            this.reasonsDisabled = reasonsDisabled;
//            callback.pluginStateChanged(getState());
        }

        synchronized State getState() {
            if (!started || stopped || !settingsChecked) {
                return STARTING_STOPPING;
            }
            if (reasonsDisabled != 0) return DISABLED;
            if (!networkInitialised) return ENABLING;
            if (!networkEnabled) return INACTIVE;
            return bootstrapped && circuitBuilt ? ACTIVE : ENABLING;
        }

        synchronized int getReasonsDisabled() {
            return getState() == DISABLED ? reasonsDisabled : 0;
        }

    }

    enum State {

        /**
         * The plugin has not finished starting or has been stopped.
         */
        STARTING_STOPPING,

        /**
         * The plugin is disabled by settings.
         */
        DISABLED,

        /**
         * The plugin is being enabled and can't yet make or receive
         * connections.
         */
        ENABLING,

        /**
         * The plugin is enabled and can make or receive connections.
         */
        ACTIVE,

        /**
         * The plugin is enabled but can't make or receive connections
         */
        INACTIVE
    }
}
