package dev.bmcreations.tipkit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterStart
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.bmcreations.tipkit.data.InlineTipData
import dev.bmcreations.tipkit.data.PopupData
import dev.bmcreations.tipkit.data.TipPresentation
import dev.bmcreations.tipkit.engines.LocalTipsEngine
import dev.bmcreations.tipkit.engines.TipsEngine
import dev.bmcreations.tipkit.utils.getCoordinatesForPlacement
import kotlinx.coroutines.launch

object TipDefaults {
    val SurfaceColor: Color
        @Composable get() = MaterialTheme.colors.background
    val ContentColor: Color
        @Composable get() = MaterialTheme.colors.onBackground

    @Composable
    private fun TipContainer(content: @Composable () -> Unit) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = SurfaceColor,
            contentColor = ContentColor,
            elevation = 8.dp,
            border = if (isSystemInDarkTheme()) BorderStroke(
                1.dp,
                MaterialTheme.colors.onBackground
            ) else null,
        ) {
            content()
        }

    }

    @Composable
    private fun TipContents(tip: Tip, onDismiss: () -> Unit = { }) {
        val tipProvider = LocalTipProvider.current

        Row(
            verticalAlignment = CenterVertically,
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
                modifier = Modifier.clickable {
                    tipProvider.dismiss()
                    onDismiss()
                },
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
                    Divider(color = MaterialTheme.colors.onBackground.copy(alpha = 0.44f))
                    tip.actions().onEach {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tipProvider.onActionClicked(it) },
                            text = it.title,
                            color = MaterialTheme.colors.primary
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun PopupContent(tip: Tip) {
        TipContainer {
            val screenWidth = LocalConfiguration.current.screenWidthDp.dp
            Column(
                modifier = Modifier
                    .padding(4.dp)
                    .widthIn(max = screenWidth * 0.6f),
            ) {
                TipContents(tip = tip)
            }
        }
    }

    @Composable
    fun InlineContent(tip: Tip, onDismiss: () -> Unit) {
        TipContainer {
            Column(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth(),
            ) {
                TipContents(tip = tip, onDismiss = onDismiss)
            }
        }
    }
}

private class TipScopeImpl : TipScope {
    override fun buildPopupTip(tip: Tip): @Composable () -> Unit {
        return { TipDefaults.PopupContent(tip = tip) }
    }

    override fun buildInlineTip(tip: Tip, onDismiss: () -> Unit): @Composable () -> Unit {
        return { TipDefaults.InlineContent(tip = tip, onDismiss = onDismiss) }
    }
}

@Composable
fun TipScaffold(
    modifier: Modifier = Modifier,
    tipsEngine: TipsEngine,
    tipScope: TipScope = TipScopeImpl(),
    navigator: TipActionNavigation = NoOpTipNavigator(),
    content: @Composable TipScope.() -> Unit
) {
    var emission by remember {
        mutableStateOf<TipPresentation?>(null)
    }

    val composeScope = rememberCoroutineScope()
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier)
    ) {
        val tipProvider = object : TipProvider() {
            override fun show(data: TipPresentation) {
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

        val density = LocalDensity.current

        CompositionLocalProvider(
            LocalTipsEngine provides tipsEngine,
            LocalTipProvider provides tipProvider,
            LocalTipScope provides tipScope
        ) {
            tipScope.content()
            emission?.let { data ->
                when (data) {
                    is PopupData -> {
                        val paddingPx = with(density) { data.padding.toPx() }
                        Box(modifier = Modifier
                            .layout { measurable, constraints ->
                                val tip = measurable.measure(constraints)
                                layout(tip.width, tip.height) {
                                    tipProvider.debugLog(
                                        "anchorPosition (${data.anchorPosition})," +
                                                " anchorSize (${data.anchorSize}), " +
                                                " padding ${paddingPx}px, " +
                                                " tip width ${tip.width}, " +
                                                " tip height ${tip.height}, " +
                                                " maxWidth=${constraints.maxWidth}, " +
                                                " maxHeight=${constraints.maxHeight}"
                                    )

                                    val (x, y) = getCoordinatesForPlacement(
                                        constraints,
                                        data.alignment,
                                        data.anchorPosition,
                                        data.anchorSize,
                                        paddingPx,
                                        tip
                                    ) { tipProvider.debugLog(it) }

                                    tip.place(x, y)
                                }
                            }
                        ) {
                            data.content()
                        }
                    }

                    is InlineTipData -> {
                        // This type is handled inline
                    }
                }
            }
        }
    }
}