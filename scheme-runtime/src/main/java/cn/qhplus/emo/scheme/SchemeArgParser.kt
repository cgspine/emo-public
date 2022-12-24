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

interface SchemeArgParser<T : Any> {
    fun parse(name: String, value: String): T
}

object SchemeBoolArgParser : SchemeArgParser<Boolean> {
    override fun parse(name: String, value: String): Boolean {
        if (value.isEmpty()) {
            return true
        }
        if (value == "1" || value == "true") {
            return true
        }
        if (value == "0" || value == "false") {
            return false
        }

        throw SchemeParseException("value($value) for $name can not convert to Boolean")
    }
}

object SchemeIntArgParser : SchemeArgParser<Int> {
    override fun parse(name: String, value: String): Int {
        return value.toInt()
    }
}

object SchemeLongArgParser : SchemeArgParser<Long> {
    override fun parse(name: String, value: String): Long {
        return value.toLong()
    }
}

object SchemeFloatArgParser : SchemeArgParser<Float> {
    override fun parse(name: String, value: String): Float {
        return value.toFloat()
    }
}

object SchemeStringArgParser : SchemeArgParser<String> {
    override fun parse(name: String, value: String): String {
        return value
    }
}
