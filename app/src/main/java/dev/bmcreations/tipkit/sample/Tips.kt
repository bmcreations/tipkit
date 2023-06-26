package dev.bmcreations.tipkit.sample

import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.bmcreations.tipkit.EligibilityCriteria
import dev.bmcreations.tipkit.EventEngine
import dev.bmcreations.tipkit.Tip
import dev.bmcreations.tipkit.TipAction
import dev.bmcreations.tipkit.TipActionNavigation
import dev.bmcreations.tipkit.TipInterface
import dev.bmcreations.tipkit.Trigger
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SampleTips : TipInterface {
    val anchor: AnchorTip
}

class AnchorTip @Inject constructor(
    eventEngine: EventEngine,
) : Tip(eventEngine) {
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
                imageVector = Icons.Rounded.FavoriteBorder,
                contentDescription = null
            )
        }
    }

    override fun actions(): List<TipAction> {
        return listOf(
            TipAction("learn-more", "Learn More")
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
