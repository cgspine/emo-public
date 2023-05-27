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
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import cn.qhplus.emo.core.EmoConfig
import cn.qhplus.emo.core.EmoLog
import cn.qhplus.emo.scheme.QueryFormat
import cn.qhplus.emo.scheme.SchemeClient
import cn.qhplus.emo.scheme.SchemeDef
import cn.qhplus.emo.scheme.parse
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import kotlinx.serialization.decodeFromString

open class ComposeFixActivity : ComponentActivity() {
    override fun setContentView(view: View?) {
        if (view is ComposeView) {
            view.consumeWindowInsets = false
        }
        super.setContentView(view)
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        if (view is ComposeView) {
            view.consumeWindowInsets = false
        }
        super.setContentView(view, params)
    }
}

abstract class ComposeHostActivity : ComposeFixActivity() {

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
                instance.build(schemeClient(), this)
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
    fun build(client: SchemeClient, navGraphBuilder: NavGraphBuilder)
}