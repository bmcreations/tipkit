package dev.bmcreations.tipkit

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface TipInterface

val LocalTipsEngine = staticCompositionLocalOf<TipsEngine?> { null }

@Singleton
class TipsEngine @Inject constructor(
    @ApplicationContext context: Context,
    private val eventsEngine: EventEngine
) {
    val tips = EntryPointAccessors.fromApplication(context, TipInterface::class.java)

    fun invalidateAllTips() {
        eventsEngine.clearCompletions()
        eventsEngine.removeAllOccurrences()
    }
}