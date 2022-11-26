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
    fun readBool(name: String, versionRelated: Boolean, default: Boolean): Boolean
    fun writeBool(name: String, versionRelated: Boolean, value: Boolean)
    fun readInt(name: String, versionRelated: Boolean, default: Int): Int
    fun writeInt(name: String, versionRelated: Boolean, value: Int)
    fun readLong(name: String, versionRelated: Boolean, default: Long): Long
    fun writeLong(name: String, versionRelated: Boolean, value: Long)
    fun readFloat(name: String, versionRelated: Boolean, default: Float): Float
    fun writeFloat(name: String, versionRelated: Boolean, value: Float)
    fun readDouble(name: String, versionRelated: Boolean, default: Double): Double
    fun writeDouble(name: String, versionRelated: Boolean, value: Double)
    fun readString(name: String, versionRelated: Boolean, default: String): String
    fun writeString(name: String, versionRelated: Boolean, value: String)
    fun remove(name: String, versionRelated: Boolean)
    fun flush()
}
