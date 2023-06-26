package dev.bmcreations.tipkit.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import dev.bmcreations.tipkit.LocalTipsEngine
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
                    .popoverTip(tips.anchor),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "Click count:$clicks")
            Switch(
                checked = checked,
                onCheckedChange = onCheckChanged
            )
            Button(
                modifier = Modifier.padding(bottom = 64.dp),
                onClick = onClick
            ) {
                Text(text = "Click me")
            }
        }
    }
}