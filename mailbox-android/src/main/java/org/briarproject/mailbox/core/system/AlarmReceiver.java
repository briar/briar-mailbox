package org.briarproject.mailbox.core.system;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.briarproject.mailbox.android.MailboxApplication;

public class AlarmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context ctx, Intent intent) {
		MailboxApplication app =
				(MailboxApplication) ctx.getApplicationContext();
		app.androidEagerSingletons.getAndroidTaskScheduler().onAlarm(intent);
	}
}
