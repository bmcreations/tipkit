package dev.bmcreations.tipkit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
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

object TipDefaults {
    val SurfaceColor: Color
        @Composable get() = MaterialTheme.colorScheme.background
    val ContentColor: Color
        @Composable get() = MaterialTheme.colorScheme.onBackground

    @Composable
    fun Content(tip: Tip) {
        val tipProvider = LocalTipProvider.current

        Surface(
            shape = MaterialTheme.shapes.small,
            color = SurfaceColor,
            contentColor = ContentColor,
            shadowElevation = 8.dp,
            tonalElevation = 0.dp,
        ) {
            val screenWidth = LocalConfiguration.current.screenWidthDp.dp
            Column(
                modifier = Modifier
                    .padding(4.dp)
                    .widthIn(max = screenWidth * 0.6f),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tip.asset()()
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        tip.title()()
                        tip.message()()
                    }
                    Icon(
                        modifier = Modifier.clickable { tipProvider.dismiss() },
                        imageVector = Icons.Default.Close,
                        contentDescription = "dismiss tip"
                    )
                }

                Row {
                    Spacer(Modifier.width(30.dp))
                    Column(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (tip.actions().isNotEmpty()) {
                            Divider(color = DividerDefaults.color.copy(alpha = 0.44f))
                            tip.actions().onEach {
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { tipProvider.onActionClicked(it) },
                                    text = it.title,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class TipScopeImpl : TipScope {
    override fun buildTip(tip: Tip): @Composable () -> Unit {
        return { TipDefaults.Content(tip = tip) }
    }
}

data class TipLocation(
    val tip: Tip? = null,
    val content: @Composable () -> Unit = { },
    val anchorPosition: Offset = Offset.Zero,
    val anchorSize: IntSize = IntSize.Zero,
    val alignment: Alignment = Alignment.TopCenter,
)

abstract class TipProvider {
    abstract fun show(data: TipLocation)
    abstract fun dismiss()
    abstract fun onActionClicked(action: TipAction)

    open val isTipShowing: Boolean = false
}

class NoOpTipProvider : TipProvider() {
    override fun show(data: TipLocation) = Unit
    override fun dismiss() = Unit

    override fun onActionClicked(action: TipAction) = Unit
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
    navigator: TipActionNavigation = NoOpTipNavigator(),
    content: @Composable TipScope.() -> Unit
) {
    var emission by remember {
        mutableStateOf<TipLocation?>(null)
    }

    val composeScope = rememberCoroutineScope()
    BoxWithConstraints(modifier = Modifier
        .fillMaxSize()
        .then(modifier)
    ) {
        val tipProvider = object : TipProvider() {
            override fun show(data: TipLocation) {
                emission = data
            }

            override fun dismiss() {
                composeScope.launch {
                    val tip = emission?.tip
                    emission = null
                    tip?.dismiss()
                }
            }

            override fun onActionClicked(action: TipAction) {
                dismiss()
                navigator.onActionClicked(action)
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
}