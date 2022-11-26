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
)

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

    fun read(): Int {
        return storage.readInt(meta.name, meta.versionRelated, default)
    }

    fun write(value: Int) {
        storage.writeInt(meta.name, meta.versionRelated, value)
        stateFlow?.get()?.value = value
    }
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

    fun read(): Boolean {
        return storage.readBool(meta.name, meta.versionRelated, default)
    }

    fun write(value: Boolean) {
        storage.writeBool(meta.name, meta.versionRelated, value)
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

    fun read(): Long {
        return storage.readLong(meta.name, meta.versionRelated, default)
    }

    fun write(value: Long) {
        storage.writeLong(meta.name, meta.versionRelated, value)
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

    fun read(): Float {
        return storage.readFloat(meta.name, meta.versionRelated, default)
    }

    fun write(value: Float) {
        storage.writeFloat(meta.name, meta.versionRelated, value)
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

    fun read(): Double {
        return storage.readDouble(meta.name, meta.versionRelated, default)
    }

    fun write(value: Double) {
        storage.writeDouble(meta.name, meta.versionRelated, value)
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

    fun read(): String {
        return storage.readString(meta.name, meta.versionRelated, default)
    }

    fun write(value: String) {
        storage.writeString(meta.name, meta.versionRelated, value)
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