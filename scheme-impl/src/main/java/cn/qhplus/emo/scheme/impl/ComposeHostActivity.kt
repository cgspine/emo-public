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

package cn.qhplus.emo.scheme.impl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.core.view.WindowCompat
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import cn.qhplus.emo.scheme.SchemeClient
import cn.qhplus.emo.scheme.SchemeDef
import cn.qhplus.emo.scheme.SchemeTransition
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.rememberAnimatedNavController

abstract class ComposeHostActivity : ComponentActivity() {

    var navController: NavHostController? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            Content()
        }
    }

    abstract fun schemeClient(): SchemeClient

    open fun transitionConverter(): SchemeTransitionConverter {
        return DefaultSchemeTransitionConverter
    }

    @Composable
    open fun Content() {
        SchemeNavHost()
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun SchemeNavHost() {
        val navController = rememberAnimatedNavController()
        AnimatedNavHost(
            navController = navController,
            modifier = Modifier.fillMaxSize(),
            startDestination = intent.getStringExtra(SchemeKeys.KEY_START_DESTINATION)!!
        ) {
            intent.getBundleExtra(SchemeKeys.KEY_START_ARGUMENTS)?.let {
                getStartDestinationArgsFromBundle(it)
            }
            this@ComposeHostActivity.navController = navController
            try {
                val cls = Class.forName(this@ComposeHostActivity::class.java.name + SchemeDef.COMPOSE_CLASS_SUFFIX)
                val instance = cls.getConstructor().newInstance() as ComposeSchemeNavGraphBuilder
                instance.build(schemeClient(), this, transitionConverter())
            } catch (ignore: Throwable) {
            }
        }
        LaunchedEffect("") {
            intent.getStringArrayExtra(SchemeKeys.KEY_BATCH_SCHEME_LIST)?.forEach {
                navController.navigate(it)
            }
        }
    }
}

interface ComposeSchemeNavGraphBuilder {
    fun build(client: SchemeClient, navGraphBuilder: NavGraphBuilder, transitionConverter: SchemeTransitionConverter)
}

@OptIn(ExperimentalAnimationApi::class)
interface SchemeTransitionConverter {
    fun enterRes(value: Int): Int
    fun exitRes(value: Int): Int
    fun enterTransition(value: Int): (AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition?)?
    fun exitTransition(value: Int): (AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition?)?
}

@OptIn(ExperimentalAnimationApi::class)
object DefaultSchemeTransitionConverter : SchemeTransitionConverter {

    override fun enterRes(value: Int): Int {
        return when (value) {
            SchemeTransition.SLIDE_IN_LEFT -> R.anim.slide_in_left
            SchemeTransition.SLIDE_IN_RIGHT -> R.anim.slide_in_right
            SchemeTransition.SLIDE_IN_BOTTOM -> R.anim.slide_in_bottom
            SchemeTransition.SLIDE_IN_TOP -> R.anim.slide_in_top
            SchemeTransition.SCALE_IN -> R.anim.scale_enter
            else -> R.anim.slide_still
        }
    }

    override fun exitRes(value: Int): Int {
        return when (value) {
            SchemeTransition.SLIDE_OUT_LEFT -> R.anim.slide_out_left
            SchemeTransition.SLIDE_OUT_RIGHT -> R.anim.slide_out_right
            SchemeTransition.SLIDE_OUT_TOP -> R.anim.slide_out_top
            SchemeTransition.SLIDE_OUT_BOTTOM -> R.anim.slide_out_bottom
            SchemeTransition.SCALE_OUT -> R.anim.scale_exit
            else -> R.anim.slide_still
        }
    }

    override fun enterTransition(value: Int): (AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition?)? {
        return when (value) {
            SchemeTransition.SLIDE_IN_LEFT -> {
                {
                    slideIn(tween(durationMillis = 300)) { IntOffset(-it.width, 0) }
                }
            }
            SchemeTransition.SLIDE_IN_RIGHT -> {
                {
                    slideIn(tween(durationMillis = 300)) { IntOffset(it.width, 0) }
                }
            }
            SchemeTransition.SLIDE_IN_BOTTOM -> {
                {
                    slideIn(tween(durationMillis = 300)) { IntOffset(0, it.height) }
                }
            }
            SchemeTransition.SLIDE_IN_TOP -> {
                {
                    slideIn(tween(durationMillis = 300)) { IntOffset(0, -it.height) }
                }
            }
            SchemeTransition.SCALE_IN -> {
                {
                    fadeIn(tween(durationMillis = 300), 0f) + scaleIn(tween(durationMillis = 300), 0.8f)
                }
            }
            SchemeTransition.STILL -> {
                {
                    fadeIn(tween(300), 1f)
                }
            }
            else -> null
        }
    }

    override fun exitTransition(value: Int): (AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition?)? {
        return when (value) {
            SchemeTransition.SLIDE_OUT_LEFT -> {
                {
                    slideOut(tween(durationMillis = 300, delayMillis = 50)) { IntOffset(-it.width, 0) }
                }
            }
            SchemeTransition.SLIDE_OUT_RIGHT -> {
                {
                    slideOut(tween(durationMillis = 300, delayMillis = 50)) { IntOffset(it.width, 0) }
                }
            }
            SchemeTransition.SLIDE_OUT_TOP -> {
                {
                    slideOut(tween(durationMillis = 300, delayMillis = 50)) { IntOffset(0, -it.height) }
                }
            }
            SchemeTransition.SLIDE_OUT_BOTTOM -> {
                {
                    slideOut(tween(durationMillis = 300, delayMillis = 50)) { IntOffset(0, it.height) }
                }
            }
            SchemeTransition.SCALE_OUT -> {
                {
                    fadeOut(tween(durationMillis = 300), 0f) + scaleOut(tween(durationMillis = 300), 0.8f)
                }
            }
            SchemeTransition.STILL -> {
                {
                    fadeOut(tween(300), 1f)
                }
            }
            else -> null
        }
    }
}
