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
import java.util.function.IntSupplier;

import javax.annotation.Nullable;

public class JavaTorPlugin extends AbstractTorPlugin {

	JavaTorPlugin(Executor ioExecutor,
			SettingsManager settingsManager,
			NetworkManager networkManager,
			LocationUtils locationUtils,
			Clock clock,
			ResourceProvider resourceProvider,
			CircumventionProvider circumventionProvider,
			@Nullable String architecture,
			File torDirectory,
			IntSupplier portSupplier) {
		super(ioExecutor, settingsManager, networkManager, locationUtils, clock,
				resourceProvider, circumventionProvider, architecture,
				torDirectory, portSupplier);
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
