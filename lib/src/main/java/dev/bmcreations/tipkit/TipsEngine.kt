package dev.bmcreations.tipkit

import androidx.compose.runtime.staticCompositionLocalOf

interface TipInterface

val LocalTipsEngine = staticCompositionLocalOf<TipsEngine?> { null }

class TipsEngine(
    private val eventsEngine: EventEngine
) {
    var tips: TipInterface = object : TipInterface {}
        private set

    val flows: MutableMap<String, List<Tip>> = mutableMapOf()

    fun configure(implementation: TipInterface) {
        tips = implementation
    }

    fun invalidateAllTips() {
        eventsEngine.clearCompletions()
        eventsEngine.removeAllOccurrences()
    }

    fun associateTipWithFlow(tip: Tip, order: Int, flowId: String) {
        val tipsInFlow = flows[flowId].orEmpty()
        if (tipsInFlow.isEmpty() || tipsInFlow.lastIndex < order) {
            flows[flowId] = tipsInFlow + tip
        } else {
            tipsInFlow.toMutableList()[order] = tip
            flows[flowId] = tipsInFlow
        }
    }
}