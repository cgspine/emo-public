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

import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.annotation.Keep
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import cn.qhplus.emo.EmoScheme
import cn.qhplus.emo.WebViewActivity
import cn.qhplus.emo.config.SchemeConst
import cn.qhplus.emo.js.bridge.EmoBridgeWebViewClientHelper
import cn.qhplus.emo.js.bridge.EmoJsBridgeHandler
import cn.qhplus.emo.js.bridge.EmoReflectJsBridgeHandler
import cn.qhplus.emo.scheme.ComposeScheme
import cn.qhplus.emo.ui.core.TopBar
import cn.qhplus.emo.ui.core.TopBarBackIconItem
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@ComposeScheme(
    action = SchemeConst.SCHEME_ACTION_JS_BRIDGE,
    alternativeHosts = [WebViewActivity::class]
)
@Composable
fun JsBridgePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val topBarIconColor = MaterialTheme.colorScheme.onPrimary
        TopBar(
            title = "JSBridge",
            leftItems = remember(topBarIconColor) {
                listOf(
                    TopBarBackIconItem(tint = topBarIconColor) {
                        EmoScheme.pop()
                    }
                )
            }
        )

        val scope = rememberCoroutineScope()
        val state = rememberWebViewState("file:///android_asset/demo.html")
        val client = remember {
            WebView.setWebContentsDebuggingEnabled(true)
//            val bridgeHandler = BusinessJsBridgeHandler(scope)
            val bridgeHandler = EmoReflectJsBridgeHandler(scope, BusinessJsReflect())
            BusinessWebViewClient(bridgeHandler)
        }
        WebView(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            onCreated = {
                it.settings.javaScriptEnabled = true
            },
            client = client
        )
    }
}

@Keep
class BusinessJsReflect {
    fun normal(scope: CoroutineScope, dataPicker: EmoJsBridgeHandler.JsonDataPicker, callback: EmoJsBridgeHandler.ResponseCallback?) {
        val data = dataPicker.pickAsJsonObject()!!
        val id = data.getInt("id")
        callback?.finish("收到 native 的结果， id = $id")
    }

    fun timeout(scope: CoroutineScope, dataPicker: EmoJsBridgeHandler.JsonDataPicker, callback: EmoJsBridgeHandler.ResponseCallback?) {
        scope.launch {
            delay(3000)
            val data = dataPicker.pickAsJsonObject()!!
            val id = data.getInt("id")
            callback?.finish("收到 native 的结果， id = $id")
        }
    }

    fun nativeError(scope: CoroutineScope, dataPicker: EmoJsBridgeHandler.JsonDataPicker, callback: EmoJsBridgeHandler.ResponseCallback?) {
        callback?.failed("native 告诉你失败了")
    }
}

class BusinessJsBridgeHandler(scope: CoroutineScope) : EmoJsBridgeHandler(scope) {
    override fun getSupportedCmdList(): List<String> {
        return listOf("normal", "timeout", "nativeError")
    }

    override fun handleMessage(cmd: String, dataPicker: JsonDataPicker, callback: ResponseCallback?) {
        when (cmd) {
            "normal" -> {
                val data = dataPicker.pickAsJsonObject()!!
                val id = data.getInt("id")
                callback?.finish("收到 native 的结果， id = $id")
            }
            "timeout" -> {
                scope.launch {
                    delay(3000)
                    val data = dataPicker.pickAsJsonObject()!!
                    val id = data.getInt("id")
                    callback?.finish("收到 native 的结果， id = $id")
                }
            }
            "nativeError" -> {
                callback?.failed("native 告诉你失败了")
            }
        }
    }
}

class BusinessWebViewClient(handler: EmoJsBridgeHandler) : AccompanistWebViewClient() {
    private val helper = EmoBridgeWebViewClientHelper(true, handler)

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        if (view != null && helper.shouldOverrideUrlLoading(view, request)) {
            return true
        }
        return super.shouldOverrideUrlLoading(view, request)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        view?.let {
            helper.doOnPageFinished(it)
        }
    }
}
