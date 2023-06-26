package dev.bmcreations.tipkit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

interface TipScope {
    fun buildTip(
        tip: Tip,
    ): @Composable () -> Unit
}

class NoOpTipScopeImpl : TipScope {

    override fun buildTip(tip: Tip): @Composable () -> Unit {
        return {}
    }
}

class TipScopeImpl : TipScope {
    override fun buildTip(tip: Tip): @Composable () -> Unit {
        return {
            Surface(
                shape = MaterialTheme.shapes.small,
                shadowElevation = 8.dp,
                tonalElevation = 4.dp,
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tip.asset()()
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        tip.title()()
                        tip.message()()
                    }
                }
            }
        }
    }
}

data class TipLocation(
    val tip: Tip = object : Tip() {},
    val content: @Composable () -> Unit = { },
    val anchorPosition: Offset = Offset.Zero,
    val anchorSize: IntSize = IntSize.Zero,
    val alignment: Alignment = Alignment.TopCenter,
)

abstract class TipProvider {
    abstract fun show(data: TipLocation)
    open val isTipShowing: Boolean = false
}

class NoOpTipProvider : TipProvider() {
    override fun show(data: TipLocation) = Unit
}

val LocalTipProvider =
    staticCompositionLocalOf<TipProvider> { NoOpTipProvider() }
val LocalTipScope =
    staticCompositionLocalOf<TipScope> { NoOpTipScopeImpl() }

@Composable
fun TipScaffold(
    modifier: Modifier = Modifier,
    tipsEngine: TipsEngine,
    tipScope: TipScope = TipScopeImpl(),
    content: @Composable TipScope.() -> Unit
) {
    var emission by remember {
        mutableStateOf<TipLocation?>(null)
    }

    val composeScope = rememberCoroutineScope()

    BoxWithConstraints(modifier = Modifier
        .fillMaxSize()
        .then(modifier)
        .noRippleClickable(emission != null) {
            composeScope.launch {
                emission?.tip?.dismiss()
                emission = null
            }

        }
    ) {
        val tipProvider = object : TipProvider() {
            override fun show(data: TipLocation) {
                emission = data
            }

            override val isTipShowing: Boolean
                get() = emission != null
        }

        CompositionLocalProvider(
            LocalTipsEngine provides tipsEngine,
            LocalTipProvider provides tipProvider,
            LocalTipScope provides tipScope
        ) {
            tipScope.content()
        }

        emission?.let { tip ->
            Box(modifier = Modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        placeable.place(
                            tip.anchorPosition.x.roundToInt() + tip.anchorSize.width / 2 - placeable.width / 2,
                            tip.anchorPosition.y.roundToInt() + tip.anchorSize.height
                        )
                    }
                }
            ) {
                tip.content()
            }
        }
    }
}


private fun Modifier.noRippleClickable(
    enabled: Boolean = true,
    role: Role = Role.Button,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit,
) = composed {

    val interaction = interactionSource ?: remember { MutableInteractionSource() }

    clickable(
        onClick = onClick,
        enabled = enabled,
        role = role,
        interactionSource = interaction,
        indication = null,
    )
}