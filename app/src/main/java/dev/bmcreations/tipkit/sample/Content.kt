package dev.bmcreations.tipkit.sample

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import dev.bmcreations.tipkit.InlineTip
import dev.bmcreations.tipkit.engines.LocalTipsEngine
import dev.bmcreations.tipkit.popoverTip

@Composable
fun Content() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val tips = LocalTipsEngine.current!!.tips as SampleTips
            var checked by remember {
                mutableStateOf(false)
            }

            val onCheckChanged = { didCheck: Boolean ->
                checked = didCheck
                tips.anchor.toggle.record(didCheck)
            }

            var clicks by remember {
                mutableStateOf(0)
            }

            val onClick = {
                clicks++
                tips.anchor.clicks.record()
            }

            Anchor(
                modifier = Modifier
                    .padding(top = 32.dp)
                    .border(1.dp, Color.Red)
                    .popoverTip(tips.anchor),
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .weight(1f),
                tonalElevation = 8.dp,
                shape = MaterialTheme.shapes.extraLarge
            ) {
                LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)) {
                    item { InlineTip(tip = tips.anchor3) }
                    items(100) { num ->
                        Text(text = "item $num")
                    }
                }
            }
            Text(text = "Click count:$clicks")
            Switch(
                checked = checked,
                onCheckedChange = onCheckChanged
            )
            Button(
                modifier = Modifier
                    .padding(bottom = 64.dp)
                    .popoverTip(tips.anchor2, alignment = Alignment.CenterEnd),
                onClick = onClick
            ) {
                Text(text = "Click me")
            }
        }
    }
}