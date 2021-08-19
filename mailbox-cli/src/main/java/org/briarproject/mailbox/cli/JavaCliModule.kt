package org.briarproject.mailbox.cli

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.briarproject.mailbox.core.CoreModule
import org.briarproject.mailbox.core.tor.JavaTorModule

@Module(
    includes = [
        CoreModule::class,
        JavaTorModule::class,
    ]
)
@InstallIn(SingletonComponent::class)
internal class JavaCliModule
