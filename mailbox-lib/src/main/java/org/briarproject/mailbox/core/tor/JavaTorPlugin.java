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

import org.briarproject.mailbox.core.settings.SettingsManager;
import org.briarproject.mailbox.core.system.LocationUtils;
import org.briarproject.onionwrapper.CircumventionProvider;
import org.briarproject.onionwrapper.UnixTorWrapper;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.function.IntSupplier;

import static org.briarproject.mailbox.core.tor.TorConstants.CONTROL_PORT;
import static org.briarproject.mailbox.core.tor.TorConstants.SOCKS_PORT;

public class JavaTorPlugin extends AbstractTorPlugin {

	JavaTorPlugin(Executor ioExecutor,
			Executor eventExecutor,
			SettingsManager settingsManager,
			NetworkManager networkManager,
			LocationUtils locationUtils,
			CircumventionProvider circumventionProvider,
			String architecture,
			File torDirectory,
			IntSupplier portSupplier) {
		super(ioExecutor, settingsManager, networkManager, locationUtils,
				circumventionProvider, portSupplier, true,
				new UnixTorWrapper(ioExecutor, eventExecutor, architecture,
						torDirectory, SOCKS_PORT, CONTROL_PORT));
	}
}
