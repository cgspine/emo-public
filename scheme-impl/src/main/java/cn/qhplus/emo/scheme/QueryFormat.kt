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

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
class QueryEncoder(override val serializersModule: SerializersModule) : AbstractEncoder() {

    private val sb = StringBuilder()

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        val elementDescriptor = descriptor.getElementDescriptor(index)
        if (elementDescriptor.kind !is PrimitiveKind) {
            throw RuntimeException("The type(${elementDescriptor.kind}) of ${descriptor.getElementName(index)} is not supported.")
        }
        sb.append(descriptor.getElementName(index))
        sb.append("=")
        return true
    }

    override fun encodeValue(value: Any) {
        throw RuntimeException("The type(${value::class.java.simpleName}) is not supported.")
    }

    override fun encodeNull() {
        val index = sb.lastIndexOf("&")
        if (index <= 0) {
            sb.clear()
        } else if (index < sb.length - 1) {
            sb.delete(index + 1, sb.length)
        }
    }

    override fun encodeBoolean(value: Boolean) {
        sb.append(if (value) 1 else 0)
        sb.append("&")
    }

    override fun encodeInt(value: Int) {
        sb.append(value)
        sb.append("&")
    }

    override fun encodeLong(value: Long) {
        sb.append(value)
        sb.append("&")
    }

    override fun encodeFloat(value: Float) {
        sb.append(value)
        sb.append("&")
    }

    override fun encodeString(value: String) {
        sb.append(value)
        sb.append("&")
    }

    fun getResult(): String {
        return sb.toString().dropLast(1)
    }
}

@OptIn(ExperimentalSerializationApi::class)
class QueryDecoder(
    content: String,
    override val serializersModule: SerializersModule
) : AbstractDecoder() {
    val list = content.split("&")
        .asSequence()
        .map { it.split("=").let { pair -> pair[0] to pair[1] } }
        .toList()

    var cur = -1
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        cur++
        val name = list.getOrNull(cur)?.first ?: return CompositeDecoder.DECODE_DONE
        return descriptor.getElementIndex(name)
    }

    override fun decodeBoolean(): Boolean {
        val pair = list[cur]
        return SchemeBoolArgParser.parse(pair.first, pair.second)
    }

    override fun decodeNotNullMark(): Boolean {
        val pair = list[cur]
        return pair.second != "null"
    }

    override fun decodeInt(): Int {
        val pair = list[cur]
        return SchemeIntArgParser.parse(pair.first, pair.second)
    }

    override fun decodeLong(): Long {
        val pair = list[cur]
        return SchemeLongArgParser.parse(pair.first, pair.second)
    }

    override fun decodeFloat(): Float {
        val pair = list[cur]
        return SchemeFloatArgParser.parse(pair.first, pair.second)
    }

    override fun decodeString(): String {
        val pair = list[cur]
        return SchemeStringArgParser.parse(pair.first, pair.second)
    }
}

object QueryFormat : StringFormat {

    override val serializersModule: SerializersModule = EmptySerializersModule()
    override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
        val encoder = QueryEncoder(serializersModule)
        serializer.serialize(encoder, value)
        return encoder.getResult()
    }

    override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
        val decoder = QueryDecoder(string, serializersModule)
        return deserializer.deserialize(decoder)
    }
}
