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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import cn.qhplus.emo.EmoScheme
import cn.qhplus.emo.WebViewActivity
import cn.qhplus.emo.config.SchemeConst
import cn.qhplus.emo.scheme.ComposeScheme
import cn.qhplus.emo.ui.core.TopBar
import cn.qhplus.emo.ui.core.TopBarBackIconItem
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState

@ComposeScheme(
    action = SchemeConst.SCHEME_ACTION_ABOUT,
    alternativeHosts = [WebViewActivity::class]
)
@Composable
fun AboutPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val topBarIconColor = MaterialTheme.colorScheme.onPrimary
        TopBar(
            title = "About",
            leftItems = remember(topBarIconColor) {
                listOf(
                    TopBarBackIconItem(tint = topBarIconColor) {
                        EmoScheme.pop()
                    }
                )
            }
        )

        val state = rememberWebViewState("https://github.com/cgspine/emo")
        WebView(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}
