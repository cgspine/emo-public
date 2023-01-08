package cn.qhplus.emo.microbenchmark

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.os.Process
import android.util.Log
import androidx.core.content.ContextCompat

object Mem {

    fun logCurrent(context: Context, tag: String){
        Runtime.getRuntime().gc()
        val activityManager = ContextCompat.getSystemService(context,ActivityManager::class.java) ?: return
        val memInfo: Array<Debug.MemoryInfo> = activityManager.getProcessMemoryInfo(intArrayOf(Process.myPid()))
        memInfo.firstOrNull()?.run {

            Log.i(
                tag,
                memoryStats.asSequence().map {
                    "${it.key} = ${it.value}"
                }.joinToString(",\n")
            )
        }
        Log.i(tag, "native allocated size = ${Debug.getNativeHeapAllocatedSize() / 1024}")
    }
}