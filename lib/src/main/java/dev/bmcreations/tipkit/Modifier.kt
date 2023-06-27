package dev.bmcreations.tipkit

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

fun Modifier.popoverTip(
    tip: Tip,
    alignment: Alignment = Alignment.BottomCenter,
) = composed {
    val tipScope = LocalTipScope.current

    var size by remember {
        mutableStateOf(IntSize.Zero)
    }

    var position by remember {
        mutableStateOf(Offset.Zero)
    }

    val tipProvider = LocalTipProvider.current
    LaunchedEffect(tip) {
        tip.observe()
            .filterNot { tip.hasBeenSeen() }
            .map { tip.show() }
            .distinctUntilChanged()
            .filter { it }
            .onEach {
                tipProvider.show(
                    TipLocation(
                        tip = tip,
                        content = tipScope.buildTip(tip),
                        anchorPosition = position,
                        anchorSize = size,
                        alignment = alignment
                    )
                )
            }
            .launchIn(this)

        tip.flowContinuation
            .map { tip.show() }
            .distinctUntilChanged()
            .filter { it }
            .onEach {
                tipProvider.show(
                    TipLocation(
                        tip = tip,
                        content = tipScope.buildTip(tip),
                        anchorPosition = position,
                        anchorSize = size,
                        alignment = alignment
                    )
                )
            }
            .launchIn(this)

    }

    return@composed Modifier.onPlaced {
        size = it.size
        position = it.positionInRoot()
    }
}