package cn.qhplus.emo

import cn.qhplus.emo.scheme.QueryFormat
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Assert
import org.junit.Test

@Serializable
data class Data(
    val b: Boolean,
    val i: Int = 80,
    val l: Long,
    val f: Float,
    val str: String
)

@Serializable
data class Data2(
    val b: Boolean,
    val i: Int = 80,
    val l: Long? = null,
    val str: String? = null
)

class QueryFormatTest {
    @Test
    fun encodeTest() {
        val data = Data(true, 100, 1000, 3.14f, "haha")
        val ret = QueryFormat.encodeToString(data)
        Assert.assertEquals("b=0&i=100&l=1000&f=3.14&str=haha", ret)
    }

    @Test
    fun encodeTestWithNull() {
        val data = Data2(false, str = "hehe")
        val ret = QueryFormat.encodeToString(data)
        Assert.assertEquals("b=0&i=80&str=hehe", ret)
    }

    @Test
    fun decodeTest() {
        val text = "b=1&i=101&l=1001&f=3.1415&str=hehe"
        val ret = QueryFormat.decodeFromString<Data>(text)
        Assert.assertEquals(Data(true, 101, 1001, 3.1415f, "hehe"), ret)
    }

    @Test
    fun decodeTestWithDefault() {
        val text = "b=1&l=1001&f=3.1415&str=hehe"
        val ret = QueryFormat.decodeFromString<Data>(text)
        Assert.assertEquals(Data(true, 80, 1001, 3.1415f, "hehe"), ret)
    }

    @Test
    fun decodeTestWithNull() {
        val text = "b=1&str=xixi&l=null"
        val ret = QueryFormat.decodeFromString<Data2>(text)
        Assert.assertEquals(Data2(true, str = "xixi"), ret)
    }
}