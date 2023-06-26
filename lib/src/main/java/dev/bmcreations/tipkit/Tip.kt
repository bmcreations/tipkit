package dev.bmcreations.tipkit

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

abstract class Tip(private val engine: EventEngine) {
    private val triggers = mutableListOf<Trigger>()
    private val events: Flow<List<TriggerOccurrenceEvent>>
        get() {
            val flows = triggers.map { it.events }
            return combine(*flows.toTypedArray()) { it.toList().flatten() }
        }

    open fun await(trigger: Trigger) = triggers.add(trigger)
    open fun asset(): @Composable () -> Unit = {}
    open fun title(): @Composable () -> Unit = {}
    open fun message(): @Composable () -> Unit = {}
    open fun image(): @Composable () -> Unit = {}
    open fun actions(): List<TipAction> = emptyList()
    open fun observe(): Flow<List<TriggerOccurrenceEvent>> = events
    suspend fun hasBeenSeen(): Boolean {
        return engine.isComplete(this::class.java.simpleName)
    }
    open suspend fun rules(): List<RuleEvaluation> = listOf { true }
    suspend fun dismiss() {
        engine.complete(this::class.java.simpleName)
    }
    open suspend fun show(): Boolean {
        return rules().all { it() } && !hasBeenSeen()
    }
}

data class TipAction(val id: String, val title: String)