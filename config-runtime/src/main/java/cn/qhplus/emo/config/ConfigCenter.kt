package cn.qhplus.emo.config

class ConfigCenter(
    val storage: ConfigStorage,
    val prodMode: Boolean = true
) {

    private val configMap by lazy {
        try {
            val cls = Class.forName(ConfigMapFactory::class.java.name + "Generated")
            val factory = cls.getDeclaredConstructor().newInstance() as ConfigMapFactory
            factory.factory(storage, prodMode)
        } catch (e: Throwable) {
            if (!prodMode) {
                throw e
            }
            ConfigMap(emptyMap(), emptyMap())
        }
    }

    fun <T> actionOf(cls: Class<T>): ConfigAction {
        return configMap.actionMap[cls] ?: throw RuntimeException("${cls.simpleName} is not a config interface")
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> implOf(cls: Class<T>): T? {
        val resolver = configMap.implMap[cls] ?: return null
        return resolver.resolve() as T?
    }
}

inline fun <reified T : Any> ConfigCenter.actionOf(): ConfigAction {
    return actionOf(T::class.java)
}

inline fun <reified T : Any> ConfigCenter.implOf(): T? {
    return implOf(T::class.java)
}