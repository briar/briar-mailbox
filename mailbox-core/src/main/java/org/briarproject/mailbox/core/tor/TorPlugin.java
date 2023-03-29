package org.briarproject.mailbox.core.tor;

import org.briarproject.mailbox.core.db.DbException;
import org.briarproject.mailbox.core.lifecycle.Service;

import kotlinx.coroutines.flow.StateFlow;

public interface TorPlugin extends Service {

	StateFlow<TorPluginState> getState();

	String getHiddenServiceAddress() throws DbException;

}
