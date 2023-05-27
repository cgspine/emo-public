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

const val SCHEME_ARG_INTENT_FLAG = "__emo_intent_flag__"
const val SCHEME_ARG_FORCE_NEW_HOST = "__emo_force_new_host__"
const val SCHEME_ARG_BAD = "__emo_bad__"

data class Scheme(
    val def: SchemeDef,
    val protocol: String,
    val action: String,
    val args: Map<String, Any>,
    val origin: String
) {

    fun getIntentFlag(): Int {
        val v = args[SCHEME_ARG_INTENT_FLAG] ?: return 0
        if (v is Int && v > 0) {
            return v
        }
        if (v is String) {
            try {
                val value = SchemeIntArgParser.parse("", v)
                if (value > 0) {
                    return value
                }
            } catch (ignore: Throwable) {
            }
        }
        return 0
    }

    fun forceNewHost(): Boolean {
        return checkBoolValue(SCHEME_ARG_FORCE_NEW_HOST)
    }

    fun isBad(): Boolean {
        return checkBoolValue(SCHEME_ARG_BAD)
    }

    private fun checkBoolValue(name: String): Boolean {
        val v = args[name] ?: return false
        return when (v) {
            is Boolean -> v
            is Int -> v > 0
            is String -> v == "1" || v.lowercase() == "true"
            else -> false
        }
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

