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

data class Scheme(
    val def: SchemeDef,
    val protocol: String,
    val action: String,
    val args: Map<String, Any>,
    val origin: String
) {
    fun toBuilder(): SchemeBuilder {
        return SchemeBuilder(protocol, action).arg(args)
    }
}

data class SchemeParts(
    val protocol: String,
    val action: String,
    val queries: Map<String, String>,
    val origin: String
) {
    fun parse(def: SchemeDef): Scheme {
        val ret = mutableMapOf<String, Any>()
        def.args.forEach {
            ret[it.name] = it.default
        }
        queries.forEach { (key, value) ->
            ret[key] = def.parserMap.getOrDefault(key, SchemeStringArgParser).parse(key, value)
        }
        return Scheme(def, protocol, action, ret.toMap(), origin)
    }
}

class SchemeParseException(msg: String) : RuntimeException(msg)

fun String.parse(): SchemeParts {
    var i = indexOf("://")
    if (i <= 0) {
        throw SchemeParseException("prefix is lost.")
    }
    val protocol = substring(0, i)
    val actionStart = i + 3
    i = indexOf("?", actionStart)
    if (i == 0) {
        throw SchemeParseException("action is lost.")
    }
    if (i < 0) {
        i = length
    }
    val action = substring(actionStart, i)
    val argStart = i + 1
    if (argStart >= length) {
        return SchemeParts(protocol, action, emptyMap(), this)
    }
    val queries = substring(argStart)
        .split("&")
        .asSequence()
        .filter { it.isNotBlank() }
        .map {
            val pair = it.split("=")
            pair[0] to pair.getOrElse(1) { "" }
        }
        .toMap()
    return SchemeParts(protocol, action, queries, this)
}

class SchemeBuilder(val protocol: String, val action: String) {
    private val args = mutableMapOf<String, Any>()

    fun arg(name: String, value: Boolean): SchemeBuilder {
        args[name] = if (value) 1 else 0
        return this
    }

    fun arg(name: String, value: Int): SchemeBuilder {
        args[name] = value
        return this
    }

    fun arg(name: String, value: Long): SchemeBuilder {
        args[name] = value
        return this
    }

    fun arg(name: String, value: Float): SchemeBuilder {
        args[name] = value
        return this
    }

    fun arg(name: String, value: Double): SchemeBuilder {
        args[name] = value
        return this
    }

    fun arg(name: String, value: String): SchemeBuilder {
        args[name] = value
        return this
    }

    internal fun arg(map: Map<String, Any>): SchemeBuilder {
        args.putAll(map)
        return this
    }

    override fun toString(): String {
        return StringBuilder(protocol)
            .append("://")
            .append(action)
            .run {
                if (args.isNotEmpty()) {
                    append("?")
                    var first = true
                    args.forEach { (name, value) ->
                        if (!first) {
                            append("&")
                        }
                        append(name)
                        append("=")
                        append(value)
                        first = false
                    }
                }
                toString()
            }
    }
}
