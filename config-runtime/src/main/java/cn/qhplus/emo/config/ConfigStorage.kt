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

package cn.qhplus.emo.config

interface ConfigStorage {
    fun readBool(meta: ConfigMeta, default: Boolean): Boolean
    fun writeBool(meta: ConfigMeta, value: Boolean)
    fun readInt(meta: ConfigMeta, default: Int): Int
    fun writeInt(meta: ConfigMeta, value: Int)
    fun readLong(meta: ConfigMeta, default: Long): Long
    fun writeLong(meta: ConfigMeta, value: Long)
    fun readFloat(meta: ConfigMeta, default: Float): Float
    fun writeFloat(meta: ConfigMeta, value: Float)
    fun readDouble(meta: ConfigMeta, default: Double): Double
    fun writeDouble(meta: ConfigMeta, value: Double)
    fun readString(meta: ConfigMeta, default: String): String
    fun writeString(meta: ConfigMeta, value: String)
    fun remove(meta: ConfigMeta)
    fun remove(metas: List<ConfigMeta>)
    fun clearUp(exclude: List<ConfigMeta>)
    fun flush()
}
