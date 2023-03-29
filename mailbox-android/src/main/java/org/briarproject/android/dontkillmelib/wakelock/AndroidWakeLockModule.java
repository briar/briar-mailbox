package org.briarproject.android.dontkillmelib.wakelock;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class AndroidWakeLockModule {

	@Provides
	@Singleton
	AndroidWakeLockManager provideWakeLockManager(
			AndroidWakeLockManagerImpl wakeLockManager) {
		return wakeLockManager;
	}
}
