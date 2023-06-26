package dev.bmcreations.tipkit

import androidx.compose.runtime.staticCompositionLocalOf

interface TipInterface

val LocalTipsEngine = staticCompositionLocalOf<TipsEngine?> { null }

class TipsEngine(
    private val eventsEngine: EventEngine
) {
    var tips: TipInterface = object : TipInterface {}
        private set

    fun configure(implementation: TipInterface) {
        tips = implementation
    }

    fun invalidateAllTips() {
        eventsEngine.clearCompletions()
        eventsEngine.removeAllOccurrences()
    }
}