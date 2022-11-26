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

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cn.qhplus.emo.config.ConfigCenter
import cn.qhplus.emo.config.ConfigStorage
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
        val storage = SpConfigStorage(appContext, System.currentTimeMillis())
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

        // sp not support double, it's not safe because it's converted from float.
//        val actionDouble = center.actionOf<ConfigTestDouble>().concreteDouble()
//        assert(actionDouble.read() == 7.5)
//        actionDouble.write(11.2)
//        assert(actionDouble.read() == 11.2)

        val actionString = center.actionOf<ConfigTestString>().concreteString()
        assert(actionString.read() == "hello")
        actionString.write("world")
        assert(actionString.read() == "world")
    }

    @Test
    fun config_impl_test() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val storage = SpConfigStorage(appContext, System.currentTimeMillis())
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
}


class SpConfigStorage(val context: Context, val version: Long) : ConfigStorage {

    private val sp = context.getSharedPreferences("config", Context.MODE_PRIVATE)

    init {
        //TODO should clear old version values/
    }

    private fun buildKey(name: String, versionRelated: Boolean): String {
        return if (versionRelated) {
            "$version-$name"
        } else {
            "forever-$name"
        }
    }

    override fun readBool(name: String, versionRelated: Boolean, default: Boolean): Boolean {
        return sp.getBoolean(buildKey(name, versionRelated), default)
    }

    override fun writeBool(name: String, versionRelated: Boolean, value: Boolean) {
        sp.edit().putBoolean(buildKey(name, versionRelated), value).apply()
    }

    override fun readInt(name: String, versionRelated: Boolean, default: Int): Int {
        return sp.getInt(buildKey(name, versionRelated), default)
    }

    override fun writeInt(name: String, versionRelated: Boolean, value: Int) {
        sp.edit().putInt(buildKey(name, versionRelated), value).apply()
    }

    override fun readLong(name: String, versionRelated: Boolean, default: Long): Long {
        return sp.getLong(buildKey(name, versionRelated), default)
    }

    override fun writeLong(name: String, versionRelated: Boolean, value: Long) {
        sp.edit().putLong(buildKey(name, versionRelated), value).apply()
    }

    override fun readFloat(name: String, versionRelated: Boolean, default: Float): Float {
        return sp.getFloat(buildKey(name, versionRelated), default)
    }

    override fun writeFloat(name: String, versionRelated: Boolean, value: Float) {
        sp.edit().putFloat(buildKey(name, versionRelated), value).apply()
    }

    override fun readDouble(name: String, versionRelated: Boolean, default: Double): Double {
        return sp.getFloat(buildKey(name, versionRelated), default.toFloat()).toDouble()
    }

    override fun writeDouble(name: String, versionRelated: Boolean, value: Double) {
        sp.edit().putFloat(buildKey(name, versionRelated), value.toFloat()).apply()
    }

    override fun readString(name: String, versionRelated: Boolean, default: String): String {
        return sp.getString(buildKey(name, versionRelated), default) ?: ""
    }

    override fun writeString(name: String, versionRelated: Boolean, value: String) {
        sp.edit().putString(buildKey(name, versionRelated), value).apply()
    }

    override fun flush() {
        sp.edit().commit()
    }

}
