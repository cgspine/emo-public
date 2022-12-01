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

package cn.qhplus.emo.ui

import android.text.format.Formatter
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import cn.qhplus.emo.network.NetworkBandwidth
import cn.qhplus.emo.network.NetworkBandwidthSampler
import cn.qhplus.emo.network.NetworkConnectivity
import cn.qhplus.emo.network.NetworkState
import cn.qhplus.emo.network.NetworkStreamTotal
import cn.qhplus.emo.theme.EmoTheme
import cn.qhplus.emo.ui.core.modifier.throttleClick
import cn.qhplus.emo.ui.page.AboutPage
import cn.qhplus.emo.ui.page.HomePage
import cn.qhplus.emo.ui.page.JsBridgePage
import cn.qhplus.emo.ui.page.ModalPage
import cn.qhplus.emo.ui.page.PermissionPage
import cn.qhplus.emo.ui.page.PhotoClipperPage
import cn.qhplus.emo.ui.page.PhotoPage
import cn.qhplus.emo.ui.page.PhotoPickerPage
import cn.qhplus.emo.ui.page.PhotoViewerPage
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EmoApp(windowSizeClass: WindowSizeClass) {
    EmoTheme {
        val navController = rememberAnimatedNavController()
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedNavHost(
                navController = navController,
                modifier = Modifier.fillMaxSize(),
                startDestination = "${RouteConst.ROUTE_HOME}/{${RouteConst.PARAM_TAB}}"
            ) {
                composable(
                    "${RouteConst.ROUTE_HOME}/{${RouteConst.PARAM_TAB}}",
                    exitTransition = slideOutLeft,
                    popEnterTransition = slideInLeft
                ) { backStack ->
                    HomePage(
                        navController,
                        backStack.arguments?.getString(RouteConst.PARAM_TAB)
                            ?: RouteConst.ROUTE_HOME_COMPONENT
                    )
                }

                slideComposable(
                    RouteConst.ROUTE_MODAL
                ) {
                    ModalPage(navController)
                }

                slideComposable(
                    RouteConst.ROUTE_JS_BRIDGE
                ) {
                    JsBridgePage(navController)
                }

                slideComposable(
                    RouteConst.ROUTE_ABOUT
                ) {
                    AboutPage(navController)
                }

                slideComposable(
                    RouteConst.ROUTE_PERMISSION
                ) {
                    PermissionPage(navController)
                }

                slideComposable(
                    RouteConst.ROUTE_PHOTO
                ) {
                    PhotoPage(navController)
                }

                slideComposable(
                    RouteConst.ROUTE_PHOTO_VIEWER
                ) {
                    PhotoViewerPage(navController)
                }

                slideComposable(
                    RouteConst.ROUTE_PHOTO_PICKER
                ) {
                    PhotoPickerPage(navController)
                }

                slideComposable(
                    RouteConst.ROUTE_PHOTO_CLIPPER
                ) {
                    PhotoClipperPage(navController)
                }
            }
            DebugInfo()
        }
    }
}


@Composable
fun BoxScope.DebugInfo() {
    var expend by remember {
        mutableStateOf(false)
    }

    val defaultBottomMargin = with(LocalDensity.current) {
        100.dp.toPx()
    }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(-defaultBottomMargin) }

    val offsetXDp = with(LocalDensity.current) {
        offsetX.toDp()
    }

    val offsetYDp = with(LocalDensity.current) {
        offsetY.toDp()
    }

    if (expend) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(offsetXDp + (-40).dp, offsetYDp + (-40).dp)
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
                .offset(offsetXDp + (-40).dp, offsetYDp + (-40).dp)
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

@ExperimentalAnimationApi
fun NavGraphBuilder.slideComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit
) {
    composable(
        route,
        arguments,
        deepLinks,
        slideInRight,
        slideOutLeft,
        slideInLeft,
        slideOutRight,
        content
    )
}

@OptIn(ExperimentalAnimationApi::class)
val slideInRight: AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition by lazy {
    {
        slideIn(tween(durationMillis = 300)) { IntOffset(it.width, 0) }
    }
}

@OptIn(ExperimentalAnimationApi::class)
val slideInLeft: AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition by lazy {
    {
        slideIn(tween(durationMillis = 300)) { IntOffset(-it.width, 0) }
    }
}

@OptIn(ExperimentalAnimationApi::class)
val slideOutLeft: AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition by lazy {
    {
        slideOut(tween(durationMillis = 300, delayMillis = 50)) { IntOffset(-it.width, 0) }
    }
}

@OptIn(ExperimentalAnimationApi::class)
val slideOutRight: AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition by lazy {
    {
        slideOut(tween(durationMillis = 300, delayMillis = 50)) { IntOffset(it.width, 0) }
    }
}
