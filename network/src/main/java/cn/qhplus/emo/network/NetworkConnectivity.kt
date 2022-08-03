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
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.os.SystemClock
import android.util.Log
import cn.qhplus.emo.core.EmoLog
import cn.qhplus.emo.core.LogTag
import cn.qhplus.emo.core.retry
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NetworkConnectivity private constructor(applicationContext: Context) : LogTag {

    companion object {

        @Volatile
        private var instance: NetworkConnectivity? = null

        @Synchronized
        fun of(context: Context): NetworkConnectivity {
            return instance ?: NetworkConnectivity(context.applicationContext).also {
                instance = it
            }
        }
    }

    private val scopeExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        EmoLog.e(TAG, "scope exception error", throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + scopeExceptionHandler)
    private val networkCallback = ConnectivityCallback()
    private val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _stateFlow = MutableStateFlow(
        kotlin.runCatching { fetchNetworkState() }.getOrDefault(NetworkState.none())
    )

    val stateFlow = _stateFlow.asStateFlow()

    init {
        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } catch (e: Throwable) {
            EmoLog.e(TAG, "registerDefaultNetworkCallback failed", e)
        }
    }

    @Synchronized
    fun getNetworkState(forceRefresh: Boolean = false): NetworkState {
        val lastValue = stateFlow.value
        if (!forceRefresh) {
            return lastValue
        }
        return try {
            fetchNetworkState().also { _stateFlow.value = it }
        } catch (e: Throwable) {
            EmoLog.e(TAG, "fetchNetworkState in getNetworkState failed.", e)
            lastValue
        }
    }

    // Special method for Huawei:
    // If app have been running long time in the background, the network would by stopped by some system,
    // but the NetworkCallback may not be called by the system the app back to the foreground,
    // the value in stateFlow is not right and do not have the chance to update,
    // so we can force set the network to fake type and update value from system after a while.
    //
    fun fakeToConnectedAndRecheckAfter(duration: Long = 5000) {
        try {
            val networkState = fetchNetworkState()
            if (networkState.isConnected) {
                _stateFlow.value = networkState
            } else {
                _stateFlow.value = NetworkState(
                    NetworkType.Fake,
                    false,
                    networkState.uuid,
                    networkState.updateTime
                )
                scope.launch {
                    delay(duration)
                    val currentValue = stateFlow.value
                    if (currentValue.networkType == NetworkType.Fake) {
                        _stateFlow.compareAndSet(currentValue, fetchNetworkState())
                    }
                }
            }
        } catch (e: Throwable) {
            EmoLog.e(TAG, "fakeToConnectedAndRecheckAfter invoke failed.", e)
        }
    }

    private fun fetchNetworkState(): NetworkState {
        val network = connectivityManager.activeNetwork ?: return NetworkState.none()
        val capabilities = retry(2) {
            connectivityManager.getNetworkCapabilities(network)
        } ?: return NetworkState(
            NetworkType.Unknown,
            false,
            "",
            0
        )
        return fetchNetworkStateByCapabilities(capabilities, network.toString())
    }

    private fun fetchNetworkStateByCapabilities(
        networkCapabilities: NetworkCapabilities,
        uuid: String
    ): NetworkState {
        if (!networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return NetworkState.none()
        }

        if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return NetworkState(
                NetworkType.Wifi,
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
                uuid,
                SystemClock.elapsedRealtime()
            )
        }

        if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return NetworkState(
                NetworkType.Cellular,
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
                uuid,
                SystemClock.elapsedRealtime()
            )
        }
        return NetworkState(
            NetworkType.Unknown,
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            uuid,
            SystemClock.elapsedRealtime()
        )
    }

    private inner class ConnectivityCallback : ConnectivityManager.NetworkCallback() {

        private var updateJob: Job? = null

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            updateJob?.cancel()
            updateJob = null
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            updateJob?.cancel()
            updateJob = scope.launch {
                _stateFlow.value =
                    fetchNetworkStateByCapabilities(networkCapabilities, network.toString())
            }
        }

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            super.onLinkPropertiesChanged(network, linkProperties)
            Log.i("cgspine", "1.${linkProperties.domains}, ${linkProperties.dnsServers}, ${linkProperties.interfaceName}")
        }

        override fun onLost(network: Network) {
            updateJob?.cancel()
            updateJob = scope.launch {
                delay(1000)
                _stateFlow.value = NetworkState.none()
            }
        }
    }
}
