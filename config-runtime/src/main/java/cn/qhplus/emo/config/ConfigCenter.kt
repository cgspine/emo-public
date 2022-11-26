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

class ConfigCenter(
    val storage: ConfigStorage,
    val prodMode: Boolean = true
) {

    private val configMap by lazy {
        try {
            val configMapFactoryClsName = ConfigMapFactory::class.java.name
            val dot: Int = configMapFactoryClsName.lastIndexOf('.')
            val packageName = if (dot != -1) configMapFactoryClsName.substring(0, dot) else ""
            val cls = Class.forName("$packageName.ConfigMapFactoryGenerated")
            val factory = cls.getDeclaredConstructor().newInstance() as ConfigMapFactory
            factory.factory(storage, prodMode)
        } catch (e: Throwable) {
            if (!prodMode) {
                throw e
            }
            ConfigMap(emptyMap(), emptyMap())
        }
    }

    fun <T> actionOf(cls: Class<T>): ConfigAction {
        return configMap.actionMap[cls] ?: throw RuntimeException("${cls.simpleName} is not a config interface")
    }

    fun actionByName(name: String): ConfigAction? {
        return configMap.actionByName(name)
    }

    fun getAll(): List<ConfigAction> {
        return configMap.actionMap.values.toList()
    }

    fun getAllAndGroupByCategory(): Map<String, List<ConfigAction>> {
        return getAll().groupBy { it.meta.category }
    }

    fun getByCategory(category: String): List<ConfigAction>{
        return getAll().filter { it.meta.category == category }
    }

    fun getByTag(tag: String): List<ConfigAction>{
        return getAll().filter { it.meta.tags.contains(tag) }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> implOf(cls: Class<T>): T? {
        val resolver = configMap.implMap[cls] ?: return null
        return resolver.resolve() as T?
    }
}

inline fun <reified T : Any> ConfigCenter.actionOf(): ConfigAction {
    return actionOf(T::class.java)
}

inline fun <reified T : Any> ConfigCenter.implOf(): T? {
    return implOf(T::class.java)
}
