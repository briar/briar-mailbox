package org.briarproject.mailbox.cli

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.briarproject.mailbox.core.CoreModule
import org.briarproject.mailbox.core.event.DefaultEventExecutorModule
import org.briarproject.mailbox.core.system.DefaultTaskSchedulerModule
import org.briarproject.mailbox.core.tor.JavaTorModule

@Module(
    includes = [
        CoreModule::class,
        DefaultEventExecutorModule::class,
        DefaultTaskSchedulerModule::class,
        JavaTorModule::class,
    ]
)
@InstallIn(SingletonComponent::class)
internal class JavaCliModule
