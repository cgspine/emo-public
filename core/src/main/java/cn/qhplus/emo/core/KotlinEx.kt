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

package cn.qhplus.emo.core

import java.io.Closeable

inline fun <T> T.runIf(condition: Boolean, block: T.() -> T): T = if (condition) block() else this

inline fun <T> List<T>.runIfNotEmpty(block: List<T>.() -> Unit) {
    if (isNotEmpty()) {
        block()
    }
}

fun Closeable.closeQuietly() {
    try {
        close()
    } catch (ignore: Throwable) {
    }
}
