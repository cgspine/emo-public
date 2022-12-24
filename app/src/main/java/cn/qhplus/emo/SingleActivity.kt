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

package cn.qhplus.emo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import cn.qhplus.emo.config.SchemeConst
import cn.qhplus.emo.scheme.ActivityScheme
import cn.qhplus.emo.ui.CommonItem
import cn.qhplus.emo.ui.core.ex.setNavTransparent
import cn.qhplus.emo.ui.core.ex.setNormalDisplayCutoutMode
import cn.qhplus.emo.ui.page.OnlyBackListPage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@ActivityScheme(SchemeConst.SCHEME_ACTION_SCHEME_ACTIVITY)
class SingleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OnlyBackListPage(title = "SingleActivity") {
                item {
                    CommonItem("Back") {
                        EmoScheme.pop()
                    }
                }
            }
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).run {
            isAppearanceLightNavigationBars = false
        }

        window.setNormalDisplayCutoutMode()
        window.setNavTransparent()

        // TODO Fix this for xiaomi
        lifecycleScope.launch {
            delay(100)
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }
}
