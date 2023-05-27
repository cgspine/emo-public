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

package cn.qhplus.emo.scheme.impl

import android.app.Application
import cn.qhplus.emo.scheme.CoreSchemeHandler
import cn.qhplus.emo.scheme.GeneratedSchemeDefStorageDelegate
import cn.qhplus.emo.scheme.InterceptorSchemeHandler
import cn.qhplus.emo.scheme.SchemeClient
import cn.qhplus.emo.scheme.SchemeDefStorage
import cn.qhplus.emo.scheme.SchemeHandler
import cn.qhplus.emo.scheme.SchemeInterceptor


class SchemeClientBuilder(val application: Application) {

    private val interceptors = mutableListOf<SchemeInterceptor>()
    var storage: SchemeDefStorage = GeneratedSchemeDefStorageDelegate
    var blockSameSchemeTimeout = 500L
    var debug: Boolean = false
       fun addInterceptor(interceptor: SchemeInterceptor): SchemeClientBuilder {
        interceptors.add(interceptor)
        return this
    }

    fun build(): SchemeClient {
        val handler = interceptors.asReversed().fold<_, SchemeHandler>(CoreSchemeHandler) { acc, schemeInterceptor ->
            InterceptorSchemeHandler(acc, schemeInterceptor)
        }
        return SchemeClient(
            blockSameSchemeTimeout,
            storage,
            debug,
            handler,
            AndroidSchemeExecTransactionFactory(application){
                SchemeTransitionProviders.get(it)
            }
        )
    }
}

fun schemeClient(application: Application, block: SchemeClientBuilder.() -> Unit = {}): SchemeClient {
    val builder = SchemeClientBuilder(application)
    block(builder)
    return builder.build()
}
