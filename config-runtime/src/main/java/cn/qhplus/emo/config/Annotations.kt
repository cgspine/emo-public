package cn.qhplus.emo.config

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class ConfigBasic(
    val name: String,
    val humanName: String,
    val category: String = "default",
    val versionRelated: Boolean = false,
    val tags: Array<String> = []
)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class ConfigWithBoolValue(
    val default: Boolean = false
)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class ConfigWithIntValue(
    val default: Int = 0
)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class ConfigWithLongValue(
    val default: Long = 0
)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class ConfigWithFloatValue(
    val default: Float = 0f
)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class ConfigWithDoubleValue(
    val default: Double = 0.0
)


@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class ConfigWithStringValue(
    val default: String = "",
)