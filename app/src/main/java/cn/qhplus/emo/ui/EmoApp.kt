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
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import cn.qhplus.emo.theme.EmoTheme
import cn.qhplus.emo.ui.page.HomePage
import cn.qhplus.emo.ui.page.ModalPage
import cn.qhplus.emo.ui.page.PhotoClipperPage
import cn.qhplus.emo.ui.page.PhotoPage
import cn.qhplus.emo.ui.page.PhotoPickerPage
import cn.qhplus.emo.ui.page.PhotoViewerPage
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EmoApp(windowSizeClass: WindowSizeClass) {
    EmoTheme {
        val navController = rememberAnimatedNavController()
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
        slideOut(tween(durationMillis = 250, delayMillis = 50)) { IntOffset(-it.width, 0) }
    }
}

@OptIn(ExperimentalAnimationApi::class)
val slideOutRight: AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition by lazy {
    {
        slideOut(tween(durationMillis = 250, delayMillis = 50)) { IntOffset(it.width, 0) }
    }
}
