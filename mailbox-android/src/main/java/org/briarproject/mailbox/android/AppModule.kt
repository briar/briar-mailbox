package org.briarproject.mailbox.android

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.briarproject.mailbox.core.CoreModule

@Module(
    includes = [
        CoreModule::class,
        // Hilt modules from this gradle module are included automatically somehow
    ]
)
@InstallIn(SingletonComponent::class)
internal class AppModule
