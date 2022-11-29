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

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ConfigCenter(
    val storage: ConfigStorage,
    val prodMode: Boolean = true,
    autoClearUp: Boolean = true
) {

    private val configMap by lazy {
        try {
            val cls = Class.forName("cn.qhplus.emo.config.ConfigMapFactoryGenerated")
            val factory = cls.getDeclaredConstructor().newInstance() as ConfigMapFactory
            factory.factory(storage, prodMode)
        } catch (e: Throwable) {
            if (!prodMode) {
                throw e
            }
            ConfigMap(emptyMap(), emptyMap())
        }
    }

    init {
        if(autoClearUp){
            @OptIn(DelicateCoroutinesApi::class)
            GlobalScope.launch {
                // delay to clear up
                delay(15 * 1000)
                clearUp()
            }
        }
    }

    fun <T> actionOf(cls: Class<T>): ConfigAction {
        return configMap.actionMap[cls] ?: throw RuntimeException("${cls.simpleName} is not a config interface")
    }

    fun actionByName(name: String): ConfigAction? {
        return configMap.actionByName(name)
    }

    fun clearUp(){
        storage.clearUp(configMap.actionMap.values.map { it.meta })
    }

    fun getAll(): List<ConfigAction> {
        return configMap.actionMap.values.toList()
    }

    fun getAllAndGroupByCategory(): Map<String, List<ConfigAction>> {
        return getAll().groupBy { it.meta.category }
    }

    fun getByCategory(category: String): List<ConfigAction> {
        return getAll().filter { it.meta.category == category }
    }

    fun getByTag(tag: String): List<ConfigAction> {
        return getAll().filter { it.meta.tags.contains(tag) }
    }

    fun removeIf(predicate: (ConfigAction) -> Boolean) {
        val metas = getAll().asSequence().filter(predicate).map { it.meta }.toList()
        return storage.remove(metas)
    }

    suspend fun writeMap(
        map: Map<String, Any>,
        onConfigNotFind: OnConfigNotDefined = OnConfigNotDefined {_, _ -> false },
        onValueTypeNotMatch: OnValueTypeNotMatched = OnValueTypeNotMatched { action, configCls, _, actual, expected ->
            if (!prodMode) {
                throw RuntimeException(
                    "Value type for ${configCls.simpleName}[name = ${action.meta.name}] is not matched. expected ${expected.simpleName}, actual ${actual.simpleName}"
                )
            }
            false
        }
    ) = withContext(Dispatchers.IO) {
        for ((name, value) in map) {
            val cls = configMap.clsByName(name)
            val action = cls?.let { configMap.actionMap[it] } ?: if (onConfigNotFind.invoke(name, value)) {
                break
            } else {
                continue
            }
            if(action is IntConfigAction){
                if(value is Int){
                    action.write(value)
                } else if(value is String){
                    try {
                        action.write(value.toInt())
                    }catch (ignore: Throwable){
                        if(onValueTypeNotMatch.invoke(action, cls, value, Int::class.java, String::class.java)){
                            break
                        }
                    }
                }
            } else if(action is BoolConfigAction) {
                if(value is Boolean){
                    action.write(value)
                }else if(value == 0 || value == "false" || value == "0"){
                    action.write(false)
                }else if(value == 1 || value == "true" || value == "1"){
                    action.write(true)
                }else{
                    if(onValueTypeNotMatch.invoke(action, cls, value, Boolean::class.java, value.javaClass)){
                        break
                    }
                }
            } else if (action is LongConfigAction){
                if(value is Long){
                    action.write(value)
                }else if(value is Int){
                    action.write(value.toLong())
                }else if(value is String){
                    try {
                        action.write(value.toLong())
                    }catch (ignore: Throwable){
                        if(onValueTypeNotMatch.invoke(action, cls, value, Long::class.java, String::class.java)){
                            break
                        }
                    }
                }else{
                    if(onValueTypeNotMatch.invoke(action, cls, value, Long::class.java, value.javaClass)){
                        break
                    }
                }
            } else if (action is FloatConfigAction){
                if(value is Number){
                    action.write(value.toFloat())
                }else if(value is String){
                    try {
                        action.write(value.toFloat())
                    }catch (ignore: Throwable){
                        if(onValueTypeNotMatch.invoke(action, cls, value, Float::class.java, String::class.java)){
                            break
                        }
                    }
                }else{
                    if(onValueTypeNotMatch.invoke(action, cls, value, Float::class.java, value.javaClass)){
                        break
                    }
                }
            } else if (action is DoubleConfigAction){
                if(value is Number){
                    action.write(value.toDouble())
                }else if(value is String){
                    try {
                        action.write(value.toDouble())
                    }catch (ignore: Throwable){
                        if(onValueTypeNotMatch.invoke(action, cls, value, Double::class.java, String::class.java)){
                            break
                        }
                    }
                }else{
                    if(onValueTypeNotMatch.invoke(action, cls, value, Double::class.java, value.javaClass)){
                        break
                    }
                }
            } else if(action is StringConfigAction){
                if(value is String){
                    action.write(value)
                }else{
                    if(onValueTypeNotMatch.invoke(action, cls, value, String::class.java, value.javaClass)){
                        break
                    }
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> implOf(cls: Class<T>): T? {
        val resolver = configMap.implMap[cls] ?: return null
        return resolver.resolve() as T?
    }

    fun interface OnConfigNotDefined {
        fun invoke(name: String, value: Any): Boolean
    }

    fun interface OnValueTypeNotMatched {
        fun invoke(action: ConfigAction, configCls: Class<*>, value: Any, expectedType: Class<*>, actualType: Class<*>): Boolean
    }
}

inline fun <reified T : Any> ConfigCenter.actionOf(): ConfigAction {
    return actionOf(T::class.java)
}

inline fun <reified T : Any> ConfigCenter.implOf(): T? {
    return implOf(T::class.java)
}
