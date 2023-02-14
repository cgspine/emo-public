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

package cn.qhplus.emo.device

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileFilter
import java.lang.reflect.Method
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

private val CPU_FILTER by lazy {
    FileFilter { pathname -> Pattern.matches("cpu\\d", pathname.name) }
}

private val totalMemoryCacheRunner by lazy {
    CacheRunner<Context, Long> {
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = ContextCompat.getSystemService(it, ActivityManager::class.java)
        if (activityManager != null) {
            runCatching {
                activityManager.getMemoryInfo(memoryInfo)
                memoryInfo.totalMem
            }.getOrNull()
        } else {
            null
        }
    }
}

private val dataStorageSizeCacheRunner by lazy {
    CacheRunner<Unit, Long> {
        Environment.getDataDirectory()?.totalSpace
    }
}

private val extraStorageSizeCacheRunner by lazy {
    CacheRunner<Unit, Long> {
        if (!hasExtraStorage()) {
            0
        } else {
            val path: File = Environment.getExternalStorageDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val availableBlocks = stat.blockCountLong
            blockSize * availableBlocks
        }
    }
}

private val cpuCountCacheRunner by lazy {
    CacheRunner<Unit, Int> {
        runCatching {
            sequenceOf(
                "/sys/devices/system/cpu/possible",
                "/sys/devices/system/cpu/present"
            ).map {
                getCoresFromFile(it)
            }.firstOrNull {
                it > 0
            } ?: File("/sys/devices/system/cpu/").listFiles(CPU_FILTER)?.size ?: 1
        }.getOrDefault(1).coerceAtLeast(1)
    }
}

private val batteryCapacityCacheRunner by lazy {
    CacheRunner<Context, Double> {
        runCatching {
            val cls = Class.forName("com.android.internal.os.PowerProfile")
            val instance = cls.getConstructor(Context::class.java).newInstance(it)
            val method: Method = cls.getMethod("getBatteryCapacity")
            method.invoke(instance) as Double
        }.getOrDefault(-1.0)
    }
}

fun getTotalMemory(context: Context): Long {
    return totalMemoryCacheRunner.get(context) ?: -1
}

fun getDataStorageSize(): Long {
    return dataStorageSizeCacheRunner.get(Unit) ?: -1
}

fun hasExtraStorage(): Boolean {
    return Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()
}

fun getExtraStorageSize(): Long {
    return extraStorageSizeCacheRunner.get(Unit) ?: -1
}

fun getCpuCoreCount(): Int {
    return cpuCountCacheRunner.get(Unit) ?: 1
}

fun getBatteryCapacity(context: Context): Double {
    return batteryCapacityCacheRunner.get(context) ?: -1.0
}

private fun getCoresFromFile(file: String): Int {
    return File(file).inputStream()
        .reader(StandardCharsets.UTF_8)
        .buffered()
        .runCatching {
            use {
                val line = it.readLine()
                if (line.matches("0-\\d+$".toRegex())) {
                    val num = line.substring(2)
                    num.toInt() + 1
                } else 0
            }
        }.getOrDefault(0)
}
