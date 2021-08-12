package org.briarproject.mailbox.android

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.briarproject.mailbox.core.CoreModule

@Module(
    includes = [
        CoreModule::class,
    ]
)
@InstallIn(SingletonComponent::class)
internal class AppModule
