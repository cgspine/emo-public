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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.lang.ref.WeakReference

class ConfigMeta(
    val name: String,
    val humanName: String,
    val versionRelated: Boolean,
    val category: String,
    val tags: Array<String>
)

sealed class ConfigAction(
    protected val storage: ConfigStorage,
    val meta: ConfigMeta
) {
    fun remove() {
        storage.remove(meta)
    }

    abstract fun readAsString(): String
    abstract fun writeFromString(value: String): Boolean
    abstract fun valueType(): Class<*>
}

class IntConfigAction(
    storage: ConfigStorage,
    meta: ConfigMeta,
    val default: Int
) : ConfigAction(storage, meta) {

    @Volatile
    private var stateFlow: WeakReference<MutableStateFlow<Int>>? = null

    fun stateFlowOf(): StateFlow<Int> {
        return stateFlow?.get() ?: synchronized(this) {
            stateFlow?.get() ?: MutableStateFlow(read()).also { stateFlow = WeakReference(it) }
        }
    }

    override fun readAsString(): String {
        return read().toString()
    }

    override fun writeFromString(value: String): Boolean {
        return value.toIntOrNull()?.run { write(this); true  } ?: false
    }

    fun read(): Int {
        return storage.readInt(meta, default)
    }

    fun write(value: Int) {
        storage.writeInt(meta, value)
        stateFlow?.get()?.value = value
    }

    override fun valueType(): Class<*> = Int::class.java
}

class BoolConfigAction(
    storage: ConfigStorage,
    meta: ConfigMeta,
    val default: Boolean
) : ConfigAction(storage, meta) {

    @Volatile
    private var stateFlow: WeakReference<MutableStateFlow<Boolean>>? = null

    fun stateFlowOf(): StateFlow<Boolean> {
        return stateFlow?.get() ?: synchronized(this) {
            stateFlow?.get() ?: MutableStateFlow(read()).also { stateFlow = WeakReference(it) }
        }
    }

    override fun readAsString(): String {
        return read().toString()
    }

    override fun writeFromString(value: String): Boolean {
        val lowercase = value.lowercase()
        if(lowercase == "1" || lowercase == "true"){
            write(true)
            return true
        }else if(lowercase == "0" || lowercase == "false"){
            write(false)
            return true
        }
        return false
    }

    override fun valueType(): Class<*> = Boolean::class.java

    fun read(): Boolean {
        return storage.readBool(meta, default)
    }

    fun write(value: Boolean) {
        storage.writeBool(meta, value)
        stateFlow?.get()?.value = value
    }
}

class LongConfigAction(
    storage: ConfigStorage,
    meta: ConfigMeta,
    val default: Long
) : ConfigAction(storage, meta) {

    @Volatile
    private var stateFlow: WeakReference<MutableStateFlow<Long>>? = null

    fun stateFlowOf(): StateFlow<Long> {
        return stateFlow?.get() ?: synchronized(this) {
            stateFlow?.get() ?: MutableStateFlow(read()).also { stateFlow = WeakReference(it) }
        }
    }

    override fun readAsString(): String {
        return read().toString()
    }

    override fun writeFromString(value: String): Boolean {
        return value.toLongOrNull()?.run { write(this); true  } ?: false
    }

    override fun valueType(): Class<*> = Long::class.java

    fun read(): Long {
        return storage.readLong(meta, default)
    }

    fun write(value: Long) {
        storage.writeLong(meta, value)
        stateFlow?.get()?.value = value
    }
}

class FloatConfigAction(
    storage: ConfigStorage,
    meta: ConfigMeta,
    private val default: Float
) : ConfigAction(storage, meta) {

    @Volatile
    private var stateFlow: WeakReference<MutableStateFlow<Float>>? = null

    fun stateFlowOf(): StateFlow<Float> {
        return stateFlow?.get() ?: synchronized(this) {
            stateFlow?.get() ?: MutableStateFlow(read()).also { stateFlow = WeakReference(it) }
        }
    }

    override fun readAsString(): String {
        return read().toString()
    }

    override fun writeFromString(value: String): Boolean {
        return value.toFloatOrNull()?.run { write(this); true  } ?: false
    }

    override fun valueType(): Class<*> = Float::class.java

    fun read(): Float {
        return storage.readFloat(meta, default)
    }

    fun write(value: Float) {
        storage.writeFloat(meta, value)
        stateFlow?.get()?.value = value
    }
}

class DoubleConfigAction(
    storage: ConfigStorage,
    meta: ConfigMeta,
    private val default: Double
) : ConfigAction(storage, meta) {

    @Volatile
    private var stateFlow: WeakReference<MutableStateFlow<Double>>? = null

    fun stateFlowOf(): StateFlow<Double> {
        return stateFlow?.get() ?: synchronized(this) {
            stateFlow?.get() ?: MutableStateFlow(read()).also { stateFlow = WeakReference(it) }
        }
    }

    override fun readAsString(): String {
        return read().toString()
    }

    override fun writeFromString(value: String): Boolean {
        return value.toDoubleOrNull()?.run { write(this); true  } ?: false
    }

    override fun valueType(): Class<*> = Double::class.java

    fun read(): Double {
        return storage.readDouble(meta, default)
    }

    fun write(value: Double) {
        storage.writeDouble(meta, value)
        stateFlow?.get()?.value = value
    }
}

class StringConfigAction(
    storage: ConfigStorage,
    meta: ConfigMeta,
    private val default: String
) : ConfigAction(storage, meta) {

    @Volatile
    private var stateFlow: WeakReference<MutableStateFlow<String>>? = null

    fun stateFlowOf(): StateFlow<String> {
        return stateFlow?.get() ?: synchronized(this) {
            stateFlow?.get() ?: MutableStateFlow(read()).also { stateFlow = WeakReference(it) }
        }
    }

    override fun readAsString(): String {
        return read()
    }

    override fun writeFromString(value: String): Boolean {
        write(value)
        return true
    }

    override fun valueType(): Class<*> = String::class.java

    fun read(): String {
        return storage.readString(meta, default)
    }

    fun write(value: String) {
        storage.writeString(meta, value)
        stateFlow?.get()?.value = value
    }
}

fun ConfigAction.concreteInt(): IntConfigAction {
    return this as IntConfigAction
}

fun ConfigAction.concreteLong(): LongConfigAction {
    return this as LongConfigAction
}

fun ConfigAction.concreteBool(): BoolConfigAction {
    return this as BoolConfigAction
}

fun ConfigAction.concreteFloat(): FloatConfigAction {
    return this as FloatConfigAction
}

fun ConfigAction.concreteDouble(): DoubleConfigAction {
    return this as DoubleConfigAction
}

fun ConfigAction.concreteString(): StringConfigAction {
    return this as StringConfigAction
}
