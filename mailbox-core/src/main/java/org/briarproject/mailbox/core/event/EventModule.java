package org.briarproject.mailbox.core.event;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class EventModule {

	@Provides
	@Singleton
	EventBus provideEventBus(EventBusImpl eventBus) {
		return eventBus;
	}
}
