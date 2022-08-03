package cn.qhplus.emo.core

import kotlinx.coroutines.delay
import java.util.concurrent.CancellationException

fun <T> retry(times: Int, block: () -> T): T{
    var throwable: Throwable? = null
    for(i in 0 until times){
        try {
            return block()
        }catch (e: Throwable){
            throwable = e
            if(e is CancellationException){
                break
            }
        }
    }
    throw  throwable ?: RuntimeException("failed after retry $times times")
}

suspend fun <T> retry2(times: Int, failedDelay: Long = 0, block: suspend () -> T): T{
    var throwable: Throwable? = null
    for(i in 0 until times){
        try {
            return block()
        }catch (e: Throwable){
            throwable = e
            if(e is CancellationException){
                break
            }
        }
        if(failedDelay > 0){
            delay(failedDelay)
        }
    }
    throw  throwable ?: RuntimeException("failed after retry $times times")
}