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
import java.lang.reflect.Method

class EmoReflectJsBridgeHandler(
    scope: CoroutineScope,
    private val obj: Any,
    bridgePropName: String = DEFAULT_BRIDGE_PROP_NAME,
    readyEventName: String = DEFAULT_READY_EVENT_NAME
) : EmoJsBridgeHandler(scope, bridgePropName, readyEventName) {

    private val supportedCmdListCache by lazy {
        obj::class.java.declaredMethods.map { it.name }
    }
    private val methodsCache = mutableMapOf<String, Method>()

    override fun getSupportedCmdList(): List<String> {
        return supportedCmdListCache
    }

    override fun handleMessage(cmd: String, dataPicker: JsonDataPicker, callback: ResponseCallback?) {
        try {
            val method = methodsCache[cmd] ?: obj::class.java.getDeclaredMethod(
                cmd,
                CoroutineScope::class.java,
                JsonDataPicker::class.java,
                ResponseCallback::class.java
            ).also {
                it.isAccessible = true
                methodsCache[cmd] = it
            }
            method.invoke(obj, scope, dataPicker, callback)
        } catch (e: Throwable) {
            callback?.failed(e.message ?: "call method failed.")
        }
    }
}
