/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package dev.polisan.colorchooser.presentation

import android.graphics.Color.RGBToHSV
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.wear.compose.material.CircularProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.experimental.and
import kotlin.math.max
import kotlin.math.min

fun Color.toHexCode(): String {
    val red = this.red * 255
    val green = this.green * 255
    val blue = this.blue * 255
    return String.format("%02x%02x%02x", red.toInt(), green.toInt(), blue.toInt())
}

fun getHue(red: Int, green: Int, blue: Int): Int {
    val min =
        min(min(red.toDouble(), green.toDouble()), blue.toDouble()).toFloat()
    val max =
        max(max(red.toDouble(), green.toDouble()), blue.toDouble()).toFloat()

    if (min == max) {
        return 0
    }

    var hue = 0f
    hue = if (max == red.toFloat()) {
        (green - blue) / (max - min)
    } else if (max == green.toFloat()) {
        2f + (blue - red) / (max - min)
    } else {
        4f + (red - green) / (max - min)
    }

    hue = hue * 60
    if (hue < 0) hue = hue + 360

    return Math.round(hue)
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var hue by remember { mutableFloatStateOf(0f) }
            var color = Color.hsv(hue, 1f, 1f)
            var isLoading by remember { mutableStateOf(true) }
            val coroutineScope = rememberCoroutineScope()
            val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
            val lifecycleOwner = LocalLifecycleOwner.current

            LaunchedEffect(Unit) {
                coroutineScope.launch {
                    Log.d("COLOR", "Asking for color")
                    val colors = withContext(Dispatchers.IO) {
                        receiveColor()
                    }
                    Log.d("COLOR", "Received colors: $colors")

                    if (colors != null) {
                        val red = (colors.getOrNull(8) ?: 0x00).toInt() and 0xFF
                        val green = (colors.getOrNull(9) ?: 0x00).toInt() and 0xFF
                        val blue = (colors.getOrNull(10) ?: 0x00).toInt() and 0xFF
                        Log.d("COLOR", "Received and parsed colors: $red $green $blue")
                        color = Color(red / 255f, green / 255f, blue / 255f)
                        val hsv = FloatArray(3)
                        RGBToHSV(red, green, blue, hsv)
                        hue = hsv[0]
                        isLoading = false
                    }
                }
            }

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
                            Log.d("COLOR", "ON CLICK")
                            coroutineScope.launch {
                                sendColor(color)
                            }
                        }
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(50.dp)
                        )
                    }
                }
            }
        }
    }
}
