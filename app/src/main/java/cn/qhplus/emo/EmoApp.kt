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
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import cn.qhplus.emo.config.actionOfConfigTestBool
import cn.qhplus.emo.config.mmkv.configCenterWithMMKV
import cn.qhplus.emo.core.EmoLog
import cn.qhplus.emo.core.EmoLogDelegate
import cn.qhplus.emo.network.NetworkBandwidthSampler
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

val configCenter by lazy {
    configCenterWithMMKV(BuildConfig.VERSION_CODE)
}

class EmoApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
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
            configCenter.actionOfConfigTestBool().stateFlowOf().collectLatest {
                Toast.makeText(this@EmoApp, "config changed: $it", Toast.LENGTH_SHORT).show()
            }
        }

        ProcessLifecycleOwner.get().lifecycleScope.launchWhenResumed {
            delay(500)
            configCenter.actionOfConfigTestBool().write(true)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(applicationContext)
            .crossfade(true)
            .build()
    }
}
