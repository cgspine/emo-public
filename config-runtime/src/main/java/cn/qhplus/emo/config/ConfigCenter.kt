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

import cn.qhplus.emo.config.ConfigCenter.OnConfigNotDefined
import cn.qhplus.emo.config.ConfigCenter.OnValueTypeNotMatched
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
        if (autoClearUp) {
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

    @Suppress("UNCHECKED_CAST")
    fun <T> resolverOf(cls: Class<T>): ConfigImplResolver<T>? {
        return configMap.implMap[cls] as? ConfigImplResolver<T>
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> implOf(cls: Class<T>): T? {
        val resolver = resolverOf(cls) ?: return null
        return resolver.resolve()
    }

    fun clsByName(name: String): Class<*>? {
        return configMap.clsByName(name)
    }

    fun actionByName(name: String): ConfigAction? {
        return configMap.actionByName(name)
    }

    fun clearUp() {
        storage.clearUp(configMap.actionMap.values.map { it.meta })
    }

    fun getAll(): Sequence<ConfigAction> {
        return configMap.actionMap.values.asSequence()
    }

    fun getByCategory(category: String): Sequence<ConfigAction> {
        return getAll().filter { it.meta.category == category }
    }

    fun getByTag(tag: String): Sequence<ConfigAction> {
        return getAll().filter { it.meta.tags.contains(tag) }
    }

    fun removeIf(predicate: (ConfigAction) -> Boolean) {
        val metas = getAll().asSequence().filter(predicate).map { it.meta }.toList()
        return storage.remove(metas)
    }

    suspend fun writeMap(
        map: Map<String, Any>,
        onConfigNotFind: OnConfigNotDefined = OnConfigNotDefined { _, _ -> false },
        onValueTypeNotMatch: OnValueTypeNotMatched = OnValueTypeNotMatched { action, configCls, _, actual, expected ->
            if (!prodMode) {
                throw RuntimeException(
                    "Value type for ${configCls.simpleName}[name = ${action.meta.name}] is not matched. " +
                        "expected ${expected.simpleName}, actual ${actual.simpleName}"
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
            if (value is String) {
                if (action.writeFromString(value)) {
                    continue
                }
            } else if (action is IntConfigAction) {
                if (value is Int) {
                    action.write(value)
                    continue
                }
            } else if (action is BoolConfigAction) {
                if (value is Boolean) {
                    action.write(value)
                    continue
                } else if (value == 0) {
                    action.write(false)
                    continue
                } else if (value == 1) {
                    action.write(true)
                    continue
                }
            } else if (action is LongConfigAction) {
                if (value is Long) {
                    action.write(value)
                    continue
                } else if (value is Int) {
                    action.write(value.toLong())
                    continue
                }
            } else if (action is FloatConfigAction) {
                if (value is Number) {
                    action.write(value.toFloat())
                    continue
                }
            } else if (action is DoubleConfigAction) {
                if (value is Number) {
                    action.write(value.toDouble())
                    continue
                }
            }

            if (onValueTypeNotMatch.invoke(action, cls, value, action.valueType(), String::class.java)) {
                break
            }
        }
    }

    fun interface OnConfigNotDefined {
        fun invoke(name: String, value: Any): Boolean
    }

    fun interface OnValueTypeNotMatched {
        fun invoke(
            action: ConfigAction,
            configCls: Class<*>,
            value: Any,
            expectedType: Class<*>,
            actualType: Class<*>
        ): Boolean
    }
}

inline fun <reified T : Any> ConfigCenter.actionOf(): ConfigAction {
    return actionOf(T::class.java)
}

inline fun <reified T : Any> ConfigCenter.resolverOf(): ConfigImplResolver<T>? {
    return resolverOf(T::class.java)
}

inline fun <reified T : Any> ConfigCenter.implOf(): T? {
    return implOf(T::class.java)
}
