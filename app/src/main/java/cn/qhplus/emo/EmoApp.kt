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
import cn.qhplus.emo.core.EmoLog
import cn.qhplus.emo.core.EmoLogDelegate
import cn.qhplus.emo.network.NetworkBandwidthSampler
import coil.ImageLoader
import coil.ImageLoaderFactory

class EmoApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        EmoLog.delegate = object: EmoLogDelegate {
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
        NetworkBandwidthSampler.of(this).startSampling()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(applicationContext)
            .crossfade(true)
            .build()
    }
}
