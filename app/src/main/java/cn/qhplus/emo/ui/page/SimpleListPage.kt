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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cn.qhplus.emo.EmoScheme
import cn.qhplus.emo.ui.core.TopBarBackIconItem
import cn.qhplus.emo.ui.core.TopBarItem
import cn.qhplus.emo.ui.core.TopBarWithLazyListScrollState
import cn.qhplus.emo.ui.core.modifier.windowInsetsCommonHorPadding
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun SimpleListPage(
    title: CharSequence,
    topBarLeftItems: PersistentList<TopBarItem> = persistentListOf(),
    topBarRightItems: PersistentList<TopBarItem> = persistentListOf(),
    content: LazyListScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val scrollState = rememberLazyListState()
        TopBarWithLazyListScrollState(
            scrollState,
            title = { title },
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
                content()
            }
        )
    }
}

@Composable
fun OnlyBackListPage(
    title: CharSequence,
    content: LazyListScope.() -> Unit
) {
    val topBarIconColor = MaterialTheme.colorScheme.onPrimary
    SimpleListPage(
        title = title,
        topBarLeftItems = remember(topBarIconColor) {
            persistentListOf(
                TopBarBackIconItem(tint = topBarIconColor) {
                    EmoScheme.pop()
                }
            )
        },
        content = content
    )
}
