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

package cn.qhplus.emo.config.mmkv

import cn.qhplus.emo.config.ConfigStorage
import com.tencent.mmkv.MMKV

class MMKVConfigStorage(
    version: Long,
    name: String = "emo-cfg-0",
    multiProcess: Boolean = false
) : ConfigStorage {

    private val kv = MMKV.mmkvWithID(name, if (multiProcess) MMKV.MULTI_PROCESS_MODE else MMKV.SINGLE_PROCESS_MODE)

    private val versionRelatedKeyPrefix = "$version-"
    private val nonVersionRelatedKeyPrefix = "forever-"

    init {
        // clear old version keys.
        kv.allKeys()?.filter {
            !it.startsWith(nonVersionRelatedKeyPrefix) && !it.startsWith(versionRelatedKeyPrefix)
        }?.let {
            if (it.isNotEmpty()) {
                kv.removeValuesForKeys(it.toTypedArray())
            }
        }
    }

    private fun buildKey(name: String, versionRelated: Boolean): String {
        return if (versionRelated) {
            "$versionRelatedKeyPrefix$name"
        } else {
            "$nonVersionRelatedKeyPrefix$name"
        }
    }

    override fun readBool(name: String, versionRelated: Boolean, default: Boolean): Boolean {
        return kv.decodeBool(buildKey(name, versionRelated), default)
    }

    override fun writeBool(name: String, versionRelated: Boolean, value: Boolean) {
        kv.encode(buildKey(name, versionRelated), value)
    }

    override fun readInt(name: String, versionRelated: Boolean, default: Int): Int {
        return kv.decodeInt(buildKey(name, versionRelated), default)
    }

    override fun writeInt(name: String, versionRelated: Boolean, value: Int) {
        kv.encode(buildKey(name, versionRelated), value)
    }

    override fun readLong(name: String, versionRelated: Boolean, default: Long): Long {
        return kv.decodeLong(buildKey(name, versionRelated), default)
    }

    override fun writeLong(name: String, versionRelated: Boolean, value: Long) {
        kv.encode(buildKey(name, versionRelated), value)
    }

    override fun readFloat(name: String, versionRelated: Boolean, default: Float): Float {
        return kv.decodeFloat(buildKey(name, versionRelated), default)
    }

    override fun writeFloat(name: String, versionRelated: Boolean, value: Float) {
        kv.encode(buildKey(name, versionRelated), value)
    }

    override fun readDouble(name: String, versionRelated: Boolean, default: Double): Double {
        return kv.decodeDouble(buildKey(name, versionRelated), default)
    }

    override fun writeDouble(name: String, versionRelated: Boolean, value: Double) {
        kv.encode(buildKey(name, versionRelated), value)
    }

    override fun readString(name: String, versionRelated: Boolean, default: String): String {
        return kv.decodeString(buildKey(name, versionRelated), default) ?: ""
    }

    override fun writeString(name: String, versionRelated: Boolean, value: String) {
        kv.encode(buildKey(name, versionRelated), value)
    }

    override fun remove(name: String, versionRelated: Boolean) {
        kv.removeValueForKey(buildKey(name, versionRelated))
    }

    override fun flush() {
        kv.sync()
    }
}
