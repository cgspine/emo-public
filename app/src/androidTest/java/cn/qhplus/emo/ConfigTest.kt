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

package cn.qhplus.emo

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cn.qhplus.emo.config.ConfigCenter
import cn.qhplus.emo.config.ConfigTestBool
import cn.qhplus.emo.config.ConfigTestDouble
import cn.qhplus.emo.config.ConfigTestFloat
import cn.qhplus.emo.config.ConfigTestImplBool
import cn.qhplus.emo.config.ConfigTestImplDouble
import cn.qhplus.emo.config.ConfigTestImplFloat
import cn.qhplus.emo.config.ConfigTestImplInt
import cn.qhplus.emo.config.ConfigTestImplLong
import cn.qhplus.emo.config.ConfigTestImplString
import cn.qhplus.emo.config.ConfigTestInt
import cn.qhplus.emo.config.ConfigTestLong
import cn.qhplus.emo.config.ConfigTestString
import cn.qhplus.emo.config.actionOf
import cn.qhplus.emo.config.concreteBool
import cn.qhplus.emo.config.concreteDouble
import cn.qhplus.emo.config.concreteFloat
import cn.qhplus.emo.config.concreteInt
import cn.qhplus.emo.config.concreteLong
import cn.qhplus.emo.config.concreteString
import cn.qhplus.emo.config.implOf
import cn.qhplus.emo.config.mmkv.MMKVConfigStorage
import cn.qhplus.emo.config.mmkv.configCenterWithMMKV
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */

private const val TAG = "ConfigTest"

@RunWith(AndroidJUnit4::class)
class ConfigTest {
    @Test
    fun config_action_test() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        MMKV.initialize(appContext)
        val storage = MMKVConfigStorage(System.currentTimeMillis().toInt())
        val center = ConfigCenter(storage, false)
        val actionInt = center.actionOf<ConfigTestInt>().concreteInt()
        assert(actionInt.read() == 1)
        actionInt.write(3)
        assert(actionInt.read() == 3)

        val actionBool = center.actionOf<ConfigTestBool>().concreteBool()
        assert(actionBool.read().not())
        actionBool.write(true)
        assert(actionBool.read())

        val actionLong = center.actionOf<ConfigTestLong>().concreteLong()
        assert(actionLong.read() == 2L)
        actionLong.write(5)
        assert(actionLong.read() == 5L)

        val actionFloat = center.actionOf<ConfigTestFloat>().concreteFloat()
        assert(actionFloat.read() == 4.0f)
        actionFloat.write(8.0f)
        assert(actionFloat.read() == 8.0f)

        val actionDouble = center.actionOf<ConfigTestDouble>().concreteDouble()
        assert(actionDouble.read() == 7.5)
        actionDouble.write(11.2)
        assert(actionDouble.read() == 11.2)

        val actionString = center.actionOf<ConfigTestString>().concreteString()
        assert(actionString.read() == "hello")
        actionString.write("world")
        assert(actionString.read() == "world")
    }

    @Test
    fun config_impl_test() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        MMKV.initialize(appContext)
        val storage = MMKVConfigStorage(System.currentTimeMillis().toInt())
        val center = ConfigCenter(storage, false)
        assert(center.implOf<ConfigTestImplInt>()?.getName() == "ConfigImplIntA")
        center.actionOf<ConfigTestImplInt>().concreteInt().write(2)
        assert(center.implOf<ConfigTestImplInt>()?.getName() == "ConfigImplIntB")

        assert(center.implOf<ConfigTestImplBool>()?.getName() == "ConfigImplBoolA")
        center.actionOf<ConfigTestImplBool>().concreteBool().write(true)
        assert(center.implOf<ConfigTestImplBool>()?.getName() == "ConfigImplBoolB")

        assert(center.implOf<ConfigTestImplLong>()?.getName() == "ConfigImplLongA")
        center.actionOf<ConfigTestImplLong>().concreteLong().write(2)
        assert(center.implOf<ConfigTestImplLong>()?.getName() == "ConfigImplLongB")

        assert(center.implOf<ConfigTestImplFloat>()?.getName() == "ConfigTestImplFloatA")
        center.actionOf<ConfigTestImplFloat>().concreteFloat().write(1.1f)
        assert(center.implOf<ConfigTestImplFloat>()?.getName() == "ConfigTestImplFloatB")

        assert(center.implOf<ConfigTestImplDouble>()?.getName() == "ConfigTestImplDoubleA")
        center.actionOf<ConfigTestImplDouble>().concreteDouble().write(2.0)
        assert(center.implOf<ConfigTestImplDouble>()?.getName() == "ConfigTestImplDoubleB")

        assert(center.implOf<ConfigTestImplString>()?.getName() == "ConfigTestImplStringA")
        center.actionOf<ConfigTestImplString>().concreteString().write("b")
        assert(center.implOf<ConfigTestImplString>()?.getName() == "ConfigTestImplStringB")


    }

    @Test
    fun config_remove_test() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        MMKV.initialize(appContext)
        val storage = MMKVConfigStorage(System.currentTimeMillis().toInt())
        val center = ConfigCenter(storage, false)
        val actionInt = center.actionOf<ConfigTestInt>().concreteInt()
        assert(actionInt.read() == 1)
        actionInt.write(3)
        assert(actionInt.read() == 3)
        actionInt.remove()
        assert(actionInt.read() == 1)
    }

    @Test
    fun config_version_test() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        MMKV.initialize(appContext)
        val center = configCenterWithMMKV(System.currentTimeMillis().toInt())
        val actionInt = center.actionOf<ConfigTestInt>().concreteInt()
        assert(actionInt.read() == 1)
        actionInt.write(3)
        assert(actionInt.read() == 3)

        val center1 = configCenterWithMMKV(System.currentTimeMillis().toInt())
        assert(center1.actionOf<ConfigTestInt>().concreteInt().read() == 1)
    }

    @Test
    fun config_write_map_test() = runTest{
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        MMKV.initialize(appContext)
        val center = configCenterWithMMKV(System.currentTimeMillis().toInt())
        var map = mutableMapOf<String, Any>()
        map[center.actionOf<ConfigTestInt>().meta.name] = 100
        map[center.actionOf<ConfigTestBool>().meta.name] = true
        map[center.actionOf<ConfigTestLong>().meta.name] = 1000L
        map[center.actionOf<ConfigTestFloat>().meta.name] = 100.1f
        map[center.actionOf<ConfigTestDouble>().meta.name] = 101.1234
        map[center.actionOf<ConfigTestString>().meta.name] = "haha"
        center.writeMap(map)
        assert(center.actionOf<ConfigTestInt>().concreteInt().read() == 100)
        assert(center.actionOf<ConfigTestBool>().concreteBool().read())
        assert(center.actionOf<ConfigTestLong>().concreteLong().read() == 1000L)
        assert(center.actionOf<ConfigTestFloat>().concreteFloat().read() == 100.1f)
        assert(center.actionOf<ConfigTestDouble>().concreteDouble().read() == 101.1234)
        assert(center.actionOf<ConfigTestString>().concreteString().read() == "haha")

        // compatibility 1
        map = mutableMapOf()
        map[center.actionOf<ConfigTestInt>().meta.name] = "10"
        map[center.actionOf<ConfigTestBool>().meta.name] = "false"
        map[center.actionOf<ConfigTestLong>().meta.name] = "10000"
        map[center.actionOf<ConfigTestFloat>().meta.name] = "1000.1"
        map[center.actionOf<ConfigTestDouble>().meta.name] = "98.123"
        center.writeMap(map)
        assert(center.actionOf<ConfigTestInt>().concreteInt().read() == 10)
        assert(center.actionOf<ConfigTestBool>().concreteBool().read().not())
        assert(center.actionOf<ConfigTestLong>().concreteLong().read() == 10000L)
        assert(center.actionOf<ConfigTestFloat>().concreteFloat().read() == 1000.1f)
        assert(center.actionOf<ConfigTestDouble>().concreteDouble().read() == 98.123)

        // compatibility 2
        map = mutableMapOf()
        map[center.actionOf<ConfigTestBool>().meta.name] = 1
        map[center.actionOf<ConfigTestFloat>().meta.name] = 10.12345
        center.writeMap(map)
        assert(center.actionOf<ConfigTestBool>().concreteBool().read())
        assert(center.actionOf<ConfigTestFloat>().concreteFloat().read() == 10.12345f)

        map = mutableMapOf()
        map["not_exist_key"] = "haha"
        map[center.actionOf<ConfigTestInt>().meta.name] = "haha"
        center.writeMap(map, { name, value ->
            assert(name == "not_exist_key")
            assert(value == "haha")
            false
        }){ _, cgfCls, value, expected, actual ->
            assert(cgfCls == ConfigTestInt::class.java)
            assert(value == "haha")
            assert(expected == Int::class.java)
            assert(actual == String::class.java)
            false
        }

    }
}
