package dev.bmcreations.tipkit.sample

import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ColorFilter
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.bmcreations.tipkit.engines.EligibilityCriteria
import dev.bmcreations.tipkit.engines.EventEngine
import dev.bmcreations.tipkit.Tip
import dev.bmcreations.tipkit.TipAction
import dev.bmcreations.tipkit.engines.TipInterface
import dev.bmcreations.tipkit.engines.TipsEngine
import dev.bmcreations.tipkit.engines.Trigger
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SampleTips : TipInterface {
    val anchor: AnchorTip
    val anchor2: Anchor2Tip
    val anchor3: Anchor3Tip
}

@Singleton
class AnchorTip @Inject constructor(
    eventEngine: EventEngine,
    tipEngine: TipsEngine
) : Tip(eventEngine, tipEngine) {

    init {
        flowPosition = 0
        flowId = "onboarding-flow"
    }

    val clicks = Trigger(
        id = "clicks",
        engine = eventEngine
    ).also { await(it) }

    val toggle = Trigger(
        id = "toggle",
        engine = eventEngine
    ).also { await(it) }

    override fun title(): @Composable () -> Unit {
        return {
            Text(
                text = "Remember",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }

    override fun message(): @Composable () -> Unit {
        return {
            Text(
                text = "With great power, comes great responsibility",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    override fun asset(): @Composable () -> Unit {
        return {
            Image(
                imageVector = Icons.Rounded.Info,
                colorFilter = ColorFilter.tint(LocalContentColor.current),
                contentDescription = null
            )
        }
    }

    override fun actions(): List<TipAction> {
        return listOf(
            TipAction(name,"learn-more", "Learn More")
        )
    }

    override suspend fun criteria(): List<EligibilityCriteria> {
        val clicks = clicks.events.firstOrNull().orEmpty()
        val toggledOn = toggle.events.firstOrNull()?.lastOrNull()?.value as? Boolean ?: false

        return listOf(
            { clicks.count() >= 5 },
            { toggledOn }
        )
    }
}

@Singleton
class Anchor2Tip @Inject constructor(
    eventEngine: EventEngine,
    tipEngine: TipsEngine
) : Tip(eventEngine, tipEngine) {

    init {
        flowPosition = 1
        flowId = "onboarding-flow"
    }

    override fun title(): @Composable () -> Unit {
        return {
            Text(
                text = "Flows",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }

    override fun message(): @Composable () -> Unit {
        return {
            Text(
                text = "Chain tips together in a flow",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    override fun asset(): @Composable () -> Unit {
        return {
            Image(
                imageVector = Icons.Rounded.ArrowForward,
                colorFilter = ColorFilter.tint(LocalContentColor.current),
                contentDescription = null
            )
        }
    }

    override suspend fun criteria(): List<EligibilityCriteria> {
        val priorTipSeen = flow.find { it.name == "anchortip" }?.hasBeenSeen() ?: false
        return listOf( { priorTipSeen } )
    }
}

@Singleton
class Anchor3Tip @Inject constructor(
    eventEngine: EventEngine,
    tipEngine: TipsEngine
) : Tip(eventEngine, tipEngine) {

    init {
        flowPosition = 2
        flowId = "onboarding-flow"
    }

    override fun title(): @Composable () -> Unit {
        return {
            Text(
                text = "Inline",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }

    override fun message(): @Composable () -> Unit {
        return {
            Text(
                text = "We can even render them inline on screen content.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    override fun asset(): @Composable () -> Unit {
        return {
            Image(
                imageVector = Icons.Rounded.Favorite,
                colorFilter = ColorFilter.tint(LocalContentColor.current),
                contentDescription = null
            )
        }
    }

    override suspend fun criteria(): List<EligibilityCriteria> {
        val priorTipSeen = flow.find { it.name == "anchor2tip" }?.hasBeenSeen() ?: false
        return listOf( { priorTipSeen } )
    }
}
