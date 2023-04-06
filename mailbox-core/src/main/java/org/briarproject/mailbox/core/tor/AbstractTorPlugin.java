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

import org.briarproject.mailbox.core.PoliteExecutor;
import org.briarproject.mailbox.core.db.DbException;
import org.briarproject.mailbox.core.event.Event;
import org.briarproject.mailbox.core.event.EventListener;
import org.briarproject.mailbox.core.lifecycle.IoExecutor;
import org.briarproject.mailbox.core.lifecycle.ServiceException;
import org.briarproject.mailbox.core.settings.Settings;
import org.briarproject.mailbox.core.settings.SettingsManager;
import org.briarproject.mailbox.core.system.LocationUtils;
import org.briarproject.onionwrapper.CircumventionProvider;
import org.briarproject.onionwrapper.CircumventionProvider.BridgeType;
import org.briarproject.onionwrapper.TorWrapper;
import org.briarproject.onionwrapper.TorWrapper.HiddenServiceProperties;
import org.briarproject.onionwrapper.TorWrapper.Observer;
import org.briarproject.onionwrapper.TorWrapper.TorState;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntSupplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static kotlinx.coroutines.flow.StateFlowKt.MutableStateFlow;
import static org.briarproject.mailbox.core.tor.TorConstants.BRIDGE_AUTO;
import static org.briarproject.mailbox.core.tor.TorConstants.BRIDGE_USE;
import static org.briarproject.mailbox.core.tor.TorConstants.BRIDGE_USE_MEEK;
import static org.briarproject.mailbox.core.tor.TorConstants.BRIDGE_USE_OBFS4;
import static org.briarproject.mailbox.core.tor.TorConstants.BRIDGE_USE_OBFS4_DEFAULT;
import static org.briarproject.mailbox.core.tor.TorConstants.BRIDGE_USE_SNOWFLAKE;
import static org.briarproject.mailbox.core.tor.TorConstants.BRIDGE_USE_VANILLA;
import static org.briarproject.mailbox.core.tor.TorConstants.HS_ADDRESS_V3;
import static org.briarproject.mailbox.core.tor.TorConstants.HS_PRIVATE_KEY_V3;
import static org.briarproject.mailbox.core.tor.TorConstants.SETTINGS_NAMESPACE;
import static org.briarproject.mailbox.core.util.LogUtils.info;
import static org.briarproject.mailbox.core.util.LogUtils.logException;
import static org.briarproject.mailbox.core.util.PrivacyUtils.scrubOnion;
import static org.briarproject.onionwrapper.CircumventionProvider.BridgeType.DEFAULT_OBFS4;
import static org.briarproject.onionwrapper.CircumventionProvider.BridgeType.MEEK;
import static org.briarproject.onionwrapper.CircumventionProvider.BridgeType.NON_DEFAULT_OBFS4;
import static org.briarproject.onionwrapper.CircumventionProvider.BridgeType.SNOWFLAKE;
import static org.briarproject.onionwrapper.CircumventionProvider.BridgeType.VANILLA;
import static org.slf4j.LoggerFactory.getLogger;

public abstract class AbstractTorPlugin implements TorPlugin, EventListener {

	private static final Logger LOG = getLogger(AbstractTorPlugin.class);

	/**
	 * The number of uploads of our onion service descriptor we wait for
	 * before we consider our onion service to be published.
	 * In reality, the actual reachability is more complicated,
	 * but this might be a reasonable heuristic.
	 */
	private static final int HS_DESC_UPLOADS = 1;

	private final Executor ioExecutor;
	private final Executor connectionStatusExecutor;
	private final SettingsManager settingsManager;
	private final NetworkManager networkManager;
	private final LocationUtils locationUtils;
	private final CircumventionProvider circumventionProvider;
	private final IntSupplier portSupplier;
	private final boolean canVerifyLetsEncryptCerts;
	private final TorWrapper tor;
	private final AtomicBoolean used = new AtomicBoolean(false);

	protected final PluginState state = new PluginState();

	private volatile Settings settings = null;

	AbstractTorPlugin(Executor ioExecutor,
			SettingsManager settingsManager,
			NetworkManager networkManager,
			LocationUtils locationUtils,
			CircumventionProvider circumventionProvider,
			IntSupplier portSupplier,
			boolean canVerifyLetsEncryptCerts,
			TorWrapper tor) {
		this.ioExecutor = ioExecutor;
		this.settingsManager = settingsManager;
		this.networkManager = networkManager;
		this.locationUtils = locationUtils;
		this.circumventionProvider = circumventionProvider;
		this.portSupplier = portSupplier;
		this.canVerifyLetsEncryptCerts = canVerifyLetsEncryptCerts;
		this.tor = tor;
		// Don't execute more than one connection status check at a time
		connectionStatusExecutor =
				new PoliteExecutor("TorPlugin", ioExecutor, 1);
		tor.setObserver(new Observer() {

			@Override
			public void onState(@Nonnull TorState s) {
				state.onStateChanged(s);
			}

			@Override
			public void onBootstrapPercentage(int percentage) {
				state.setBootstrapPercent(percentage);
			}

			@Override
			public void onHsDescriptorUpload(@Nonnull String onion) {
				state.onServiceDescriptorUploaded();
			}

			@Override
			public void onClockSkewDetected(long skewSeconds) {
				state.onClockSkewDetected();
			}
		});
	}

	public StateFlow<TorPluginState> getState() {
		return state.state;
	}

	@Override
	public void startService() throws ServiceException {
		if (used.getAndSet(true)) throw new IllegalStateException();
		// Load the settings
		try {
			settings = settingsManager.getSettings(SETTINGS_NAMESPACE);
		} catch (DbException e) {
			logException(LOG, e, "Error while retrieving settings");
			settings = new Settings();
		}
		// Start Tor
		try {
			tor.start();
		} catch (InterruptedException e) {
			LOG.warn("Interrupted while starting Tor");
			Thread.currentThread().interrupt();
			throw new ServiceException(e);
		} catch (IOException e) {
			throw new ServiceException(e);
		}
		// Check whether we're online
		updateConnectionStatus(networkManager.getNetworkStatus());
		// Create a hidden service if necessary
		ioExecutor.execute(() -> {
			int port;
			try {
				port = portSupplier.getAsInt();
			} catch (Exception e) {
				throw new AssertionError(e);
			}
			info(LOG, () -> "Binding hidden service to port: " + port);
			publishHiddenService(port);
		});
	}

	@IoExecutor
	private void publishHiddenService(int port) {
		if (!tor.isTorRunning()) return;

		String privateKey3 = settings.get(HS_PRIVATE_KEY_V3);
		createV3HiddenService(port, privateKey3);
	}

	@IoExecutor
	private void createV3HiddenService(int port, @Nullable String privKey) {
		LOG.info("Creating v3 hidden service");
		HiddenServiceProperties hsProps;
		try {
			hsProps = tor.publishHiddenService(port, 80, privKey);
		} catch (IOException e) {
			logException(LOG, e, "Error while add onion service");
			return;
		}
		info(LOG, () -> "V3 hidden service " + scrubOnion(hsProps.onion));

		if (privKey == null) {
			Settings s = new Settings();
			s.put(HS_ADDRESS_V3, hsProps.onion);
			s.put(HS_PRIVATE_KEY_V3, hsProps.privKey);
			try {
				settingsManager.mergeSettings(s, SETTINGS_NAMESPACE);
				// update cached settings with merge result
				settings = settingsManager.getSettings(SETTINGS_NAMESPACE);
			} catch (DbException e) {
				logException(LOG, e, "Error while merging settings");
			}
		}
	}

	@Nullable
	public String getHiddenServiceAddress() {
		return settings == null ? null : settings.get(HS_ADDRESS_V3);
	}

	private void enableBridges(List<BridgeType> bridgeTypes, String countryCode)
			throws IOException {
		if (bridgeTypes.isEmpty()) {
			tor.disableBridges();
		} else {
			List<String> bridges = new ArrayList<>();
			for (BridgeType bridgeType : bridgeTypes) {
				bridges.addAll(circumventionProvider.getBridges(bridgeType,
						countryCode, canVerifyLetsEncryptCerts));
			}
			tor.enableBridges(bridges);
		}
	}

	@Override
	public void stopService() {
		try {
			tor.stop();
		} catch (IOException e) {
			logException(LOG, e,
					"Error while sending tor shutdown instructions");
		}
	}

	@Override
	public void eventOccurred(Event e) {
		if (e instanceof NetworkStatusEvent) {
			updateConnectionStatus(((NetworkStatusEvent) e).getStatus());
		}
	}

	@Override
	public void onSettingsChanged() {
		ioExecutor.execute(() -> {
			try {
				settings = settingsManager.getSettings(SETTINGS_NAMESPACE);
			} catch (DbException e) {
				logException(LOG, e, "Error while retrieving settings");
				settings = new Settings();
			}
			// TODO reset actual plugin state, if this causes us to lose conn
			updateConnectionStatus(networkManager.getNetworkStatus());
		});
	}

	private void updateConnectionStatus(NetworkStatus status) {
		connectionStatusExecutor.execute(() -> {
			if (!tor.isTorRunning()) return;
			boolean online = status.isConnected();
			boolean wifi = status.isWifi();
			boolean ipv6Only = status.isIpv6Only();
			String country = locationUtils.getCurrentCountry();

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
				bridgeTypes = getBridgeTypes(country, ipv6Only);
				if (wifi) {
					LOG.info("Enabling connection padding");
					enableConnectionPadding = true;
				} else {
					LOG.info("Disabling connection padding");
				}
			}

			try {
				if (enableNetwork) {
					enableBridges(bridgeTypes, country);
					tor.enableConnectionPadding(enableConnectionPadding);
					tor.enableIpv6(ipv6Only);
				}
				tor.enableNetwork(enableNetwork);
			} catch (IOException e) {
				logException(LOG, e, "Error enabling network");
			}
		});
	}

	private List<BridgeType> getBridgeTypes(String country, boolean ipv6Only) {
		List<BridgeType> bridgeTypes = emptyList();
		boolean bridgesNeeded =
				circumventionProvider.doBridgesWork(country);
		boolean bridgeAuto = settings.getBoolean(BRIDGE_AUTO, true);
		if (bridgeAuto) {
			if (bridgesNeeded) {
				if (ipv6Only) {
					bridgeTypes = asList(MEEK, SNOWFLAKE);
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
		} else {
			boolean useBridges = settings.getBoolean(BRIDGE_USE, bridgesNeeded);
			if (useBridges) {
				List<BridgeType> defaultTypes =
						circumventionProvider.getSuitableBridgeTypes(country);
				ArrayList<BridgeType> types = new ArrayList<>();
				if (settings.getBoolean(BRIDGE_USE_SNOWFLAKE,
						defaultTypes.contains(SNOWFLAKE))) {
					types.add(SNOWFLAKE);
				}
				if (settings.getBoolean(BRIDGE_USE_MEEK,
						defaultTypes.contains(MEEK))) {
					types.add(MEEK);
				}
				if (settings.getBoolean(BRIDGE_USE_OBFS4,
						defaultTypes.contains(NON_DEFAULT_OBFS4))) {
					types.add(NON_DEFAULT_OBFS4);
				}
				if (settings.getBoolean(BRIDGE_USE_OBFS4_DEFAULT,
						defaultTypes.contains(DEFAULT_OBFS4))) {
					types.add(DEFAULT_OBFS4);
				}
				if (settings.getBoolean(BRIDGE_USE_VANILLA,
						defaultTypes.contains(VANILLA))) {
					types.add(VANILLA);
				}
				bridgeTypes = types;
				if (LOG.isInfoEnabled()) {
					LOG.info("Using bridge types " + bridgeTypes);
				}
			} else {
				LOG.info("Not using bridges");
			}
		}
		return bridgeTypes;
	}

	@ThreadSafe
	private class PluginState {

		private final MutableStateFlow<TorPluginState> state =
				MutableStateFlow(TorPluginState.StartingStopping.INSTANCE);

		@GuardedBy("this")
		private boolean clockSkewed = false;

		@GuardedBy("this")
		private int bootstrapPercent = 0, numServiceUploads = 0;

		synchronized void setBootstrapPercent(int percent) {
			if (percent < 0 || percent > 100) {
				throw new IllegalArgumentException("percent: " + percent);
			}
			bootstrapPercent = percent;
			if (percent == 100) clockSkewed = false;
			state.setValue(getCurrentState());
		}

		synchronized void onClockSkewDetected() {
			clockSkewed = true;
			state.setValue(getCurrentState());
		}

		synchronized void onServiceDescriptorUploaded() {
			numServiceUploads++;
			state.setValue(getCurrentState());
		}

		synchronized void onStateChanged(TorState torState) {
			state.setValue(getCurrentState(torState));
		}

		private synchronized TorPluginState getCurrentState() {
			return getCurrentState(tor.getTorState());
		}

		private synchronized TorPluginState getCurrentState(TorState torState) {
			if (torState == TorState.STARTING_STOPPING) {
				return TorPluginState.StartingStopping.INSTANCE;
			} else if (torState == TorState.DISABLED) {
				return TorPluginState.Inactive.INSTANCE;
			} else if (clockSkewed) {
				return TorPluginState.ClockSkewed.INSTANCE;
			} else if (torState == TorState.CONNECTING) {
				return new TorPluginState.Enabling(bootstrapPercent);
			} else if (torState == TorState.CONNECTED) {
				if (numServiceUploads >= HS_DESC_UPLOADS) {
					return TorPluginState.Published.INSTANCE;
				} else {
					return TorPluginState.Active.INSTANCE;
				}
			} else {
				throw new AssertionError();
			}
		}
	}
}
