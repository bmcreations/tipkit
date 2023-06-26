package dev.bmcreations.tipkit.sample

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.bmcreations.tipkit.EventEngine
import dev.bmcreations.tipkit.TipsEngine
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun providesTipEngine(
        eventEngine: EventEngine
    ) = TipsEngine(eventEngine)

    @Singleton
    @Provides
    fun providesEventEngine(
        @ApplicationContext context: Context
    ) = EventEngine(context)
}