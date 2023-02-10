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

import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.Keep
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import cn.qhplus.emo.config.SchemeConst
import cn.qhplus.emo.js.bridge.EmoBridgeWebViewClientHelper
import cn.qhplus.emo.js.bridge.EmoJsBridgeHandler
import cn.qhplus.emo.js.bridge.EmoReflectJsBridgeHandler
import cn.qhplus.emo.scheme.ActivityScheme
import cn.qhplus.emo.scheme.SchemeStringArg
import cn.qhplus.emo.theme.EmoTheme
import cn.qhplus.emo.ui.core.TopBar
import cn.qhplus.emo.ui.core.TopBarBackIconItem
import cn.qhplus.emo.ui.core.ex.setNavTransparent
import cn.qhplus.emo.ui.core.ex.setNormalDisplayCutoutMode
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@ActivityScheme(
    action = SchemeConst.SCHEME_ACTION_WEB
)
@SchemeStringArg(SchemeConst.SCHEME_ARG_TITLE)
@SchemeStringArg(SchemeConst.SCHEME_ARG_URL)
class WebViewActivity : ComponentActivity() {

    val title = mutableStateOf("")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setNormalDisplayCutoutMode()
        window.setNavTransparent()
        title.value = intent.getStringExtra(SchemeConst.SCHEME_ARG_TITLE)?.let { Uri.decode(it) } ?: ""
        setContent {
            val state = rememberWebViewState(
                intent.getStringExtra(SchemeConst.SCHEME_ARG_URL)?.let { Uri.decode(it) } ?: ""
            )
            val scope = rememberCoroutineScope()
            val client = remember {
                val bridgeHandler = EmoReflectJsBridgeHandler(scope, BusinessJsReflect)
                BusinessWebViewClient(bridgeHandler)
            }
            EmoTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopBar(
                        title = { title.value },
                        leftItems = listOf(TopBarBackIconItem{
                            EmoScheme.pop()
                        })
                    )
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
        }
    }

    inner class BusinessWebViewClient(handler: EmoJsBridgeHandler) : AccompanistWebViewClient() {
        private val helper = EmoBridgeWebViewClientHelper(true, handler)

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            if (view != null && helper.shouldOverrideUrlLoading(view, request)) {
                return true
            }
            val url = request?.url?.toString() ?: ""
            if(url.startsWith("${SchemeConst.SCHEME_PROTOCOL}://")){
                EmoScheme.handleQuietly(url)
                return true
            }
            return super.shouldOverrideUrlLoading(view, request)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            view?.let {
                helper.doOnPageFinished(it)
            }
            view?.title?.let {
                if(it.isNotBlank()){
                    title.value = it
                }
            }
        }
    }
}


@Suppress("UNUSED_PARAMETER", "unused")
@Keep
object BusinessJsReflect {
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

@Suppress("unused")
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
