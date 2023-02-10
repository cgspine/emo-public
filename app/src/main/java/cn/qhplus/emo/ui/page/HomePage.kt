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

package cn.qhplus.emo.ui.page

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Grid3x3
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import cn.qhplus.emo.EmoKvInstance
import cn.qhplus.emo.MainActivity
import cn.qhplus.emo.R
import cn.qhplus.emo.config.SchemeConst
import cn.qhplus.emo.config.panel.ConfigPanel
import cn.qhplus.emo.config.runQuietly
import cn.qhplus.emo.config.schemeBuilder
import cn.qhplus.emo.config.webSchemeBuilder
import cn.qhplus.emo.configCenter
import cn.qhplus.emo.modal.emoBottomSheet
import cn.qhplus.emo.modal.emoToast
import cn.qhplus.emo.report.reportClick
import cn.qhplus.emo.scheme.ComposeScheme
import cn.qhplus.emo.scheme.SchemeStringArg
import cn.qhplus.emo.scheme.impl.SchemeKeys
import cn.qhplus.emo.ui.CommonItem
import cn.qhplus.emo.ui.core.TopBarTextItem
import cn.qhplus.emo.ui.core.modifier.windowInsetsCommonNavPadding

data class HomeDestination(
    val route: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector, val iconTextId: Int, val content: @Composable () -> Unit
)

val HOME_DESTINATIONS = listOf(HomeDestination(route = SchemeConst.VALUE_TAB_HOME_COMPONENT,
    selectedIcon = Icons.Filled.Widgets,
    unselectedIcon = Icons.Outlined.Widgets,
    iconTextId = R.string.component,
    content = {
        ComponentPage()
    }), HomeDestination(route = SchemeConst.VALUE_TAB_HOME_TEST,
    selectedIcon = Icons.Filled.Grid3x3,
    unselectedIcon = Icons.Outlined.Grid3x3,
    iconTextId = R.string.test,
    content = {
        TestPage()
    }))

@ComposeScheme(
    action = SchemeConst.SCHEME_ACTION_HOME, alternativeHosts = [MainActivity::class]
)
@SchemeStringArg(
    name = SchemeConst.SCHEME_ARG_TAB, default = SchemeConst.VALUE_TAB_HOME_COMPONENT
)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomePage(navBackStackEntry: NavBackStackEntry) {
    val tab = navBackStackEntry.arguments?.getString(SchemeConst.SCHEME_ARG_TAB) ?: SchemeConst.VALUE_TAB_HOME_COMPONENT
    val currentTab = rememberSaveable(tab) {
        mutableStateOf(tab)
    }

    LaunchedEffect("") {
        Log.i("EmoDemo", "exposure for scheme: ${navBackStackEntry.arguments?.getString(SchemeKeys.KEY_ORIGIN)?.let { Uri.decode(it) }}")
    }

    Scaffold(modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = WindowInsets.navigationBarsIgnoringVisibility,
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .shadow(16.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .windowInsetsCommonNavPadding(), tonalElevation = 0.dp
            ) {
                HOME_DESTINATIONS.forEach { destination ->
                    val selected = currentTab.value == destination.route
                    NavigationBarItem(selected = selected, onClick = {
                        currentTab.value = destination.route
                    }, icon = {
                        Icon(
                            if (selected) {
                                destination.selectedIcon
                            } else {
                                destination.unselectedIcon
                            }, contentDescription = null
                        )
                    }, label = { Text(stringResource(destination.iconTextId)) })
                }
            }
        }) { padding ->
        Surface(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
        ) {
            when (currentTab.value) {
                SchemeConst.VALUE_TAB_HOME_TEST -> {
                    TestPage()
                }
                else -> {
                    ComponentPage()
                }
            }
        }
    }
}

@Composable
fun ComponentPage() {
    val topBarIconColor = MaterialTheme.colorScheme.onPrimary
    SimpleListPage(title = "Components", topBarRightItems = remember(topBarIconColor) {
        listOf(TopBarTextItem(text = "文档", color = topBarIconColor) {
            webSchemeBuilder("https://emo.qhplus.cn", "emo").runQuietly()
        })
    }) {
        item {
            CommonItem("Modal") {
                schemeBuilder(SchemeConst.SCHEME_ACTION_MODAL).runQuietly()
            }
        }

        item {
            CommonItem("Photo") {
                schemeBuilder(SchemeConst.SCHEME_ACTION_PHOTO).runQuietly()
            }
        }

        item {
            CommonItem("Permission") {
                schemeBuilder(SchemeConst.SCHEME_ACTION_PERMISSION).runQuietly()
            }
        }

        item {
            CommonItem("JS Bridge") {
                webSchemeBuilder("file:///android_asset/demo.html", "JsBridge").runQuietly()
            }
        }

        item {
            val view = LocalView.current
            CommonItem("Config Panel") {
                view.emoBottomSheet {
                    ConfigPanel(configCenter)
                }.show()
            }
        }

        item {
            CommonItem("Scheme") {
                schemeBuilder(SchemeConst.SCHEME_ACTION_SCHEME).runQuietly()
            }
        }
    }
}

@Composable
fun TestPage() {
    val view = LocalView.current
    SimpleListPage(
        title = "Components"
    ) {
        item {
            CommonItem("ThrottleClick") {
                view.emoToast("use Modifier.throttleClick")
            }
        }

        item {
            CommonItem("Separator") {
                view.emoToast("use Modifier.top/bottom/left/rightSeparator")
            }
        }

        item {
            CommonItem("Report") {
                reportClick("test")
                view.emoToast("see the log")
                EmoKvInstance.put("hehe", "xx")
            }
        }
    }
}
