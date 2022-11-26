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

class ConfigMap(
    val actionMap: Map<Class<*>, ConfigAction>,
    val implMap: Map<Class<*>, ConfigImplResolver<*>>
) {
    private val nameMap = mutableMapOf<String, ConfigAction>().apply {
        actionMap.values.forEach {
            put(it.meta.name, it)
        }
    }

    fun actionByName(name: String): ConfigAction? {
        return nameMap[name]
    }
}

interface ConfigMapFactory {
    fun factory(storage: ConfigStorage, prodMode: Boolean): ConfigMap
}

interface ConfigImplResolver<T> {
    fun resolve(): T?
    fun setToNext(): T?
}

class ConfigImplItem<T, V>(
    val cls: Class<out T>?,
    val instance: T?,
    val value: V
)

abstract class InstanceListConfigImplResolver<T, V>(
    private val implList: List<ConfigImplItem<T, V>>,
    private val prodMode: Boolean
) : ConfigImplResolver<T> {
    private val instanceMap = mutableMapOf<Class<out T>, T>()

    override fun setToNext(): T? {
        val value = readValue()
        val index = implList.indexOfFirst { it.value == value }
        val item = implList.getOrElse(index + 1) {
            implList[0]
        }
        writeValue(item.value)
        return instanceOf(item)
    }

    override fun resolve(): T? {
        val value = readValue()
        val item = implList.find { it.value == value } ?: return null
        return instanceOf(item)
    }

    private fun instanceOf(item: ConfigImplItem<T, V>): T? {
        val objectInstance = item.instance
        if (objectInstance != null) {
            return objectInstance
        }
        val cls = item.cls ?: if (prodMode) {
            return null
        } else {
            throw RuntimeException("Config generate error.")
        }
        val cache = instanceMap[cls]
        if (cache != null) {
            return cache
        }
        return synchronized(this) {
            try {
                instanceMap[cls] ?: cls.getDeclaredConstructor().newInstance().also {
                    instanceMap[cls] = it
                }
            } catch (e: Throwable) {
                if (prodMode) {
                    null
                } else {
                    throw e
                }
            }
        }
    }

    abstract fun readValue(): V
    abstract fun writeValue(v: V)
}

class IntClsConfigImplResolver<T>(
    implList: List<ConfigImplItem<T, Int>>,
    prodMode: Boolean,
    private val action: IntConfigAction
) : InstanceListConfigImplResolver<T, Int>(implList, prodMode) {

    override fun readValue(): Int {
        return action.read()
    }

    override fun writeValue(v: Int) {
        action.write(v)
    }
}

class BoolClsConfigImplResolver<T>(
    implList: List<ConfigImplItem<T, Boolean>>,
    prodMode: Boolean,
    private val action: BoolConfigAction
) : InstanceListConfigImplResolver<T, Boolean>(implList, prodMode) {

    override fun readValue(): Boolean {
        return action.read()
    }

    override fun writeValue(v: Boolean) {
        action.write(v)
    }
}

class LongClsConfigImplResolver<T>(
    implList: List<ConfigImplItem<T, Long>>,
    prodMode: Boolean,
    private val action: LongConfigAction
) : InstanceListConfigImplResolver<T, Long>(implList, prodMode) {

    override fun readValue(): Long {
        return action.read()
    }

    override fun writeValue(v: Long) {
        action.write(v)
    }
}

class FloatClsConfigImplResolver<T>(
    implList: List<ConfigImplItem<T, Float>>,
    prodMode: Boolean,
    private val action: FloatConfigAction
) : InstanceListConfigImplResolver<T, Float>(implList, prodMode) {

    override fun readValue(): Float {
        return action.read()
    }

    override fun writeValue(v: Float) {
        action.write(v)
    }
}

class DoubleClsConfigImplResolver<T>(
    implList: List<ConfigImplItem<T, Double>>,
    prodMode: Boolean,
    private val action: DoubleConfigAction
) : InstanceListConfigImplResolver<T, Double>(implList, prodMode) {

    override fun readValue(): Double {
        return action.read()
    }

    override fun writeValue(v: Double) {
        action.write(v)
    }
}

class StringClsConfigImplResolver<T>(
    implList: List<ConfigImplItem<T, String>>,
    prodMode: Boolean,
    private val action: StringConfigAction
) : InstanceListConfigImplResolver<T, String>(implList, prodMode) {

    override fun readValue(): String {
        return action.read()
    }

    override fun writeValue(v: String) {
        action.write(v)
    }
}
