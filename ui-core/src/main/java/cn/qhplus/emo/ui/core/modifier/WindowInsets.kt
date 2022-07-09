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

package cn.qhplus.emo.ui.core.modifier

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.debugInspectorInfo

@OptIn(ExperimentalLayoutApi::class)
fun Modifier.windowInsetsCommonTopPadding() = composed(
    inspectorInfo = debugInspectorInfo {
        name = "windowInsetsCommonTopPadding"
    }
) {
    windowInsetsPadding(
        WindowInsets.statusBarsIgnoringVisibility
            .union(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
    )
}

@OptIn(ExperimentalLayoutApi::class)
fun Modifier.windowInsetsCommonNavPadding() = composed(
    inspectorInfo = debugInspectorInfo {
        name = "windowInsetsCommonNavPadding"
    }
) {
    windowInsetsPadding(
        WindowInsets.navigationBarsIgnoringVisibility.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
    )
}

fun Modifier.windowInsetsCommonHorPadding() = composed(
    inspectorInfo = debugInspectorInfo {
        name = "windowInsetsCommonHorPadding"
    }
) {
    windowInsetsPadding(
        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
    )
}
