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

import cn.qhplus.emo.config.panel.ConfigImplDisplayable

@ConfigBasic(
    category = "action",
    name = "test_int",
    humanName = "测试 Int",
    versionRelated = true,
    tags = ["FromServer"]
)
@ConfigWithIntValue(default = 1)
sealed interface ConfigTestInt

@ConfigBasic(
    category = "action",
    name = "test_bool",
    humanName = "测试 Bool",
    versionRelated = true
)
@ConfigWithBoolValue(default = false)
sealed interface ConfigTestBool

@ConfigBasic(
    category = "action",
    name = "test_long",
    humanName = "测试 Long",
    versionRelated = true
)
@ConfigWithLongValue(default = 2)
sealed interface ConfigTestLong

@ConfigBasic(
    category = "action",
    name = "test_float",
    humanName = "测试 Float",
    versionRelated = true
)
@ConfigWithFloatValue(default = 4.0f)
sealed interface ConfigTestFloat

@ConfigBasic(
    category = "action",
    name = "test_double",
    humanName = "测试 Double",
    versionRelated = true
)
@ConfigWithDoubleValue(default = 7.5)
sealed interface ConfigTestDouble

@ConfigBasic(
    category = "action",
    name = "test_string",
    humanName = "测试 String",
    versionRelated = true
)
@ConfigWithStringValue(default = "hello")
sealed interface ConfigTestString

@ConfigBasic(
    category = "implementation",
    name = "test_impl_int",
    humanName = "测试 Int 实现类",
    versionRelated = true
)
@ConfigWithIntValue(default = 1)
sealed interface ConfigTestImplInt {
    fun getName(): String
}

@ConfigWithIntValue(default = 1)
object ConfigImplIntA : ConfigTestImplInt {
    override fun getName(): String {
        return "ConfigImplIntA"
    }
}

@ConfigWithIntValue(default = 2)
object ConfigImplIntB : ConfigTestImplInt {
    override fun getName(): String {
        return "ConfigImplIntB"
    }
}

@ConfigBasic(
    category = "implementation",
    name = "test_domain",
    humanName = "请求环境",
    versionRelated = true
)
@ConfigWithIntValue(default = 1)
sealed interface ConfigTestImplDisplay : ConfigImplDisplayable {
    fun getHost(): String
}

@ConfigWithIntValue(default = 1)
object ConfigTestImplDisplayA : ConfigTestImplDisplay {

    override fun displayName(): String {
        return "现网环境"
    }

    override fun getHost(): String {
        return "prod.qhplus.cn"
    }
}

@ConfigWithIntValue(default = 2)
object ConfigTestImplDisplayB : ConfigTestImplDisplay {

    override fun displayName(): String {
        return "开发环境"
    }

    override fun getHost(): String {
        return "dev.qhplus.cn"
    }
}

@ConfigWithIntValue(default = 3)
object ConfigTestImplDisplayC : ConfigTestImplDisplay {

    override fun displayName(): String {
        return "测试环境"
    }

    override fun getHost(): String {
        return "test.qhplus.cn"
    }
}

@ConfigBasic(
    category = "implementation",
    name = "test_impl_bool",
    humanName = "测试 Bool 实现类",
    versionRelated = true
)
@ConfigWithBoolValue(default = false)
sealed interface ConfigTestImplBool {
    fun getName(): String
}

@ConfigWithBoolValue(default = false)
object ConfigImplBoolA : ConfigTestImplBool {
    override fun getName(): String {
        return "ConfigImplBoolA"
    }
}

@ConfigWithBoolValue(default = true)
object ConfigImplBoolB : ConfigTestImplBool {
    override fun getName(): String {
        return "ConfigImplBoolB"
    }
}

@ConfigBasic(
    category = "implementation",
    name = "test_impl_long",
    humanName = "测试 Long 实现类",
    versionRelated = true
)
@ConfigWithLongValue(default = 1)
sealed interface ConfigTestImplLong {
    fun getName(): String
}

@ConfigWithLongValue(default = 1)
object ConfigImplLongA : ConfigTestImplLong {
    override fun getName(): String {
        return "ConfigImplLongA"
    }
}

@ConfigWithLongValue(default = 2)
object ConfigImplLongB : ConfigTestImplLong {
    override fun getName(): String {
        return "ConfigImplLongB"
    }
}

@ConfigBasic(
    category = "implementation",
    name = "test_impl_float",
    humanName = "测试 Float 实现类",
    versionRelated = true
)
@ConfigWithFloatValue(default = 1.0f)
sealed interface ConfigTestImplFloat {
    fun getName(): String
}

@ConfigWithFloatValue(default = 1.0f)
object ConfigTestImplFloatA : ConfigTestImplFloat {
    override fun getName(): String {
        return "ConfigTestImplFloatA"
    }
}

@ConfigWithFloatValue(default = 1.1f)
object ConfigTestImplFloatB : ConfigTestImplFloat {
    override fun getName(): String {
        return "ConfigTestImplFloatB"
    }
}

@ConfigBasic(
    category = "implementation",
    name = "test_impl_double",
    humanName = "测试 Double 实现类",
    versionRelated = true
)
@ConfigWithDoubleValue(default = 1.0)
sealed interface ConfigTestImplDouble {
    fun getName(): String
}

@ConfigWithDoubleValue(default = 1.0)
object ConfigTestImplDoubleA : ConfigTestImplDouble {
    override fun getName(): String {
        return "ConfigTestImplDoubleA"
    }
}

@ConfigWithDoubleValue(default = 2.0)
object ConfigTestImplDoubleB : ConfigTestImplDouble {
    override fun getName(): String {
        return "ConfigTestImplDoubleB"
    }
}

@ConfigBasic(
    category = "implementation",
    name = "test_impl_string",
    humanName = "测试 String 实现类",
    versionRelated = true
)
@ConfigWithStringValue(default = "a")
sealed interface ConfigTestImplString {
    fun getName(): String
}

@ConfigWithStringValue(default = "a")
object ConfigTestImplStringA : ConfigTestImplString {
    override fun getName(): String {
        return "ConfigTestImplStringA"
    }
}

@ConfigWithStringValue(default = "b")
object ConfigTestImplStringB : ConfigTestImplString {
    override fun getName(): String {
        return "ConfigTestImplStringB"
    }
}
