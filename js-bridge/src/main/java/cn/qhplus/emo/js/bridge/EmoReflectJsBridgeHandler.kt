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

import kotlinx.coroutines.CoroutineScope

class EmoReflectJsBridgeHandler(
    scope: CoroutineScope,
    private val obj: Any,
    bridgePropName: String = DEFAULT_BRIDGE_PROP_NAME,
    readyEventName: String = DEFAULT_READY_EVENT_NAME
) : EmoJsBridgeHandler(scope, bridgePropName, readyEventName) {
    override fun getSupportedCmdList(): List<String> {
        return obj::class.java.declaredMethods.map { it.name }
    }

    override fun handleMessage(cmd: String, dataPicker: JsonDataPicker, callback: ResponseCallback?) {
        try {
            val method = obj::class.java.getDeclaredMethod(
                cmd,
                CoroutineScope::class.java,
                JsonDataPicker::class.java,
                ResponseCallback::class.java
            )
            method.isAccessible = true
            method.invoke(obj, scope, dataPicker, callback)
        } catch (e: Throwable) {
            if (callback == null) {
                kotlin.runCatching {
                    val method = obj::class.java.getDeclaredMethod(
                        cmd,
                        CoroutineScope::class.java,
                        JsonDataPicker::class.java
                    )
                    method.isAccessible = true
                    method.invoke(obj, scope, dataPicker)
                }
            } else {
                callback.failed(e.message ?: "call method failed.")
            }
        }
    }
}
