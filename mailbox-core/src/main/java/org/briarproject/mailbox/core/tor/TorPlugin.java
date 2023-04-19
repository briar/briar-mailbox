package org.briarproject.mailbox.core.tor;

import org.briarproject.mailbox.core.db.DbException;
import org.briarproject.mailbox.core.lifecycle.Service;
import org.briarproject.mailbox.core.settings.Settings;
import org.briarproject.onionwrapper.CircumventionProvider.BridgeType;

import java.util.List;

import kotlinx.coroutines.flow.StateFlow;

public interface TorPlugin extends Service {

	StateFlow<TorPluginState> getState();

	/**
	 * Call this whenever {@link Settings} in
	 * {@link TorConstants#SETTINGS_NAMESPACE} have changed.
	 */
	void onSettingsChanged();

	/**
	 * This is only available after {@link #startService()} has returned.
	 * Otherwise returns null.
	 */
	String getHiddenServiceAddress() throws DbException;

	/**
	 * Get a list of bridge types that Tor will be using with current settings,
	 * country and {@link NetworkStatus}.
	 */
	List<BridgeType> getCustomBridgeTypes();

}
