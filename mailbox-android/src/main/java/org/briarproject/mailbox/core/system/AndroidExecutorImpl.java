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

package org.briarproject.mailbox.core.system;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

class AndroidExecutorImpl implements AndroidExecutor {

	private final Handler uiHandler;
	private final Runnable loop;
	private final AtomicBoolean started = new AtomicBoolean(false);
	private final CountDownLatch startLatch = new CountDownLatch(1);

	private volatile Handler backgroundHandler = null;

	@Inject
	AndroidExecutorImpl(Application app) {
		uiHandler = new Handler(app.getApplicationContext().getMainLooper());
		loop = () -> {
			Looper.prepare();
			backgroundHandler = new Handler();
			startLatch.countDown();
			Looper.loop();
		};
	}

	private void startIfNecessary() {
		if (!started.getAndSet(true)) {
			Thread t = new Thread(loop, "AndroidExecutor");
			t.setDaemon(true);
			t.start();
		}
		try {
			startLatch.await();
		} catch (InterruptedException e) {
			throw new RejectedExecutionException(e);
		}
	}

	@Override
	public void runOnBackgroundThread(Runnable r) {
		startIfNecessary();
		backgroundHandler.post(r);
	}

	@Override
	public void runOnUiThread(Runnable r) {
		uiHandler.post(r);
	}
}
