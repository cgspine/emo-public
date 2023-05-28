package cn.qhplus.emo.scheme

import androidx.navigation.NavBackStackEntry
import cn.qhplus.emo.core.EmoConfig
import cn.qhplus.emo.core.EmoLog
import cn.qhplus.emo.scheme.impl.SchemeKeys
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.serializer

class SchemeBuilder(val protocol: String, val action: String) {
    private val args = mutableMapOf<String, Any>()
    private var modelData: String? = null

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

    fun forceNewHost(): SchemeBuilder {
        args[SCHEME_ARG_FORCE_NEW_HOST] = 1
        return this
    }

    fun <T> model(serializer: SerializationStrategy<T>, data: T): SchemeBuilder {
        try {
            modelData = QueryFormat.encodeToString(serializer, data)
        }catch (e: Throwable){
            if(EmoConfig.debug){
                throw e
            }
            EmoLog.e("scheme", "serialize model data failed. data = $data", e)
            args[SCHEME_ARG_BAD] = 1
        }
        return this
    }

    fun flagsIfNewActivity(flags: Int): SchemeBuilder {
        args[SCHEME_ARG_INTENT_FLAG] = flags
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
                if (args.isNotEmpty() || modelData != null) {
                    append("?")
                    modelData?.let {
                        append(it)
                    }
                    var notFirst = false
                    args.forEach { (name, value) ->
                        if(notFirst){
                            append("&")
                        }else{
                            notFirst = true
                        }
                        append(name)
                        append("=")
                        append(value)
                    }

                }
                toString()
            }
    }
}

inline fun <reified T> SchemeBuilder.model(data: T): SchemeBuilder {
    return model(QueryFormat.serializersModule.serializer(), data)
}

fun Scheme.toBuilder(): SchemeBuilder {
    return SchemeBuilder(protocol, action).arg(args)
}

inline fun <reified T> NavBackStackEntry.parseModelData(): T?{
    val origin = arguments?.getString(SchemeKeys.KEY_ORIGIN) ?: return null
    val queryStart = origin.indexOf("?")
    if(queryStart < 0 || queryStart == origin.length - 1){
        return null
    }
    val queries = origin.substring(queryStart + 1)
    return try {
        QueryFormat.decodeFromString<T>(queries)
    }catch (e: Throwable){
        if(EmoConfig.debug){
            throw e
        }
        EmoLog.e("scheme", "parse model data failed. origin = $origin", e)
        null
    }
}

fun NavBackStackEntry.readOriginScheme(): String? {
    return arguments?.getString(SchemeKeys.KEY_ORIGIN)
}

fun NavBackStackEntry.readTransition(): Int {
    return arguments?.getInt(SchemeKeys.KEY_TRANSITION, SchemeTransition.UNDEFINED) ?: SchemeTransition.UNDEFINED
}

fun NavBackStackEntry.readAction(): String? {
    return arguments?.getString(SchemeKeys.KEY_ACTION)
}