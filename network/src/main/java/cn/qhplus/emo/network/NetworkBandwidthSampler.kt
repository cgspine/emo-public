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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

data class NetworkStreamTotal(
    val id: Int,
    val down: Long,
    val up: Long,
    val timestamp: Long
) {
    companion object {
        val ZERO = NetworkStreamTotal(0, 0, 0, 0)
    }
}

data class NetworkBandwidth(val down: Double, val up: Double) {
    companion object {
        val UNDEFINED = NetworkBandwidth(0.0, 0.0)
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
    private val samplingTaskId = AtomicInteger(0)

    private val downStreamBandwidth = ExponentialMovingAverage(DEFAULT_DECAY)
    private val upStreamBandwidth = ExponentialMovingAverage(DEFAULT_DECAY)

    private val _streamTotalFlow = MutableStateFlow(NetworkStreamTotal.ZERO)
    private val _bandwidthFlow = MutableStateFlow(NetworkBandwidth.UNDEFINED)

    var sampleInterval: Long = 2000

    val streamTotalFlow = _streamTotalFlow.asStateFlow()
    val bandwidthFlow = _bandwidthFlow.asStateFlow()

    fun startSampling() {
        if (samplingCounter.getAndIncrement() == 0) {
            val sampleId = samplingTaskId.incrementAndGet()
            scope.launch {
                var previousRxBytes = -1L
                var previousTxBytes = -1L
                var lastReadingTime = -1L
                var historyRxBytes = -1L
                var historyTxBytes = -1L
                while (isActive && samplingTaskId.get() == sampleId) {
                    val rx = TrafficStats.getUidRxBytes(applicationContext.applicationInfo.uid)
                    val tx = TrafficStats.getUidTxBytes(applicationContext.applicationInfo.uid)
                    val curTimeReading = SystemClock.elapsedRealtime()
                    if (historyRxBytes < 0) {
                        historyRxBytes = rx
                        historyTxBytes = tx
                    } else {
                        _streamTotalFlow.value = NetworkStreamTotal(sampleId, rx - historyRxBytes, tx - historyTxBytes, curTimeReading)
                    }

                    if (previousRxBytes >= 0) {
                        val rxDiff = rx - previousRxBytes
                        val txDiff = tx - previousTxBytes
                        val timeDiff = curTimeReading - lastReadingTime
                        recordBandwidth(downStreamBandwidth, rxDiff, timeDiff)
                        recordBandwidth(upStreamBandwidth, txDiff, timeDiff)
                        val down = downStreamBandwidth.getAverage()
                        val up = upStreamBandwidth.getAverage()
                        if (down >= 0 || up >= 0) {
                            _bandwidthFlow.value = NetworkBandwidth(
                                down.coerceAtLeast(0.0),
                                up.coerceAtLeast(0.0)
                            )
                        }
                    }
                    lastReadingTime = curTimeReading
                    previousRxBytes = rx
                    previousTxBytes = tx
                    delay(sampleInterval)
                }
            }
        }
    }

    fun stopSampling() {
        if (samplingCounter.decrementAndGet() == 0) {
            val current = samplingTaskId.get()
            samplingTaskId.compareAndSet(current, current + 1)
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
