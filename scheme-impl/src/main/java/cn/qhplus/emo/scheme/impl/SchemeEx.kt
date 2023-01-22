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

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import cn.qhplus.emo.scheme.Scheme
import cn.qhplus.emo.scheme.SchemeDef

private val schemeDefToRouteCache = mutableMapOf<SchemeDef, String>()
private val schemeDefToNavArgCache = mutableMapOf<SchemeDef, List<NamedNavArgument>>()

fun SchemeDef.toComposeRouteDefine(): String {
    val cache = schemeDefToRouteCache[this]
    if (cache != null) {
        return cache
    }
    val builder = StringBuilder("/").append(action)
    val group = args.groupBy { it.special }
    val path = group.getOrDefault(true, emptyList()).sortedBy { it.name }.joinToString("/") { "${it.name}_${it.default}" }
    if (path.isNotBlank()) {
        builder.append("/")
        builder.append(path)
    }
    val query = group.getOrDefault(false, emptyList()).sortedBy { it.name }.joinToString("&") { "${it.name}={${it.name}}" }
    builder.append("?")
    if (query.isNotBlank()) {
        builder.append(query)
        builder.append("&")
    }
    builder.append(SchemeKeys.KEY_ORIGIN)
    builder.append("={")
    builder.append(SchemeKeys.KEY_ORIGIN)
    builder.append("}")
    return builder.toString().also {
        schemeDefToRouteCache[this] = it
    }
}

fun SchemeDef.toComposeNavArg(): List<NamedNavArgument> {
    val cache = schemeDefToNavArgCache[this]
    if (cache != null) {
        return cache
    }
    val ret = args.filter { !it.special }.map {
        navArgument(it.name) {
            type = when (it.default::class.java) {
                Int::class.java, java.lang.Integer::class.java -> NavType.IntType
                Boolean::class.java, java.lang.Boolean::class.java -> NavType.BoolType
                Long::class.java, java.lang.Long::class.java -> NavType.LongType
                Float::class.java, java.lang.Float::class.java -> NavType.FloatType
                else -> NavType.StringType
            }
            defaultValue = it.default
        }
    }.toMutableList()
    ret.add(
        navArgument(SchemeKeys.KEY_ORIGIN) {
            type = NavType.StringType
            defaultValue = ""
        }
    )
    return ret.also {
        schemeDefToNavArgCache[this] = it
    }
}

fun Scheme.toComposeRouteValue(): String {
    val builder = StringBuilder("/").append(action)
    val special = def.args.asSequence().filter { it.special }.sortedBy { it.name }
    val path = special.sortedBy { it.name }.joinToString("/") { "${it.name}_${it.default}" }
    if (path.isNotBlank()) {
        builder.append("/")
        builder.append(path)
    }
    val query = args.entries.filter { entity -> special.find { it.name == entity.key } == null }.joinToString("&") { "${it.key}=${it.value}" }
    builder.append("?")
    if (query.isNotBlank()) {
        builder.append(query)
        builder.append("&")
    }
    builder.append(SchemeKeys.KEY_ORIGIN)
    builder.append("=")
    builder.append(Uri.encode(origin))
    return builder.toString()
}

fun NavGraphBuilder.getStartDestinationArgsFromBundle(bundle: Bundle) {
    bundle.keySet().forEach { name ->
        bundle.get(name)?.let {
            argument(name) {
                type = when (it.javaClass) {
                    Int::class.java, java.lang.Integer::class.java -> NavType.IntType
                    Boolean::class.java, java.lang.Boolean::class.java -> NavType.BoolType
                    Long::class.java, java.lang.Long::class.java -> NavType.LongType
                    Float::class.java, java.lang.Float::class.java -> NavType.FloatType
                    else -> NavType.StringType
                }
                defaultValue = it
            }
        }
    }
}
