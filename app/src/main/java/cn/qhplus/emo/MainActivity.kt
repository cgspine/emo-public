/*
 * Copyright 2022 emo Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.qhplus.emo

import android.os.Bundle
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import cn.qhplus.emo.network.NetworkBandwidth
import cn.qhplus.emo.network.NetworkBandwidthSampler
import cn.qhplus.emo.network.NetworkConnectivity
import cn.qhplus.emo.network.NetworkState
import cn.qhplus.emo.network.NetworkStreamTotal
import cn.qhplus.emo.scheme.SchemeClient
import cn.qhplus.emo.scheme.impl.ComposeHostActivity
import cn.qhplus.emo.theme.EmoTheme
import cn.qhplus.emo.ui.core.ex.setNavTransparent
import cn.qhplus.emo.ui.core.ex.setNormalDisplayCutoutMode
import cn.qhplus.emo.ui.core.modifier.throttleClick
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.text.NumberFormat

class MainActivity : ComposeHostActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).run {
            isAppearanceLightNavigationBars = false
        }

        window.setNormalDisplayCutoutMode()
        window.setNavTransparent()

        // TODO Fix this for xiaomi
        lifecycleScope.launch {
            delay(100)
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    @Composable
    override fun Content() {
        EmoTheme {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                SchemeNavHost()
                DebugInfo()
            }
        }
    }

    override fun schemeClient(): SchemeClient {
        return EmoScheme
    }
}

@Composable
fun BoxWithConstraintsScope.DebugInfo() {
    var expend by remember {
        mutableStateOf(false)
    }

    val size = 48.dp
    val edgeProtection = 30.dp
    val defaultRightMargin = with(LocalDensity.current) {
        30.dp.toPx()
    }
    val defaultBottomMargin = with(LocalDensity.current) {
        140.dp.toPx()
    }
    var offsetX by remember { mutableStateOf(-defaultRightMargin) }
    var offsetY by remember { mutableStateOf(-defaultBottomMargin) }

    val offsetXDp = with(LocalDensity.current) {
        offsetX.toDp().coerceAtLeast(-maxWidth + size + edgeProtection).coerceAtMost(-edgeProtection)
    }

    val offsetYDp = with(LocalDensity.current) {
        offsetY.toDp().coerceAtLeast(-maxHeight + size + edgeProtection).coerceAtMost(-edgeProtection)
    }

    if (expend) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(offsetXDp, offsetYDp)
                .shadow(12.dp, RoundedCornerShape(8.dp), true)
                .background(Color.White)
                .throttleClick {
                    expend = false
                }
                .padding(14.dp)
        ) {
            NetworkBaseInfo()
            NetworkStreamTotalInfo()
            NetworkBandwidthInfo()
        }
    } else {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(offsetXDp, offsetYDp)
                .size(48.dp)
                .shadow(12.dp, CircleShape, true)
                .background(Color.White)
                .pointerInput(Unit) {
                    coroutineScope {
                        launch {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            }
                        }

                        launch {
                            detectTapGestures {
                                expend = true
                            }
                        }
                    }
                }
        ) {
            Text(
                text = "Monitor",
                fontSize = 9.sp,
                color = Color.Black,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
fun NetworkBaseInfo() {
    val context = LocalContext.current
    val state = remember {
        mutableStateOf<NetworkState?>(null)
    }

    LaunchedEffect(context) {
        NetworkConnectivity.of(context).stateFlow.collectLatest {
            state.value = it
        }
    }

    val value = state.value
    if (value != null) {
        Column {
            Text("network type = ${value.networkType}")
            Text("valid = ${value.isValid}")
        }
    }
}

@Composable
fun NetworkStreamTotalInfo() {
    val context = LocalContext.current
    val state = remember {
        mutableStateOf(NetworkStreamTotal.ZERO)
    }

    LaunchedEffect(context) {
        NetworkBandwidthSampler.of(context).streamTotalFlow.collectLatest {
            state.value = it
        }
    }

    val value = state.value
    if (value != NetworkStreamTotal.ZERO) {
        Column {
            Text("total.up = ${Formatter.formatShortFileSize(context, value.up)}")
            Text("total.down = ${Formatter.formatShortFileSize(context, value.down)}")
        }
    }
}

@Composable
fun NetworkBandwidthInfo() {
    val context = LocalContext.current
    val state = remember {
        mutableStateOf(NetworkBandwidth.UNDEFINED)
    }

    val formatter = remember {
        val format = NumberFormat.getNumberInstance()
        format.maximumFractionDigits = 2
        format
    }

    LaunchedEffect(context) {
        NetworkBandwidthSampler.of(context).bandwidthFlow.collectLatest {
            state.value = it
        }
    }

    val value = state.value
    if (value != NetworkBandwidth.UNDEFINED) {
        Column {
            Text("bandwidth.up = ${formatter.format(value.up)} kbps")
            Text("bandwidth.down = ${formatter.format(value.down)} kbps")
        }
    }
}
