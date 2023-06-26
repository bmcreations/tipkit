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

    /**
     * Registers the [trigger] for watching events
     */
    open fun await(trigger: Trigger) = triggers.add(trigger)

    // region UI configuration
    open fun asset(): @Composable () -> Unit = {}
    open fun title(): @Composable () -> Unit = {}
    open fun message(): @Composable () -> Unit = {}
    open fun image(): @Composable () -> Unit = {}
    open fun actions(): List<TipAction> = emptyList()
    // endregion

    /**
     * event stream for [triggers]
     */
    open fun observe(): Flow<List<TriggerOccurrenceEvent>> = events

    /**
     * Eligibility criteria for whether this tip should show
     */
    open suspend fun criteria(): List<EligibilityCriteria> = listOf { true }

    /**
     * Triggers a check of all [criteria] and if so, the Modifier will pass the [TipLocation] to the [TipProvider]
     * for display.
     */
    suspend fun show(): Boolean {
        return criteria().all { it() } && !hasBeenSeen()
    }

    /**
     * Whether or not this tip has been displayed
     */
    suspend fun hasBeenSeen(): Boolean {
        return engine.isComplete(this::class.java.simpleName)
    }

    /**
     * Marks the tip completed
     */
    suspend fun dismiss() {
        engine.complete(this::class.java.simpleName)
    }
}

data class TipAction(val id: String, val title: String)