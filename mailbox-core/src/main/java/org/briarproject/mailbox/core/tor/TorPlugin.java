package org.briarproject.mailbox.core.tor;

import org.briarproject.mailbox.core.db.DbException;
import org.briarproject.mailbox.core.lifecycle.Service;
import org.briarproject.mailbox.core.settings.Settings;
import org.briarproject.nullsafety.NotNullByDefault;
import org.briarproject.onionwrapper.CircumventionProvider.BridgeType;

import java.util.List;

import javax.annotation.Nullable;

import kotlinx.coroutines.flow.StateFlow;

@NotNullByDefault
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
	@Nullable
	String getHiddenServiceAddress() throws DbException;

	/**
	 * Get a list of bridge types that Tor will be using with current settings,
	 * country and {@link NetworkStatus}.
	 */
	List<BridgeType> getCustomBridgeTypes();

}
