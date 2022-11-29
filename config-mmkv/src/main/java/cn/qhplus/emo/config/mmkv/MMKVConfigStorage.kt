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

import cn.qhplus.emo.config.ConfigMeta
import cn.qhplus.emo.config.ConfigStorage
import com.tencent.mmkv.MMKV

class MMKVConfigStorage(
    version: Int,
    name: String = "emo-cfg-0",
    multiProcess: Boolean = false
) : ConfigStorage {

    private val kv = MMKV.mmkvWithID(name, if (multiProcess) MMKV.MULTI_PROCESS_MODE else MMKV.SINGLE_PROCESS_MODE)

    private val versionRelatedKeyPrefix = "$version-"
    private val nonVersionRelatedKeyPrefix = "forever-"

    private fun ConfigMeta.buildKey(): String {
        return if (versionRelated) {
            "$versionRelatedKeyPrefix$name"
        } else {
            "$nonVersionRelatedKeyPrefix$name"
        }
    }

    override fun readBool(meta: ConfigMeta, default: Boolean): Boolean {
        return kv.decodeBool(meta.buildKey(), default)
    }

    override fun writeBool(meta: ConfigMeta, value: Boolean) {
        kv.encode(meta.buildKey(), value)
    }

    override fun readInt(meta: ConfigMeta, default: Int): Int {
        return kv.decodeInt(meta.buildKey(), default)
    }

    override fun writeInt(meta: ConfigMeta, value: Int) {
        kv.encode(meta.buildKey(), value)
    }

    override fun readLong(meta: ConfigMeta, default: Long): Long {
        return kv.decodeLong(meta.buildKey(), default)
    }

    override fun writeLong(meta: ConfigMeta, value: Long) {
        kv.encode(meta.buildKey(), value)
    }

    override fun readFloat(meta: ConfigMeta, default: Float): Float {
        return kv.decodeFloat(meta.buildKey(), default)
    }

    override fun writeFloat(meta: ConfigMeta, value: Float) {
        kv.encode(meta.buildKey(), value)
    }

    override fun readDouble(meta: ConfigMeta, default: Double): Double {
        return kv.decodeDouble(meta.buildKey(), default)
    }

    override fun writeDouble(meta: ConfigMeta, value: Double) {
        kv.encode(meta.buildKey(), value)
    }

    override fun readString(meta: ConfigMeta, default: String): String {
        return kv.decodeString(meta.buildKey(), default) ?: ""
    }

    override fun writeString(meta: ConfigMeta, value: String) {
        kv.encode(meta.buildKey(), value)
    }

    override fun remove(meta: ConfigMeta) {
        kv.removeValueForKey(meta.buildKey())
    }

    override fun remove(metas: List<ConfigMeta>) {
        kv.removeValuesForKeys(metas.map { it.buildKey() }.toTypedArray())
    }

    override fun clearUp(exclude: List<ConfigMeta>) {
        val excludeKeys = exclude.map { it.buildKey() }
        kv.allKeys()
            ?.filter { !excludeKeys.contains(it) }
            ?.let {
                if (it.isNotEmpty()) {
                    kv.removeValuesForKeys(it.toTypedArray())
                }
            }
    }

    override fun flush() {
        kv.sync()
    }
}
