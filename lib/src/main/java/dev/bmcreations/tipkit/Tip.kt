package dev.bmcreations.tipkit

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge

abstract class Tip {
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
    open suspend fun hasBeenSeen(): Boolean = false
    open suspend fun rules(): List<RuleEvaluation> = listOf { true }
    open suspend fun dismiss() = Unit
    open suspend fun show(): Boolean {
        return rules().all { it() } && !hasBeenSeen()
    }
}

data class TipAction(val id: String, val title: String)