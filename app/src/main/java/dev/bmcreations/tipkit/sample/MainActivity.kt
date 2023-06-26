package dev.bmcreations.tipkit.sample

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dev.bmcreations.tipkit.TipAction
import dev.bmcreations.tipkit.TipActionNavigation
import dev.bmcreations.tipkit.TipInterface
import dev.bmcreations.tipkit.TipScaffold
import dev.bmcreations.tipkit.TipsEngine
import dev.bmcreations.tipkit.sample.ui.theme.AndroidTipKitTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tips: TipsEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tips.configure(EntryPointAccessors.fromApplication(this, TipInterface::class.java))
        tips.invalidateAllTips()

        val tipNavigation = object : TipActionNavigation {
            override fun onActionClicked(action: TipAction) {
                Toast.makeText(this@MainActivity, "clicked ${action.id}", Toast.LENGTH_SHORT).show()
            }

        }
        setContent {
            AndroidTipKitTheme {
                TipScaffold(
                    tipsEngine = tips,
                    navigator = tipNavigation,
                ) {
                    Content()
                }
            }
        }
    }
}

@Composable
fun Anchor(modifier: Modifier = Modifier) {
    Text(
        text = "I am the anchor",
        modifier = modifier.border(1.dp, Color.Black)
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AndroidTipKitTheme {
        Anchor()
    }
}