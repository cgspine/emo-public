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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumedWindowInsets
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cn.qhplus.emo.R
import cn.qhplus.emo.config.panel.ConfigPanel
import cn.qhplus.emo.configCenter
import cn.qhplus.emo.modal.emoBottomSheet
import cn.qhplus.emo.modal.emoToast
import cn.qhplus.emo.report.reportClick
import cn.qhplus.emo.ui.CommonItem
import cn.qhplus.emo.ui.RouteConst
import cn.qhplus.emo.ui.core.TopBarTextItem
import cn.qhplus.emo.ui.core.modifier.windowInsetsCommonNavPadding

data class HomeDestination(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val iconTextId: Int,
    val content: @Composable (navController: NavHostController) -> Unit
)

val HOME_DESTINATIONS = listOf(
    HomeDestination(
        route = RouteConst.ROUTE_HOME_COMPONENT,
        selectedIcon = Icons.Filled.Widgets,
        unselectedIcon = Icons.Outlined.Widgets,
        iconTextId = R.string.component,
        content = {
            ComponentPage(it)
        }
    ),
    HomeDestination(
        route = RouteConst.ROUTE_HOME_HELPER,
        selectedIcon = Icons.Filled.Grid3x3,
        unselectedIcon = Icons.Outlined.Grid3x3,
        iconTextId = R.string.helper,
        content = {
            HelperPage(it)
        }
    )
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomePage(navController: NavHostController, tab: String) {
    val currentTab = remember(tab) {
        mutableStateOf(tab)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = WindowInsets.navigationBarsIgnoringVisibility,
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .shadow(16.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .windowInsetsCommonNavPadding(),
                tonalElevation = 0.dp
            ) {
                HOME_DESTINATIONS.forEach { destination ->
                    val selected = currentTab.value == destination.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            currentTab.value = destination.route
                        },
                        icon = {
                            Icon(
                                if (selected) {
                                    destination.selectedIcon
                                } else {
                                    destination.unselectedIcon
                                },
                                contentDescription = null
                            )
                        },
                        label = { Text(stringResource(destination.iconTextId)) }
                    )
                }
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .padding(padding)
                .consumedWindowInsets(padding)
        ) {
            when (currentTab.value) {
                RouteConst.ROUTE_HOME_HELPER -> {
                    HelperPage(navController)
                }
                else -> {
                    ComponentPage(navController)
                }
            }
        }
    }
}

@Composable
fun ComponentPage(navController: NavHostController) {
    val topBarIconColor = MaterialTheme.colorScheme.onPrimary
    SimpleListPage(
        navController = navController,
        title = "Components",
        topBarRightItems = remember(topBarIconColor) {
            listOf(
                TopBarTextItem(text = "About", color = topBarIconColor) {
                    navController.navigate(RouteConst.ROUTE_ABOUT)
                }
            )
        }
    ) {
        item {
            CommonItem("Modal") {
                navController.navigate(RouteConst.ROUTE_MODAL)
            }
        }

        item {
            CommonItem("Photo") {
                navController.navigate(RouteConst.ROUTE_PHOTO)
            }
        }

        item {
            CommonItem("Permission") {
                navController.navigate(RouteConst.ROUTE_PERMISSION)
            }
        }

        item {
            CommonItem("JS Bridge") {
                navController.navigate(RouteConst.ROUTE_JS_BRIDGE)
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
    }
}

@Composable
fun HelperPage(navController: NavHostController) {
    val view = LocalView.current
    SimpleListPage(
        navController = navController,
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
            }
        }
    }
}
