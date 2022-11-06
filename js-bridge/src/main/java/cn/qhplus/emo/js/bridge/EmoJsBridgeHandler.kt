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

import android.webkit.ValueCallback
import android.webkit.WebView
import cn.qhplus.emo.core.EmoLog
import cn.qhplus.emo.core.LogTag
import cn.qhplus.emo.core.runIf
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

private const val EMO_BRIDGE_FILE_NAME = "emo-bridge.js"

private const val JS_METHOD_NAME_FETCH_QUEUE = "_fetchQueueFromNative"
private const val JS_METHOD_NAME_RESP = "_handleResponseFromNative"
private const val MSG_RESPONSE_ID = "responseId"
private const val MSG_DATA = "data"
private const val MSG_ERROR = "error"
private const val MSG_CMD = "cmd"

private const val CMD_GET_SUPPORTED_LIST = "__getSupportedCmdList__"
private const val CMD_ON_BRIDGE_READY = "__onBridgeReady__"

private val defaultCoroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
    EmoLog.e("EmoJsBridgeHandler", "scope error.", throwable)
}

abstract class EmoJsBridgeHandler(
    val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob() + defaultCoroutineExceptionHandler),
    private val bridgePropName: String = DEFAULT_BRIDGE_PROP_NAME,
    private val readyEventName: String = DEFAULT_READY_EVENT_NAME
) : LogTag {

    companion object {
        const val DEFAULT_BRIDGE_PROP_NAME = "EmoBridge"
        const val DEFAULT_READY_EVENT_NAME = "EmoBridgeReady"
    }

    private val waitingList = arrayListOf<() -> Unit>()

    var isBridgeReady: Boolean = false
        private set

    fun doAfterBridgeReady(block: () -> Unit) {
        if (isBridgeReady) {
            block()
        } else {
            waitingList.add(block)
        }
    }

    internal fun fetchAndHandleMessageFromJs(webView: WebView) {
        webView.evaluateJavascriptCatching("$bridgePropName.$JS_METHOD_NAME_FETCH_QUEUE()") { value ->
            if (value != null) {
                try {
                    val array = JSONArray(value)
                    for (i in 0 until array.length()) {
                        val message = array.getJSONObject(i)
                        parseMessage(webView, message)
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }

    internal fun loadBridgeScript(webView: WebView) {
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                webView
                    .context
                    .applicationContext
                    .assets
                    .open(EMO_BRIDGE_FILE_NAME)
                    .buffered()
                    .use {
                        String(it.readBytes())
                    }
                    .runIf(bridgePropName.isNotBlank() && bridgePropName != DEFAULT_BRIDGE_PROP_NAME) {
                        replace("window.EmoBridge", "window.$bridgePropName")
                    }
                    .runIf(readyEventName.isNotBlank()) {
                        replace("EmoBridgeReady", readyEventName)
                    }
            }
            webView.evaluateJavascriptCatching(text, null)
        }
    }

    private fun parseMessage(webView: WebView, message: JSONObject) {
        when (val cmd = message.getString(MSG_CMD)) {
            CMD_GET_SUPPORTED_LIST -> {
                val responseId = message.getString(MSG_RESPONSE_ID)
                val script = createResponseScript(responseId, JSONArray(getSupportedCmdList()))
                webView.evaluateJavascriptCatching(script, null)
            }
            CMD_ON_BRIDGE_READY -> {
                isBridgeReady = true
                waitingList.forEach {
                    it()
                }
                waitingList.clear()
            }
            else -> {
                val needResponse = message.has(MSG_RESPONSE_ID)
                val dataPicker = JsonDataPicker(message)
                var callback: ResponseCallback? = null
                if (needResponse) {
                    val responseId = message.getString(MSG_RESPONSE_ID)
                    callback = object : ResponseCallback {
                        override val responseId: String = responseId

                        private fun innerFinish(data: Any?) {
                            scope.launch {
                                webView.evaluateJavascriptCatching(createResponseScript(responseId, data), null)
                            }
                        }

                        override fun finish() {
                            innerFinish(null)
                        }

                        override fun finish(data: Boolean) {
                            innerFinish(data)
                        }

                        override fun finish(data: Int) {
                            innerFinish(data)
                        }

                        override fun finish(data: Long) {
                            innerFinish(data)
                        }

                        override fun finish(data: Double) {
                            innerFinish(data)
                        }

                        override fun finish(data: String) {
                            innerFinish(data)
                        }

                        override fun finish(data: JSONArray) {
                            innerFinish(data)
                        }

                        override fun finish(data: JSONObject) {
                            innerFinish(data)
                        }

                        override fun failed(error: String) {
                            scope.launch {
                                webView.evaluateJavascriptCatching(createErrorScript(responseId, error), null)
                            }
                        }
                    }
                }
                handleMessage(cmd, dataPicker, callback)
            }
        }
    }

    private fun WebView.evaluateJavascriptCatching(script: String, resultCallback: ValueCallback<String>?) {
        kotlin.runCatching {
            evaluateJavascript(script, resultCallback)
        }.takeIf { it.isFailure }?.run {
            EmoLog.e(TAG, "js evaluateJavascript failed， code = $script", exceptionOrNull())
        }
    }

    private fun createErrorScript(responseId: String, error: String): String {
        val response = JSONObject()
        response.put(MSG_RESPONSE_ID, responseId)
        response.put(MSG_ERROR, error)
        return "$bridgePropName.$JS_METHOD_NAME_RESP($response)"
    }

    private fun createResponseScript(responseId: String, data: Any?): String {
        val response = JSONObject()
        response.put(MSG_RESPONSE_ID, responseId)
        response.put(MSG_DATA, data)
        return "$bridgePropName.$JS_METHOD_NAME_RESP($response)"
    }

    protected abstract fun getSupportedCmdList(): List<String>

    protected abstract fun handleMessage(cmd: String, dataPicker: JsonDataPicker, callback: ResponseCallback?)

    interface ResponseCallback {

        val responseId: String

        fun finish()

        fun finish(data: Boolean)

        fun finish(data: Int)

        fun finish(data: Long)

        fun finish(data: Double)

        fun finish(data: String)

        fun finish(data: JSONArray)

        fun finish(data: JSONObject)

        fun failed(error: String)
    }

    class JsonDataPicker(private val json: JSONObject) {
        fun pickAsJsonArray(): JSONArray? {
            return json.optJSONArray(MSG_DATA)
        }

        fun pickAsJsonObject(): JSONObject? {
            return json.optJSONObject(MSG_DATA)
        }

        fun pickAsString(): String? {
            return if (json.isNull(MSG_DATA)) null else json.optString(MSG_DATA)
        }

        fun pickAsInt(defaultValue: Int = 0): Int {
            return json.optInt(MSG_DATA, defaultValue)
        }

        fun pickAsBool(defaultValue: Boolean = false): Boolean {
            return json.optBoolean(MSG_DATA, defaultValue)
        }

        fun pickAsLong(defaultValue: Long = 0): Long {
            return json.optLong(MSG_DATA, defaultValue)
        }

        fun pickAsDouble(defaultValue: Double = 0.0): Double {
            return json.optDouble(MSG_DATA, defaultValue)
        }
    }
}
