package dev.bmcreations.tipkit

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Singleton
    @Provides
    fun provideDataLoadingScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Singleton
    @Provides
    fun providesEventEngine(
        @ApplicationContext context: Context,
        dataScope: CoroutineScope,
    ) = EventEngine(context, dataScope)
}

class EventEngine @Inject constructor(
    @ApplicationContext context: Context,
    private val dataScope: CoroutineScope,
) {
    private val module = SerializersModule {
        polymorphic(Any::class) {
            subclass(Boolean::class)
            subclass(Int::class)
            subclass(Double::class)
            subclass(Long::class)
            subclass(Short::class)
            subclass(Float::class)
            subclass(String::class)
        }
    }

    private val jsonDecoder by lazy {
        Json {
            ignoreUnknownKeys = true
        }
    }

    internal fun clearCompletions() {
        dataScope.launch {
            completions.edit { it.clear() }
        }
    }

    internal fun clearCompletion(vararg tip: Tip) {
        dataScope.launch {
            completions.edit { prefs ->
                tip.map { longPreferencesKey("${it::class.java.simpleName}-completedAt") }
                    .onEach { key ->
                        prefs.remove(key)
                    }
            }
        }
    }

    internal fun removeAllOccurrences() {
        dataScope.launch {
            occurrenceInstances.edit { it.clear() }
        }
    }

    internal fun <T> removeOccurrencesOf(vararg trigger: Trigger) {
        dataScope.launch {
            occurrenceInstances.edit { prefs ->
                trigger.map { longPreferencesKey(it.id) }.onEach { key ->
                    prefs.remove(key)
                }
            }
        }
    }

    private val completions = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler(
            produceNewData = { emptyPreferences() }
        ),
        migrations = listOf(),
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile = { context.preferencesDataStoreFile("event-engine-completions") }
    )

    private val occurrenceInstances = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler(
            produceNewData = { emptyPreferences() }
        ),
        migrations = listOf(),
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile = { context.preferencesDataStoreFile("event-engine-occurrences") }
    )

    internal fun recordTriggerOccurrence(occurrence: Trigger, value: Any) =
        dataScope.launch(Dispatchers.IO) {
            occurrenceInstances.edit { prefs ->
                val key = stringSetPreferencesKey(occurrence.id)
                val pref = prefs[key].orEmpty()
                val events = pref.toList()
                    .mapNotNull {
                        runCatching {
                            jsonDecoder.decodeFromString<DbTriggerOccurrenceEvent>(it)
                        }.getOrNull()
                    }
                    .sortedBy { it.timestamp }

                val event = when (value) {
                    is Boolean -> BooleanEvent(
                        id = occurrence.id,
                        timestamp = Clock.System.now(),
                        value = value
                    )

                    is Int -> IntEvent(
                        id = occurrence.id,
                        timestamp = Clock.System.now(),
                        value = value
                    )

                    is Long -> LongEvent(
                        id = occurrence.id,
                        timestamp = Clock.System.now(),
                        value = value
                    )

                    is Double -> DoubleEvent(
                        id = occurrence.id,
                        timestamp = Clock.System.now(),
                        value = value
                    )

                    is Float -> FloatEvent(
                        id = occurrence.id,
                        timestamp = Clock.System.now(),
                        value = value
                    )

                    else -> InstantEvent(
                        id = occurrence.id,
                        timestamp = Clock.System.now(),
                    )
                }


                prefs[key] =
                    (events + event).map {
                        jsonDecoder.encodeToString(DbTriggerOccurrenceEvent.serializer(), it)
                    }.toSet()
            }
        }

    internal fun recordTriggerOccurrence(occurrence: Trigger) =
        dataScope.launch(Dispatchers.IO) {
            occurrenceInstances.edit { prefs ->
                val key = stringSetPreferencesKey(occurrence.id)
                val pref = prefs[key].orEmpty()
                val events = pref.toList()
                    .mapNotNull {
                        runCatching {
                            jsonDecoder.decodeFromString<DbTriggerOccurrenceEvent>(it)
                        }.getOrNull()
                    }
                    .sortedBy { it.timestamp }

                val event = InstantEvent(
                    id = occurrence.id,
                    timestamp = Clock.System.now(),
                )


                prefs[key] =
                    (events + event).map {
                        jsonDecoder.encodeToString(DbTriggerOccurrenceEvent.serializer(), it)
                    }.toSet()
            }
        }

    fun occurrences() = occurrenceInstances.data.map {
        it.asMap().values.map { it as Set<String> }
            .map { set -> set.toList() }
            .map { list ->
                list.mapNotNull { entry ->
                    runCatching {
                        jsonDecoder.decodeFromString<DbTriggerOccurrenceEvent>(entry)
                    }.getOrNull()
                }.map { event ->
                    when (event) {
                        is BooleanEvent -> TriggerOccurrenceEvent(
                            id = event.id,
                            timestamp = event.timestamp,
                            value = event.value
                        )

                        is DoubleEvent -> TriggerOccurrenceEvent(
                            id = event.id,
                            timestamp = event.timestamp,
                            value = event.value
                        )

                        is FloatEvent -> TriggerOccurrenceEvent(
                            id = event.id,
                            timestamp = event.timestamp,
                            value = event.value
                        )

                        is IntEvent -> TriggerOccurrenceEvent(
                            id = event.id,
                            timestamp = event.timestamp,
                            value = event.value
                        )

                        is LongEvent -> TriggerOccurrenceEvent(
                            id = event.id,
                            timestamp = event.timestamp,
                            value = event.value
                        )

                        else -> TriggerOccurrenceEvent(
                            id = event.id,
                            timestamp = event.timestamp,
                            value = event.timestamp
                        )
                    }
                }
            }.flatten()
    }

    suspend fun complete(name: String) {
        completions.edit { prefs ->
            prefs[longPreferencesKey("$name-completedAt")] =
                Clock.System.now().toEpochMilliseconds()
        }
    }

    suspend fun isComplete(name: String): Boolean {
        return completions.data.map { prefs ->
            prefs[longPreferencesKey("$name-completedAt")]
        }.firstOrNull() != null
    }
}

typealias RuleEvaluation = () -> Boolean

class Trigger(
    val id: String,
    val engine: EventEngine,
    val events: Flow<List<TriggerOccurrenceEvent>> = engine.occurrences().map { it.filter { e -> e.id == id } }.onEmpty { emit(emptyList()) },
) {
    fun record() {
        engine.recordTriggerOccurrence(this)
    }

    fun record(value: Any) {
        engine.recordTriggerOccurrence(this, value)
    }
}

@Serializable
private sealed class DbTriggerOccurrenceEvent {
    abstract val id: String
    abstract val timestamp: Instant
}

@Serializable
private data class InstantEvent(
    override val id: String,
    override val timestamp: Instant
) : DbTriggerOccurrenceEvent()

@Serializable
private data class BooleanEvent(
    override val id: String,
    override val timestamp: Instant,
    val value: Boolean
) : DbTriggerOccurrenceEvent()

@Serializable
private data class IntEvent(
    override val id: String,
    override val timestamp: Instant,
    val value: Int
) : DbTriggerOccurrenceEvent()

@Serializable
private data class DoubleEvent(
    override val id: String,
    override val timestamp: Instant,
    val value: Double
) : DbTriggerOccurrenceEvent()

@Serializable
private data class LongEvent(
    override val id: String,
    override val timestamp: Instant,
    val value: Long
) : DbTriggerOccurrenceEvent()

@Serializable
private data class FloatEvent(
    override val id: String,
    override val timestamp: Instant,
    val value: Float
) : DbTriggerOccurrenceEvent()

@Serializable
data class TriggerOccurrenceEvent(
    val id: String,
    val timestamp: Instant,
    @Polymorphic
    val value: Any
)
