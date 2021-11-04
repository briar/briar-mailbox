package org.briarproject.mailbox.core.system

import android.content.Context
import org.briarproject.android.dontkillmelib.AbstractDozeWatchdogImpl
import org.briarproject.mailbox.core.lifecycle.Service

class DozeWatchdog(appContext: Context) : AbstractDozeWatchdogImpl(appContext), Service
