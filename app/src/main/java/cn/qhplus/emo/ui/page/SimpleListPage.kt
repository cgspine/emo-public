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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cn.qhplus.emo.ui.core.TopBarBackIconItem
import cn.qhplus.emo.ui.core.TopBarItem
import cn.qhplus.emo.ui.core.TopBarWithLazyListScrollState
import cn.qhplus.emo.ui.core.modifier.windowInsetsCommonHorPadding

@Composable
fun SimpleListPage(
    navController: NavHostController,
    title: CharSequence,
    topBarLeftItems: List<TopBarItem> = emptyList(),
    topBarRightItems: List<TopBarItem> = emptyList(),
    content: LazyListScope.(NavHostController) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val scrollState = rememberLazyListState()
        TopBarWithLazyListScrollState(
            scrollState,
            title = title,
            leftItems = topBarLeftItems,
            rightItems = topBarRightItems
        )
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surface)
                .windowInsetsCommonHorPadding(),
            contentPadding = PaddingValues(top = 8.dp),
            content = {
                content(navController)
            }
        )
    }
}

@Composable
fun OnlyBackListPage(
    navController: NavHostController,
    title: CharSequence,
    content: LazyListScope.(NavHostController) -> Unit
) {
    val topBarIconColor = MaterialTheme.colorScheme.onPrimary
    SimpleListPage(
        navController = navController,
        title = title,
        topBarLeftItems = remember(topBarIconColor) {
            listOf(
                TopBarBackIconItem(tint = topBarIconColor) {
                    navController.popBackStack()
                }
            )
        },
        content = content
    )
}
