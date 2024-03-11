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

import android.app.Application;
import android.os.Build;

import org.briarproject.android.dontkillmelib.wakelock.AndroidWakeLockManager;
import org.briarproject.mailbox.core.settings.SettingsManager;
import org.briarproject.onionwrapper.AndroidTorWrapper;
import org.briarproject.onionwrapper.CircumventionProvider;
import org.briarproject.onionwrapper.LocationUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.IntSupplier;

import static android.os.Build.VERSION.SDK_INT;
import static java.util.Arrays.asList;
import static org.briarproject.mailbox.core.tor.TorConstants.CONTROL_PORT;
import static org.briarproject.mailbox.core.tor.TorConstants.SOCKS_PORT;

public class AndroidTorPlugin extends AbstractTorPlugin {

	AndroidTorPlugin(Executor ioExecutor,
			Executor eventExecutor,
			Application app,
			SettingsManager settingsManager,
			NetworkManager networkManager,
			LocationUtils locationUtils,
			CircumventionProvider circumventionProvider,
			AndroidWakeLockManager wakeLockManager,
			String architecture,
			File torDirectory,
			IntSupplier portSupplier) {
		super(ioExecutor, settingsManager, networkManager, locationUtils,
				circumventionProvider, portSupplier,
				new AndroidTorWrapper(app, wakeLockManager, ioExecutor,
						eventExecutor, architecture, torDirectory, SOCKS_PORT,
						CONTROL_PORT));
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
