/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package dev.polisan.colorchooser.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Indication

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.net.URL

fun Color.toHexCode(): String {
    val red = this.red * 255
    val green = this.green * 255
    val blue = this.blue * 255
    return String.format("%02x%02x%02x", red.toInt(), green.toInt(), blue.toInt())
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sendColor(Color.Red)
        setContent {
            var hue by remember { mutableFloatStateOf(0f) }
            val color = Color.hsv(hue, 1f, 1f)
            val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
            val focusManager = LocalFocusManager.current
            val lifecycleOwner = LocalLifecycleOwner.current

            // Ensure the focus requester requests focus when the component starts
            DisposableEffect(Unit) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_START) {
                        focusRequester.requestFocus()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentSize(Alignment.Center)
                    .onRotaryScrollEvent { event ->
                        val delta = -event.verticalScrollPixels / 50
                        hue = (hue + delta) % 360f
                        if (hue < 0) hue += 360f
                        Thread {
                            sendColor(color)
                        }.start()
                        true
                    }
                    .focusRequester(focusRequester)
                    .focusable()
            ) {
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color)
                        .clickable(interactionSource = interactionSource, indication = null) {
                            Log.d("COLOR", "ON CLICK1")
                            Thread {
                                sendColor(color)
                            }.start()

                        }
                )
            }
        }
    }
}

