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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Grid3x3
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cn.qhplus.emo.R

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
        route = RouteConst.ROUTE_HOME_UTIL,
        selectedIcon = Icons.Filled.Grid3x3,
        unselectedIcon = Icons.Outlined.Grid3x3,
        iconTextId = R.string.util,
        content = {
            UtilPage(it)
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
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .shadow(16.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                        )
                    ),
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
                RouteConst.ROUTE_HOME_UTIL -> {
                    UtilPage(navController)
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                WindowInsets.statusBars
                    .union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
            )
    ) {
        Text(text = "Component")
    }
}

@Composable
fun UtilPage(navController: NavHostController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                WindowInsets.statusBars
                    .union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
            )
    ) {
        Text(text = "Util")
    }
}