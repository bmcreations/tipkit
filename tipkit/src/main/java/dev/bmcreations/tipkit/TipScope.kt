package dev.bmcreations.tipkit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalTipScope =
    staticCompositionLocalOf<TipScope> { NoOpTipScopeImpl() }

interface TipScope {
    fun buildPopupTip(
        tip: Tip,
    ): @Composable () -> Unit

    fun buildInlineTip(
        tip: Tip,
        onDismiss: () -> Unit
    ): @Composable () -> Unit
}

class NoOpTipScopeImpl : TipScope {

    override fun buildInlineTip(tip: Tip, onDismiss: () -> Unit): @Composable () -> Unit {
        return { }
    }

    override fun buildPopupTip(tip: Tip): @Composable () -> Unit {
        return { }
    }
}
