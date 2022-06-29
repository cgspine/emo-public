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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cn.qhplus.emo.ui.core.TopBarBackIconItem
import cn.qhplus.emo.ui.core.TopBarWithLazyListScrollState

@Composable
fun ModalPage(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val scrollState = rememberLazyListState()
        TopBarWithLazyListScrollState(
            scrollState,
            title = "Modal",
            leftItems = listOf(
                TopBarBackIconItem(tint = MaterialTheme.colorScheme.onPrimary) {
                    navController.popBackStack()
                }
            )
        )
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(top = 8.dp)
        ) {
            item {
                CommonItem("Dialog") {
                }
            }

            item {
                CommonItem("Toast") {
                }
            }
        }
    }
}
