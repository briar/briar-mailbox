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

package org.briarproject.mailbox.core.tor;

import net.freehaven.tor.control.EventHandler;
import net.freehaven.tor.control.TorControlConnection;
import net.freehaven.tor.control.TorNotRunningException;

import org.briarproject.mailbox.core.PoliteExecutor;
import org.briarproject.mailbox.core.db.DbException;
import org.briarproject.mailbox.core.event.Event;
import org.briarproject.mailbox.core.event.EventListener;
import org.briarproject.mailbox.core.lifecycle.IoExecutor;
import org.briarproject.mailbox.core.lifecycle.ServiceException;
import org.briarproject.mailbox.core.settings.Settings;
import org.briarproject.mailbox.core.settings.SettingsManager;
import org.briarproject.mailbox.core.system.Clock;
import org.briarproject.mailbox.core.system.LocationUtils;
import org.briarproject.mailbox.core.system.ResourceProvider;
import org.briarproject.mailbox.core.tor.CircumventionProvider.BridgeType;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import io.netty.util.IntSupplier;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static kotlinx.coroutines.flow.StateFlowKt.MutableStateFlow;
import static net.freehaven.tor.control.TorControlCommands.HS_ADDRESS;
import static net.freehaven.tor.control.TorControlCommands.HS_PRIVKEY;
import static org.briarproject.mailbox.core.tor.CircumventionProvider.BridgeType.MEEK;
import static org.briarproject.mailbox.core.tor.TorConstants.CONTROL_PORT;
import static org.briarproject.mailbox.core.tor.TorConstants.HS_ADDRESS_V3;
import static org.briarproject.mailbox.core.tor.TorConstants.HS_PRIVATE_KEY_V3;
import static org.briarproject.mailbox.core.tor.TorConstants.SETTINGS_NAMESPACE;
import static org.briarproject.mailbox.core.tor.TorConstants.SOCKS_PORT;
import static org.briarproject.mailbox.core.util.IoUtils.copyAndClose;
import static org.briarproject.mailbox.core.util.IoUtils.tryToClose;
import static org.briarproject.mailbox.core.util.LogUtils.info;
import static org.briarproject.mailbox.core.util.LogUtils.logException;
import static org.briarproject.mailbox.core.util.LogUtils.warn;
import static org.briarproject.mailbox.core.util.PrivacyUtils.scrubOnion;
import static org.slf4j.LoggerFactory.getLogger;

public abstract class AbstractTorPlugin
		implements TorPlugin, EventHandler, EventListener {

	private static final Logger LOG = getLogger(AbstractTorPlugin.class);

	private static final String[] EVENTS = {
			"CIRC",
			"ORCONN",
			"STATUS_GENERAL",
			"STATUS_CLIENT",
			"HS_DESC",
			"NOTICE",
			"WARN",
			"ERR"
	};
	private static final String OWNER = "__OwningControllerProcess";
	private static final int COOKIE_TIMEOUT_MS = 3000;
	private static final int COOKIE_POLLING_INTERVAL_MS = 200;
	/**
	 * The number of uploads of our onion service descriptor we wait for
	 * before we consider our onion service to be published.
	 * In reality, the actual reachability is more complicated,
	 * but this might be a reasonable heuristic.
	 */
	private static final int HS_DESC_UPLOADS = 1;
	private final Pattern bootstrapPattern =
			Pattern.compile("^Bootstrapped ([0-9]{1,3})%.*$");
	private final Pattern clockSkewPattern = Pattern.compile("CLOCK_SKEW");

	private final Executor ioExecutor;
	private final Executor connectionStatusExecutor;
	private final SettingsManager settingsManager;
	private final NetworkManager networkManager;
	private final LocationUtils locationUtils;
	private final Clock clock;
	@Nullable
	private final String architecture;
	private final CircumventionProvider circumventionProvider;
	private final ResourceProvider resourceProvider;
	private final File torDirectory, configFile;
	private final File doneFile, cookieFile;
	private final IntSupplier portSupplier;
	private final AtomicBoolean used = new AtomicBoolean(false);

	protected final PluginState state = new PluginState();

	private volatile Socket controlSocket = null;
	private volatile TorControlConnection controlConnection = null;

	protected abstract int getProcessId();

	protected abstract long getLastUpdateTime();

	AbstractTorPlugin(Executor ioExecutor,
			SettingsManager settingsManager,
			NetworkManager networkManager,
			LocationUtils locationUtils,
			Clock clock,
			ResourceProvider resourceProvider,
			CircumventionProvider circumventionProvider,
			@Nullable String architecture,
			File torDirectory,
			IntSupplier portSupplier) {
		this.ioExecutor = ioExecutor;
		this.settingsManager = settingsManager;
		this.networkManager = networkManager;
		this.locationUtils = locationUtils;
		this.clock = clock;
		this.resourceProvider = resourceProvider;
		this.circumventionProvider = circumventionProvider;
		this.architecture = architecture;
		this.torDirectory = torDirectory;
		configFile = new File(torDirectory, "torrc");
		doneFile = new File(torDirectory, "done");
		cookieFile = new File(torDirectory, ".tor/control_auth_cookie");
		this.portSupplier = portSupplier;
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

	public StateFlow<TorState> getState() {
		return state.state;
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
		try {
			// Install or update the assets if necessary
			if (!assetsAreUpToDate()) installAssets();
			// Start from the default config every time
			extract(getConfigInputStream(), configFile);
		} catch (IOException e) {
			throw new ServiceException(e);
		}
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
		pb.redirectErrorStream(true);
		try {
			torProcess = pb.start();
		} catch (SecurityException | IOException e) {
			throw new ServiceException(e);
		}
		try {
			// Wait for the Tor process to start
			waitForTorToStart(torProcess);
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
			String info = controlConnection.getInfo("status/bootstrap-phase");
			if (info != null && info.contains("PROGRESS=100")) {
				LOG.info("Tor has already bootstrapped");
				state.setBootstrapPercent(100);
			}
			// Check whether Tor has already built a circuit
			info = controlConnection.getInfo("status/circuit-established");
			if ("1".equals(info)) {
				LOG.info("Tor has already built a circuit");
				state.setCircuitBuilt(true);
			}
		} catch (IOException e) {
			throw new ServiceException(e);
		}
		state.setStarted();
		// Check whether we're online
		updateConnectionStatus(networkManager.getNetworkStatus());
		// Create a hidden service if necessary
		ioExecutor.execute(() -> {
			int port;
			try {
				port = portSupplier.get();
			} catch (Exception e) {
				throw new AssertionError(e);
			}
			info(LOG, () -> "Binding hidden service to port: " + port);
			publishHiddenService(String.valueOf(port));
		});
	}

	private boolean assetsAreUpToDate() {
		return doneFile.lastModified() > getLastUpdateTime();
	}

	private void installAssets() throws ServiceException {
		if (architecture == null)
			throw new ServiceException(
					"Tor not supported on this architecture");
		try {
			// The done file may already exist from a previous installation
			//noinspection ResultOfMethodCallIgnored
			doneFile.delete();
			installTorExecutable();
			installObfs4Executable();
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

	private InputStream getObfs4InputStream() throws IOException {
		InputStream in = resourceProvider
				.getResourceInputStream("obfs4proxy_" + architecture, ".zip");
		ZipInputStream zin = new ZipInputStream(in);
		if (zin.getNextEntry() == null) throw new IOException();
		return zin;
	}

	private static void append(StringBuilder strb, String name, Object value) {
		strb.append(name);
		strb.append(" ");
		strb.append(value);
		strb.append("\n");
	}

	private InputStream getConfigInputStream() {
		File dataDirectory = new File(torDirectory, ".tor");
		StringBuilder strb = new StringBuilder();
		append(strb, "ControlPort", CONTROL_PORT);
		append(strb, "CookieAuthentication", 1);
		append(strb, "DataDirectory", dataDirectory.getAbsolutePath());
		append(strb, "DisableNetwork", 1);
		append(strb, "RunAsDaemon", 1);
		append(strb, "SafeSocks", 1);
		append(strb, "SocksPort", SOCKS_PORT);
		strb.append("GeoIPFile\n");
		strb.append("GeoIPv6File\n");
		append(strb, "ConnectionPadding", 0);
		String obfs4Path = getObfs4ExecutableFile().getAbsolutePath();
		append(strb, "ClientTransportPlugin obfs4 exec", obfs4Path);
		append(strb, "ClientTransportPlugin meek_lite exec", obfs4Path);
		//noinspection CharsetObjectCanBeUsed
		return new ByteArrayInputStream(
				strb.toString().getBytes(Charset.forName("UTF-8")));
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

	protected void waitForTorToStart(Process torProcess)
			throws InterruptedException, ServiceException {
		Scanner stdout = new Scanner(torProcess.getInputStream());
		// Log the first line of stdout (contains Tor and library versions)
		if (stdout.hasNextLine()) LOG.info(stdout.nextLine());
		// Read the process's stdout (and redirected stderr) until it detaches
		while (stdout.hasNextLine()) stdout.nextLine();
		stdout.close();
		// Wait for the process to detach or exit
		int exit = torProcess.waitFor();
		if (exit != 0) {
			warn(LOG, () -> "Tor exited with value " + exit);
			throw new ServiceException();
		}
	}

	@IoExecutor
	private void publishHiddenService(String port) {
		if (!state.isTorRunning()) return;

		Settings s;
		try {
			s = settingsManager.getSettings(SETTINGS_NAMESPACE);
		} catch (DbException e) {
			logException(LOG, e, "Error while retrieving settings");
			s = new Settings();
		}
		String privateKey3 = s.get(HS_PRIVATE_KEY_V3);
		createV3HiddenService(port, privateKey3);
	}

	@IoExecutor
	private void createV3HiddenService(String port, @Nullable String privKey) {
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
		} catch (TorNotRunningException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			logException(LOG, e, "Error while add onion service");
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
		Settings s = new Settings();
		String onion3 = response.get(HS_ADDRESS);
		s.put(HS_ADDRESS_V3, onion3);
		info(LOG, () -> "V3 hidden service " + scrubOnion(onion3));

		if (privKey == null) {
			s.put(HS_PRIVATE_KEY_V3, response.get(HS_PRIVKEY));
			try {
				settingsManager.mergeSettings(s, SETTINGS_NAMESPACE);
			} catch (DbException e) {
				logException(LOG, e, "Error while merging settings");
			}
		}
	}

	@Nullable
	public String getHiddenServiceAddress() throws DbException {
		Settings s = settingsManager.getSettings(SETTINGS_NAMESPACE);
		return s.get(HS_ADDRESS_V3);
	}

	protected void enableNetwork(boolean enable) throws IOException {
		if (!state.enableNetwork(enable)) return; // Unchanged
		controlConnection.setConf("DisableNetwork", enable ? "0" : "1");
	}

	private void enableBridges(List<BridgeType> bridgeTypes)
			throws IOException {
		if (!state.setBridgeTypes(bridgeTypes)) return; // Unchanged
		if (bridgeTypes.isEmpty()) {
			controlConnection.setConf("UseBridges", "0");
			controlConnection.resetConf(singletonList("Bridge"));
		} else {
			Collection<String> conf = new ArrayList<>();
			conf.add("UseBridges 1");
			for (BridgeType bridgeType : bridgeTypes) {
				conf.addAll(circumventionProvider.getBridges(bridgeType));
			}
			controlConnection.setConf(conf);
		}
	}

	@Override
	public void stopService() {
		state.setStopped();
		if (controlSocket != null && controlConnection != null) {
			try {
				LOG.info("Stopping Tor");
				controlConnection.shutdownTor("TERM");
				controlSocket.close();
			} catch (IOException e) {
				logException(LOG, e,
						"Error while sending tor shutdown instructions");
			}
		}
	}

	@Override
	public void circuitStatus(String status, String id, String path) {
		// In case of races between receiving CIRCUIT_ESTABLISHED and setting
		// DisableNetwork, set our circuitBuilt flag if not already set
		if (status.equals("BUILT") && state.setCircuitBuilt(true)) {
			LOG.info("Circuit built");
		}
	}

	@Override
	public void streamStatus(String status, String id, String target) {
	}

	@Override
	public void orConnStatus(String status, String orName) {
		info(LOG, () -> "OR connection " + status);

		if (status.equals("CONNECTED")) state.onOrConnectionConnected();
		else if (status.equals("CLOSED")) state.onOrConnectionClosed();
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
		if (severity.equals("NOTICE")) {
			Matcher matcher = bootstrapPattern.matcher(msg);
			if (matcher.matches()) {
				String percentStr = matcher.group(1);
				int percent = Integer.parseInt(percentStr);
				state.setBootstrapPercent(percent);
			}
		} else if (severity.equals("WARN")) {
			Matcher matcher = clockSkewPattern.matcher(msg);
			if (matcher.find()) state.setClockSkewed();
		}
	}

	@Override
	public void unrecognized(String type, String msg) {
		if (type.equals("STATUS_CLIENT")) {
			handleClientStatus(removeSeverity(msg));
		} else if (type.equals("STATUS_GENERAL")) {
			handleGeneralStatus(removeSeverity(msg));
		} else if (type.equals("HS_DESC") && msg.startsWith("UPLOADED")) {
			LOG.info("V3 descriptor uploaded");
			state.onServiceDescriptorUploaded();
		}
	}

	private String removeSeverity(String msg) {
		return msg.replaceFirst("[^ ]+ ", "");
	}

	private void handleClientStatus(String msg) {
		if (msg.startsWith("BOOTSTRAP PROGRESS=100")) {
			LOG.info("Bootstrapped");
			state.setBootstrapPercent(100);
		} else if (msg.startsWith("CIRCUIT_ESTABLISHED")) {
			if (state.setCircuitBuilt(true)) {
				LOG.info("Circuit built");
			}
		} else if (msg.startsWith("CIRCUIT_NOT_ESTABLISHED")) {
			if (state.setCircuitBuilt(false)) {
				LOG.info("Circuit not built");
				// TODO: Disable and re-enable network to prompt Tor to rebuild
				//  its guard/bridge connections? This will also close any
				//  established circuits, which might still be functioning
			}
		}
	}

	private void handleGeneralStatus(String msg) {
		if (msg.startsWith("CLOCK_JUMPED")) {
			Long time = parseLongArgument(msg, "TIME");
			if (time != null) {
				warn(LOG, () -> "Clock jumped " + time + " seconds");
			}
		} else if (msg.startsWith("CLOCK_SKEW")) {
			Long skew = parseLongArgument(msg, "SKEW");
			if (skew != null) {
				warn(LOG, () -> "Clock is skewed by " + skew + " seconds");
			}
		}
	}

	@Nullable
	private Long parseLongArgument(String msg, String argName) {
		String[] args = msg.split(" ");
		for (String arg : args) {
			if (arg.startsWith(argName + "=")) {
				try {
					return Long.parseLong(arg.substring(argName.length() + 1));
				} catch (NumberFormatException e) {
					break;
				}
			}
		}
		warn(LOG, () -> "Failed to parse " + argName + " from '" + msg + "'");
		return null;
	}

	@Override
	public void controlConnectionClosed() {
		if (state.isTorRunning()) {
			throw new RuntimeException("Control connection closed");
		}
	}

	@Override
	public void eventOccurred(Event e) {
		if (e instanceof NetworkStatusEvent) {
			updateConnectionStatus(((NetworkStatusEvent) e).getStatus());
		}
	}

	private void updateConnectionStatus(NetworkStatus status) {
		connectionStatusExecutor.execute(() -> {
			if (!state.isTorRunning()) return;
			boolean online = status.isConnected();
			boolean wifi = status.isWifi();
			boolean ipv6Only = status.isIpv6Only();
			String country = locationUtils.getCurrentCountry();
			boolean bridgesWork = circumventionProvider.doBridgesWork(country);

			if (LOG.isInfoEnabled()) {
				LOG.info("Online: " + online + ", wifi: " + wifi
						+ ", IPv6 only: " + ipv6Only);
				if (country.isEmpty()) LOG.info("Country code unknown");
				else LOG.info("Country code: " + country);
			}

			boolean enableNetwork = false, enableConnectionPadding = false;
			List<BridgeType> bridgeTypes = emptyList();

			if (!online) {
				LOG.info("Disabling network, device is offline");
			} else {
				LOG.info("Enabling network");
				enableNetwork = true;
				if (bridgesWork) {
					if (ipv6Only) {
						bridgeTypes = singletonList(MEEK);
					} else {
						bridgeTypes = circumventionProvider
								.getSuitableBridgeTypes(country);
					}
					if (LOG.isInfoEnabled()) {
						LOG.info("Using bridge types " + bridgeTypes);
					}
				} else {
					LOG.info("Not using bridges");
				}
				if (wifi) {
					LOG.info("Enabling connection padding");
					enableConnectionPadding = true;
				} else {
					LOG.info("Disabling connection padding");
				}
			}

			try {
				if (enableNetwork) {
					enableBridges(bridgeTypes);
					enableConnectionPadding(enableConnectionPadding);
					enableIpv6(ipv6Only);
				}
				enableNetwork(enableNetwork);
			} catch (IOException e) {
				logException(LOG, e, "Error enabling network");
			}
		});
	}

	private void enableConnectionPadding(boolean enable) throws IOException {
		if (!state.enableConnectionPadding(enable)) return; // Unchanged
		try {
			controlConnection.setConf("ConnectionPadding", enable ? "1" : "0");
		} catch (TorNotRunningException e) {
			throw new RuntimeException(e);
		}
	}

	private void enableIpv6(boolean enable) throws IOException {
		if (!state.enableIpv6(enable)) return; // Unchanged
		try {
			controlConnection.setConf("ClientUseIPv4", enable ? "0" : "1");
			controlConnection.setConf("ClientUseIPv6", enable ? "1" : "0");
		} catch (TorNotRunningException e) {
			throw new RuntimeException(e);
		}
	}

	@ThreadSafe
	protected static class PluginState {

		private final MutableStateFlow<TorState> state =
				MutableStateFlow(TorState.StartingStopping.INSTANCE);

		@GuardedBy("this")
		private boolean started = false,
				stopped = false,
				networkInitialised = false,
				networkEnabled = false,
				paddingEnabled = false,
				ipv6Enabled = false,
				circuitBuilt = false,
				clockSkewed = false;
		@GuardedBy("this")
		private int bootstrapPercent = 0, numServiceUploads = 0;

		@GuardedBy("this")
		private int orConnectionsConnected = 0;

		@GuardedBy("this")
		private List<BridgeType> bridgeTypes = emptyList();

		synchronized void setStarted() {
			started = true;
			state.setValue(getCurrentState());
		}

		@SuppressWarnings("BooleanMethodIsAlwaysInverted")
		synchronized boolean isTorRunning() {
			return started && !stopped;
		}

		synchronized void setStopped() {
			stopped = true;
			state.setValue(getCurrentState());
		}

		synchronized void setBootstrapPercent(int percent) {
			if (percent < 0 || percent > 100) {
				throw new IllegalArgumentException("percent: " + percent);
			}
			bootstrapPercent = percent;
			if (percent == 100) clockSkewed = false;
			state.setValue(getCurrentState());
		}

		synchronized void setClockSkewed() {
			clockSkewed = true;
			state.setValue(getCurrentState());
		}

		/**
		 * Sets the `circuitBuilt` flag and returns true if the flag has
		 * changed.
		 */
		private synchronized boolean setCircuitBuilt(boolean built) {
			if (built == circuitBuilt) return false; // Unchanged
			circuitBuilt = built;
			if (bootstrapPercent == 100) clockSkewed = false;
			state.setValue(getCurrentState());
			return true; // Changed
		}

		synchronized void onServiceDescriptorUploaded() {
			numServiceUploads++;
			state.setValue(getCurrentState());
		}

		/**
		 * Sets the `networkEnabled` flag and returns true if the flag has
		 * changed.
		 */
		synchronized boolean enableNetwork(boolean enable) {
			boolean wasInitialised = networkInitialised;
			boolean wasEnabled = networkEnabled;
			networkInitialised = true;
			networkEnabled = enable;
			if (!enable) circuitBuilt = false;
			if (!wasInitialised || enable != wasEnabled) {
				state.setValue(getCurrentState());
			}
			return enable != wasEnabled;
		}

		/**
		 * Sets the `paddingEnabled` flag and returns true if the flag has
		 * changed. Doesn't affect getState().
		 */
		private synchronized boolean enableConnectionPadding(boolean enable) {
			if (enable == paddingEnabled) return false; // Unchanged
			paddingEnabled = enable;
			return true; // Changed
		}

		/**
		 * Sets the `ipv6Enabled` flag and returns true if the flag has
		 * changed. Doesn't affect getState().
		 */
		private synchronized boolean enableIpv6(boolean enable) {
			if (enable == ipv6Enabled) return false; // Unchanged
			ipv6Enabled = enable;
			return true; // Changed
		}

		/**
		 * Sets the list of bridge types being used and returns true if the
		 * list has changed. The list is empty if bridges are disabled.
		 * Doesn't affect getState().
		 */
		private synchronized boolean setBridgeTypes(List<BridgeType> types) {
			if (types.equals(bridgeTypes)) return false; // Unchanged
			bridgeTypes = types;
			return true; // Changed
		}

		private synchronized TorState getCurrentState() {
			if (!started || stopped) {
				return TorState.StartingStopping.INSTANCE;
			}
			if (!networkInitialised) {
				return new TorState.Enabling(bootstrapPercent);
			}
			if (!networkEnabled) return TorState.Inactive.INSTANCE;
			if (clockSkewed) return TorState.ClockSkewed.INSTANCE;
			if (bootstrapPercent == 100 && circuitBuilt &&
					orConnectionsConnected > 0) {
				return (numServiceUploads >= HS_DESC_UPLOADS) ?
						TorState.Published.INSTANCE : TorState.Active.INSTANCE;
			} else return new TorState.Enabling(bootstrapPercent);
		}

		private synchronized void onOrConnectionConnected() {
			int oldConnected = orConnectionsConnected;
			orConnectionsConnected++;
			logOrConnections();
			if (oldConnected == 0) state.setValue(getCurrentState());
		}

		private synchronized void onOrConnectionClosed() {
			int oldConnected = orConnectionsConnected;
			orConnectionsConnected--;
			if (orConnectionsConnected < 0) {
				LOG.warn("Count was zero before connection closed");
				orConnectionsConnected = 0;
			}
			logOrConnections();
			if (orConnectionsConnected == 0 && oldConnected != 0) {
				state.setValue(getCurrentState());
			}
		}

		@GuardedBy("this")
		private void logOrConnections() {
			info(LOG, () ->
					orConnectionsConnected + " OR connections connected");
		}

	}
}
