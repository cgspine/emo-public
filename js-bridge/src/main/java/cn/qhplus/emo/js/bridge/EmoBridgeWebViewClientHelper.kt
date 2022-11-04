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

package cn.qhplus.emo.js.bridge

import android.webkit.WebResourceRequest
import android.webkit.WebView

private const val EMO_BRIDGE_QUEUE_MESSAGE = "emo://__QUEUE_MSG__/"

class EmoBridgeWebViewClientHelper(
    private val injectJsCode: Boolean,
    private val handler: EmoJsBridgeHandler
) {
    fun shouldOverrideUrlLoading(webView: WebView, request: WebResourceRequest?): Boolean {
        if (request?.url?.toString()?.startsWith(EMO_BRIDGE_QUEUE_MESSAGE) == true) {
            handler.fetchAndHandleMessageFromJs(webView)
            return true
        }
        return false
    }

    fun doOnPageFinished(webView: WebView) {
        if (injectJsCode) {
            handler.loadBridgeScript(webView)
        }
    }
}
