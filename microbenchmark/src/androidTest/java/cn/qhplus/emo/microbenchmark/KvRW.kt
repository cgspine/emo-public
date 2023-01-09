package cn.qhplus.emo.microbenchmark

import android.content.Context
import android.util.Log
import cn.qhplus.emo.kv.EmoKV
import com.github.hf.leveldb.LevelDB
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

object KvRW {
    private val testCommonName = "kv3"
    private const val KEY_PREFIX = "key_asdfdfdsafjdsklafj_"
    private const val VALUE_SUFFIX = ".emo 是根据功能独立了非常多的子库，没有统一的引入方式，你需要按需引入。" +
            "在每篇文档的开始，都会给出其具体依赖的引入方式。所以去查看左边的目录，看看有没有自己想要的功能吧。"
    fun createWriteData(count: Int): Map<String, String>{
        return buildMap(count){
            for (i in 0 until count){
                put(
                    "$KEY_PREFIX$i", "$i$VALUE_SUFFIX"

                )
            }
        }
    }

    fun emoKvWrite(context: Context, count: Int){
        val kv = EmoKV(context, "emo-$testCommonName", indexInitSpace = 4096 * 128, crc = false, compress = false)
        createWriteData(count).onEach { (t, u) ->
            kv.put(t, u)
        }
        kv.close()
    }

    fun emoKvReadSingleThread(context: Context, count: Int){
        Mem.logCurrent(context, "emoKvReadSingleThread_s")
        val kv = EmoKV(context, "emo-$testCommonName", indexInitSpace = 4096 * 128, crc = false, compress = false)
        for(i in 0 until count){
            val ret = kv.getString("$KEY_PREFIX$i")
            if(ret != "$i$VALUE_SUFFIX"){
                throw RuntimeException("not matched")
            }
        }
        Mem.logCurrent(context, "emoKvReadSingleThread_e")
        kv.close()
    }

    suspend fun emoKvReadMultiThread(scope: CoroutineScope, context: Context, count: Int){
        val kv = EmoKV(context, "emo-$testCommonName", indexInitSpace = 4096 * 128, crc = false, compress = false)
        buildList<Job>(10){
            for(i in 0 until 10){
                val eachSize = count / 10
                scope.launch {
                    for(j in eachSize * i until eachSize * (i + 1)){
                        val ret = kv.getString("$KEY_PREFIX$j")
                        if(ret != "$j$VALUE_SUFFIX"){
                            throw RuntimeException("not matched")
                        }
                    }
                }.let {
                    add(it)
                }
            }
        }.forEach {
            it.join()
        }
        kv.close()
    }

    fun mmkvWrite(context: Context, count: Int){
        MMKV.initialize(context)
        val kv = MMKV.mmkvWithID("mmkv-$testCommonName")
        createWriteData(count).onEach { (t, u) ->
            kv.putString(t, u)
        }
        kv.close()
    }

    fun mmkvReadSingleThread(context: Context, count: Int){
        Mem.logCurrent(context, "mmkvReadSingleThread_s")
        MMKV.initialize(context)
        val kv = MMKV.mmkvWithID("mmkv-$testCommonName")
        for(i in 0 until count){
            val ret = kv.getString("$KEY_PREFIX$i", "")
            if(ret != "$i$VALUE_SUFFIX"){
                throw RuntimeException("not matched")
            }
        }
        Mem.logCurrent(context, "mmkvReadSingleThread_e")
        kv.close()
    }

    suspend fun mmkvReadMultiThread(scope: CoroutineScope, context: Context, count: Int){
        MMKV.initialize(context)
        val kv = MMKV.mmkvWithID("mmkv-$testCommonName")
        buildList<Job>(10){
            for(i in 0 until 10){
                val eachSize = count / 10
                scope.launch {
                    for(j in eachSize * i until eachSize * (i + 1)){
                        val ret = kv.getString("$KEY_PREFIX$j", "")
                        if(ret != "$j$VALUE_SUFFIX"){
                            throw RuntimeException("not matched")
                        }
                    }
                }.let {
                    add(it)
                }
            }
        }.forEach {
            it.join()
        }
        kv.close()
    }

    fun levelDbWrite(context: Context, count: Int){
        val dir = File(context.filesDir, "leveldb-$testCommonName")
        dir.mkdir()
        val kv = LevelDB.open(dir.path, LevelDB.configure().createIfMissing(true))

        kv.put("leveldb".toByteArray(), "Is awesome!".toByteArray())
        createWriteData(count).onEach { (t, u) ->
            kv.put(t.toByteArray(), u.toByteArray())
        }
        kv.close()
    }

    fun levelDbReadSingleThread(context: Context, count: Int){
        Mem.logCurrent(context, "levelDbReadSingleThread_s")
        val dir = File(context.filesDir, "leveldb-$testCommonName")
        dir.mkdir()
        val kv = LevelDB.open(dir.path, LevelDB.configure().createIfMissing(true))
        for(i in 0 until count){
            val ret = String(kv.get("$KEY_PREFIX$i".toByteArray()))
            if(ret != "$i$VALUE_SUFFIX"){
                throw RuntimeException("not matched")
            }
        }
        Mem.logCurrent(context, "levelDbReadSingleThread_s")
        kv.close()
    }

    suspend fun levelDbReadMultiThread(scope: CoroutineScope, context: Context, count: Int){
        val dir = File(context.filesDir, "leveldb-$testCommonName")
        dir.mkdir()
        val kv = LevelDB.open(dir.path, LevelDB.configure().createIfMissing(true))
        buildList<Job>(10){
            for(i in 0 until 10){
                val eachSize = count / 10
                scope.launch {
                    for(j in eachSize * i until eachSize * (i + 1)){
                        val ret = String(kv.get("$KEY_PREFIX$j".toByteArray()))
                        if(ret != "$j$VALUE_SUFFIX"){
                            throw RuntimeException("not matched")
                        }
                    }
                }.let {
                    add(it)
                }
            }
        }.forEach {
            it.join()
        }
        kv.close()
    }
}