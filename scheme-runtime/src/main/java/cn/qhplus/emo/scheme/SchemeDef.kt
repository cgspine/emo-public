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

package cn.qhplus.emo.scheme

import kotlin.reflect.KClass

data class SchemeArgDefine<T : Any>(
    val name: String,
    val special: Boolean,
    val parser: SchemeArgParser<T>,
    val default: T
)

data class SchemeDef(
    val id: Int,
    val action: String,
    val alternativeHosts: List<KClass<*>>,
    val args: List<SchemeArgDefine<*>>,
    val targetId: String,
    val transition: Int
) {

    companion object {
        const val COMPOSE_CLASS_SUFFIX = "_ComposeBuilder"
    }

    val parserMap by lazy {
        val map = mutableMapOf<String, SchemeArgParser<*>>()
        args.forEach {
            map[it.name] = it.parser
        }
        map.toMap()
    }

    fun match(schemeParts: SchemeParts): Boolean {
        return schemeParts.action == action &&
            matchSpecialArgs(schemeParts)
    }

    internal fun matchSpecialArgs(schemeParts: SchemeParts): Boolean {
        return args.asSequence()
            .filter { it.special }
            .all { def ->
                schemeParts.queries[def.name].let {
                    it != null && def.parser.parse(def.name, it) == def.default
                }
            }
    }
}

interface SchemeDefStorage {
    fun find(schemeParts: SchemeParts): SchemeDef?
    fun findById(id: Int): SchemeDef?
}

abstract class AbstractSchemeDefStorage : SchemeDefStorage {
    private val map = mutableMapOf<String, MutableList<SchemeDef>>()
    private val mapById = mutableMapOf<Int, SchemeDef>()

    protected fun add(schemeDef: SchemeDef) {
        mapById[schemeDef.id] = schemeDef
        val exist = map[schemeDef.action]
        if (exist != null) {
            exist.add(schemeDef)
        } else {
            map[schemeDef.action] = mutableListOf(schemeDef)
        }
    }

    override fun find(schemeParts: SchemeParts): SchemeDef? {
        val subMap = map.entries.find { schemeParts.action == it.key }?.value ?: return null
        return subMap.find { it.matchSpecialArgs(schemeParts) }
    }

    override fun findById(id: Int): SchemeDef? {
        return mapById[id]
    }
}

object DummySchemeDefStorage : SchemeDefStorage {
    override fun find(schemeParts: SchemeParts): SchemeDef? {
        return null
    }

    override fun findById(id: Int): SchemeDef? {
        return null
    }
}

object GeneratedSchemeDefStorageDelegate : SchemeDefStorage {
    private val storage by lazy {
        kotlin.runCatching {
            val cls = Class.forName("cn.qhplus.emo.scheme.impl.GeneratedSchemeDefStorage")
            cls.getConstructor().newInstance() as SchemeDefStorage
        }.getOrDefault(DummySchemeDefStorage)
    }

    override fun find(schemeParts: SchemeParts): SchemeDef? {
        return storage.find(schemeParts)
    }

    override fun findById(id: Int): SchemeDef? {
        return storage.findById(id)
    }
}
