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

package cn.qhplus.emo

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cn.qhplus.emo.config.mmkv.configCenterWithMMKV
import cn.qhplus.emo.core.EmoConfig
import cn.qhplus.emo.core.EmoLog
import cn.qhplus.emo.core.EmoLogDelegate
import cn.qhplus.emo.network.NetworkBandwidthSampler
import cn.qhplus.emo.report.reportWake
import cn.qhplus.emo.scheme.SchemeClient
import cn.qhplus.emo.scheme.impl.schemeClient
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.launch

val configCenter by lazy {
    configCenterWithMMKV(BuildConfig.VERSION_CODE)
}

lateinit var EmoScheme: SchemeClient
    private set

class EmoApp : Application(), ImageLoaderFactory {

    companion object {
        lateinit var instance: EmoApp
            private set
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        instance = this
    }

    override fun onCreate() {
        super.onCreate()
        EmoConfig.debug = BuildConfig.DEBUG
        EmoScheme = schemeClient(this) {
            debug = BuildConfig.DEBUG
        }
        EmoLog.delegate = object : EmoLogDelegate {
            override fun e(tag: String, msg: String, throwable: Throwable?) {
                Log.e(tag, msg, throwable)
            }

            override fun w(tag: String, msg: String, throwable: Throwable?) {
                Log.w(tag, msg, throwable)
            }

            override fun i(tag: String, msg: String, throwable: Throwable?) {
                Log.i(tag, msg, throwable)
            }

            override fun d(tag: String, msg: String, throwable: Throwable?) {
                Log.d(tag, msg, throwable)
            }
        }
        MMKV.initialize(this)
        NetworkBandwidthSampler.of(this).startSampling()
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            ProcessLifecycleOwner.get().repeatOnLifecycle(Lifecycle.State.STARTED) {
                reportWake("cgspine")
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(applicationContext)
            .crossfade(true)
            .build()
    }
}
