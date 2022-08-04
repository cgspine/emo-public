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

package cn.qhplus.emo.network

import android.content.Context
import android.net.TrafficStats
import android.os.SystemClock
import cn.qhplus.emo.core.EmoLog
import cn.qhplus.emo.core.LogTag
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

data class NetworkStreamTotal(val down: Long, val up: Long) {
    companion object {
        val ZERO = NetworkStreamTotal(0, 0)
    }
}

data class NetworkBandwidth(val down: Double, val up: Double) {
    companion object {
        val UNDEFINED = NetworkBandwidth(-1.0, -1.0)
    }
}

class NetworkBandwidthSampler private constructor(
    private val applicationContext: Context
) : LogTag {
    companion object {
        private const val DEFAULT_DECAY = 0.2F
        private const val BITS_PER_BYTE = 8
        private const val BANDWIDTH_LOWER_BOUND = 10.0 // kbps

        @Volatile
        private var instance: NetworkBandwidthSampler? = null

        @Synchronized
        fun of(context: Context): NetworkBandwidthSampler {
            return instance ?: NetworkBandwidthSampler(context.applicationContext).also {
                instance = it
            }
        }
    }

    private val scopeExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        EmoLog.e(TAG, "scope exception error", throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + scopeExceptionHandler)
    private val samplingCounter = AtomicInteger(0)
    private var samplingJob: Job? = null

    private val downStreamBandwidth = ExponentialMovingAverage(DEFAULT_DECAY)
    private val upStreamBandwidth = ExponentialMovingAverage(DEFAULT_DECAY)

    private val _streamTotalFlow = MutableStateFlow(NetworkStreamTotal.ZERO)
    private val _bandwidthFlow = MutableStateFlow(NetworkBandwidth.UNDEFINED)

    var sampleInterval: Long = 2000

    val streamTotalFlow = _streamTotalFlow.asStateFlow()
    val bandwidthFlow = _bandwidthFlow.asStateFlow()

    fun startSampling() {
        if (samplingCounter.getAndDecrement() == 0) {
            samplingJob = scope.launch {
                var previousRxBytes = -1L
                var previousTxBytes = -1L
                var lastReadingTime = -1L
                while (isActive) {
                    val rx = TrafficStats.getUidRxBytes(applicationContext.applicationInfo.uid)
                    val tx = TrafficStats.getUidTxBytes(applicationContext.applicationInfo.uid)
                    _streamTotalFlow.value = NetworkStreamTotal(rx, tx)
                    lastReadingTime = if (previousRxBytes >= 0) {
                        val rxDiff = rx - previousRxBytes
                        val txDiff = tx - previousTxBytes
                        val curTimeReading = SystemClock.elapsedRealtime()
                        val timeDiff = curTimeReading - lastReadingTime
                        recordBandwidth(downStreamBandwidth, rxDiff, timeDiff)
                        recordBandwidth(upStreamBandwidth, txDiff, timeDiff)
                        _bandwidthFlow.value = NetworkBandwidth(downStreamBandwidth.getAverage(), upStreamBandwidth.getAverage())
                        curTimeReading
                    } else {
                        SystemClock.elapsedRealtime()
                    }
                    previousRxBytes = rx
                    previousTxBytes = tx
                    delay(sampleInterval)
                }
            }
        }
    }

    fun stopSampling() {
        // create a local variable point to the current samplingJob to avoid race condition.
        val job = samplingJob
        if (samplingCounter.decrementAndGet() == 0) {
            job?.cancel()
            upStreamBandwidth.reset()
            downStreamBandwidth.reset()
        }
    }

    @Synchronized
    private fun recordBandwidth(
        bandwidth: ExponentialMovingAverage,
        bytes: Long,
        timeInMs: Long
    ) {
        if (timeInMs == 0L) {
            return
        }
        val measurement = bytes * 1.0 * BITS_PER_BYTE / timeInMs
        if (measurement < BANDWIDTH_LOWER_BOUND) {
            return
        }
        bandwidth.addMeasurement(measurement)
    }
}
