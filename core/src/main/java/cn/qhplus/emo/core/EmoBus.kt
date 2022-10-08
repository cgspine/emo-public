package cn.qhplus.emo.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class EmoEventProp(
    val sticky: Boolean = false,
    val keepChannelAlive: Boolean = false,
    val extraBufferCapacity: Int = 1,
    val onBufferOverflow: BufferOverflow = BufferOverflow.DROP_OLDEST
)

class EmoBus {

    companion object {
        val default: EmoBus by lazy {
            EmoBus()
        }
    }

    private val strongFlowMap = ConcurrentHashMap<Class<*>, MutableSharedFlow<*>>()
    private val weakFlowMap = ConcurrentHashMap<Class<*>, WeakReference<MutableSharedFlow<*>>>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        scope.launch {
            while (true){
                delay(10 * 60 * 1000)
                // clear empty values.
                weakFlowMap.forEach {
                    val value = it.value
                    if(value.get() == null){
                        weakFlowMap.remove(it.key, value)
                    }
                }
            }
        }
    }

    fun <T : Any> emitNonSuspend(cls: Class<T>, event: T): Job {
        return scope.launch {
            emit(cls, event)
        }
    }

    suspend fun <T : Any> emit(cls: Class<T>, event: T) {
        val prop = cls.getAnnotation(EmoEventProp::class.java)
        if (prop != null && prop.sticky) {
            var flow = strongFlowMap[cls]
            if (flow == null) {
                val new = createFlow<T>(prop)
                flow = strongFlowMap.putIfAbsent(cls, new) ?: new
            }
            (flow as MutableSharedFlow<T>).emit(event)
        } else {
            (weakFlowMap[cls]?.get() as? MutableSharedFlow<T>)?.emit(event)
        }
    }

    fun <T : Any> flowOf(cls: Class<T>): SharedFlow<T> {
        val prop = cls.getAnnotation(EmoEventProp::class.java)
        if (prop != null && (prop.sticky || prop.keepChannelAlive)) {
            var flow = strongFlowMap[cls]
            if (flow == null) {
                val new = createFlow<T>(prop)
                flow = strongFlowMap.putIfAbsent(cls, new) ?: new
            }
            return (flow as MutableSharedFlow<T>).asSharedFlow()
        } else {
            while (true) {
                val weakReference = weakFlowMap[cls]
                if (weakReference == null) {
                    val new = createFlow<T>(prop)
                    val ref = WeakReference<MutableSharedFlow<*>>(new)
                    val exit = weakFlowMap.putIfAbsent(cls, ref)
                    if (exit != null) {
                        val flow = exit.get()
                        if (flow != null) {
                            return (flow as MutableSharedFlow<T>).asSharedFlow()
                        }
                    } else {
                        return new.asSharedFlow()
                    }
                } else {
                    val flow = weakReference.get()
                    if (flow == null) {
                        val new = createFlow<T>(prop)
                        val ref = WeakReference<MutableSharedFlow<*>>(new)
                        if (weakFlowMap.replace(cls, weakReference, ref)) {
                            return new.asSharedFlow()
                        }
                    }else{
                        return (flow as MutableSharedFlow<T>).asSharedFlow()
                    }
                }
            }
        }
    }


    private fun <T : Any> createFlow(
        prop: EmoEventProp?
    ): MutableSharedFlow<T> {
        return MutableSharedFlow(
            if (prop?.sticky == true) 1 else 0,
            prop?.extraBufferCapacity ?: 1,
            prop?.onBufferOverflow ?: BufferOverflow.DROP_OLDEST
        )
    }

    fun <T : Any> removeChannel(cls: Class<T>) {
        val prop = cls.getAnnotation(EmoEventProp::class.java)
        if (prop != null && (prop.sticky || prop.keepChannelAlive)) {
            strongFlowMap.remove(cls)
        }else{
            weakFlowMap.remove(cls)
        }
    }
}

suspend inline fun <reified T : Any> EmoBus.emit(event: T) = emit(T::class.java, event)
inline fun <reified T : Any> EmoBus.emitNonSuspend(event: T) = emitNonSuspend(T::class.java, event)
inline fun <reified T : Any> EmoBus.flowOf(): SharedFlow<T> = flowOf(T::class.java)
inline fun <reified T : Any> EmoBus.removeChannel() = removeChannel(T::class.java)