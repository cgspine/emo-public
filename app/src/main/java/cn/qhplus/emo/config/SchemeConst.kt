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

package cn.qhplus.emo.config

import cn.qhplus.emo.EmoScheme
import cn.qhplus.emo.scheme.SchemeBuilder

object SchemeConst {
    const val SCHEME_PROTOCOL = "emo"

    const val SCHEME_ACTION_HOME = "home"
    const val SCHEME_ACTION_MODAL = "modal"
    const val SCHEME_ACTION_ABOUT = "about"
    const val SCHEME_ACTION_PHOTO = "photo"
    const val SCHEME_ACTION_PERMISSION = "permission"
    const val SCHEME_ACTION_PHOTO_VIEWER = "photoViewer"
    const val SCHEME_ACTION_PHOTO_PICKER = "photoPicker"
    const val SCHEME_ACTION_PHOTO_CLIPPER = "photoClipper"
    const val SCHEME_ACTION_JS_BRIDGE = "jsBridge"
    const val SCHEME_ACTION_SCHEME = "scheme"
    const val SCHEME_ACTION_SCHEME_ARG = "scheme_arg"
    const val SCHEME_ACTION_SCHEME_ACTIVITY = "activity"
    const val SCHEME_ACTION_SCHEME_HOST_ARG = "host_arg"
    const val SCHEME_ACTION_SCHEME_ALPHA = "scheme_alpha"
    const val SCHEME_ACTION_SCHEME_SLIDE_BOTTOM = "scheme_slide_bottom"

    const val SCHEME_ARG_TAB = "tab"

    const val VALUE_TAB_HOME_COMPONENT = "component"
    const val VALUE_TAB_HOME_TEST = "test"
}

fun schemeBuilder(action: String): SchemeBuilder = SchemeBuilder(SchemeConst.SCHEME_PROTOCOL, action)
suspend fun SchemeBuilder.run(): Boolean {
    return EmoScheme.handle(toString())
}

fun SchemeBuilder.runQuietly() {
    EmoScheme.handleQuietly(toString())
}
