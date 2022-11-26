package cn.qhplus.emo.config.ksp

import cn.qhplus.emo.config.ConfigBasic
import cn.qhplus.emo.config.ConfigMapFactory
import cn.qhplus.emo.config.ConfigWithBoolValue
import cn.qhplus.emo.config.ConfigWithDoubleValue
import cn.qhplus.emo.config.ConfigWithFloatValue
import cn.qhplus.emo.config.ConfigWithIntValue
import cn.qhplus.emo.config.ConfigWithLongValue
import cn.qhplus.emo.config.ConfigWithStringValue
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import java.io.OutputStream
import kotlin.reflect.KClass

class ConfigMapProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {

        val configs = resolver
            .getSymbolsWithAnnotation(ConfigBasic::class.java.name)
            .filterIsInstance<KSClassDeclaration>()
            .toList()

        if (configs.isEmpty()) return emptyList()

        val inputFlies = configs.map { it.containingFile!! }.toTypedArray()
        val os: OutputStream = codeGenerator.createNewFile(
            dependencies = Dependencies(true, *inputFlies),
            packageName = ConfigMapFactory::class.java.packageName,
            fileName = "Generated${ConfigMapFactory::class.java.simpleName}.kt"
        )

        os.writeLine("package ${ConfigMapFactory::class.java.packageName}")
        os.writeLine("import ${ConfigMapFactory::class.java.name}")
        os.write("class ${ConfigMapFactory::class.java.simpleName}Generated:${ConfigMapFactory::class.java.simpleName}")
        os.writeBlock {
            os.write("override fun factory(storage: ConfigStorage, prodMode: Boolean): ConfigMap")
            os.writeBlock {
                writeFactoryBody(configs)
            }
        }
        os.close()


        return emptyList()
    }

    private fun OutputStream.write(str: String) = write(str.toByteArray())
    private fun OutputStream.writeLine(str: String) = write(str + "\n")

    private fun OutputStream.writeBlock(block: OutputStream.() -> Unit) {
        writeLine("{")
        block()
        writeLine("}")
    }

    @OptIn(KspExperimental::class)
    private fun OutputStream.writeFactoryBody(configs: List<KSClassDeclaration>) {
        writeLine("val actionMap = mutableMapOf<Class<*>, ConfigAction>()")
        writeLine("val implMap = mutableMapOf<Class<*>, ConfigImplResolver<*>>()")
        configs.forEachIndexed { index, t ->
            logger.info("Generate config info for ${t.simpleName}.")

            if (t.modifiers.find { it == Modifier.SEALED } == null) {
                logger.exception(RuntimeException("${t.simpleName} must declared as sealed."))
            }

            val configBasicList = t.getAnnotationsByType(ConfigBasic::class).toList()
            if (configBasicList.size > 1) {
                logger.exception(RuntimeException("${t.simpleName} only can have one annotation with ConfigBasic."))
            }
            val configBasic = configBasicList[0]
            val metaVarLeft = if (index == 0) "var meta" else "meta"
            writeLine(
                "$metaVarLeft = ConfigMeta(" +
                        "\"${configBasic.name}\", " +
                        "\"${configBasic.humanName}\", " +
                        "${configBasic.versionRelated}, " +
                        "\"${configBasic.category}\", " +
                        "arrayOf(${configBasic.tags.joinToString(",") { "\"$it\"" }}))"
            )

            val subClasses = t.getSealedSubclasses().toList()
            val qualifiedName = t.qualifiedName!!.asString()
            var isConfigValueParsed: Boolean
            var writeSuccess = tryWriteConfigInfoWithType(
                t, subClasses, qualifiedName, index, "Bool", ConfigWithBoolValue::class
            ) {
                it.default.toString()
            }
            isConfigValueParsed = writeSuccess
            val mutiConfigValueMsg = "${t.simpleName} only can have one annotation in ConfigWith*Value"
            fun checkConfigValueParsed() {
                if (isConfigValueParsed && writeSuccess) {
                    logger.exception(RuntimeException(mutiConfigValueMsg))
                } else if (writeSuccess) {
                    isConfigValueParsed = true
                }
            }
            writeSuccess = tryWriteConfigInfoWithType(
                t, subClasses, qualifiedName, index, "Int", ConfigWithIntValue::class
            ) {
                it.default.toString()
            }
            checkConfigValueParsed()

            writeSuccess = tryWriteConfigInfoWithType(
                t, subClasses, qualifiedName, index, "Long", ConfigWithLongValue::class
            ) {
                it.default.toString()
            }
            checkConfigValueParsed()

            writeSuccess = tryWriteConfigInfoWithType(
                t, subClasses, qualifiedName, index, "Float", ConfigWithFloatValue::class
            ) {
                "${it.default}f"
            }
            checkConfigValueParsed()

            writeSuccess = tryWriteConfigInfoWithType(
                t, subClasses, qualifiedName, index, "Double", ConfigWithDoubleValue::class
            ) {
                it.default.toString()
            }
            checkConfigValueParsed()

            writeSuccess = tryWriteConfigInfoWithType(
                t, subClasses, qualifiedName, index, "String", ConfigWithStringValue::class
            ) {
                "\"${it.default}\""
            }
            checkConfigValueParsed()

            if (!isConfigValueParsed) {
                logger.exception(RuntimeException("${t.simpleName} must annotated by ConfigWith*Value"))
            }


        }
        writeLine("return ConfigMap(actionMap, implMap)")
    }

    @OptIn(KspExperimental::class)
    private fun <T : Annotation> OutputStream.tryWriteConfigInfoWithType(
        cls: KSClassDeclaration,
        subClasses: List<KSClassDeclaration>,
        qualifiedName: String,
        index: Int,
        configClsPrefix: String,
        annotationKClass: KClass<T>,
        defaultValue: (T) -> String
    ): Boolean {
        cls.getAnnotationsByType(annotationKClass).firstOrNull()?.let {
            writeLine("val action$index = ${configClsPrefix}ConfigAction(storage, meta, ${defaultValue(it)})")
            writeLine("actionMap[$qualifiedName::class.java] = action$index")
            writeImplResolverIfNeeded(subClasses, qualifiedName, index, configClsPrefix, annotationKClass) { v ->
                defaultValue(v)
            }
            return true
        }
        return false
    }

    @OptIn(KspExperimental::class)
    private fun <T : Annotation> OutputStream.writeImplResolverIfNeeded(
        subClasses: List<KSClassDeclaration>,
        qualifiedName: String,
        index: Int,
        configClsPrefix: String,
        annotationKClass: KClass<T>,
        defaultValue: (T) -> String
    ) {
        if (subClasses.isNotEmpty()) {
            val pairs = subClasses.mapNotNull { cls ->
                val annotation = cls.getAnnotationsByType(annotationKClass).firstOrNull()
                if (annotation == null) {
                    logger.warn("${cls.simpleName} should annotated with ConfigWithBoolValue")
                    null
                } else {
                    val instanceField = if(cls.classKind == ClassKind.OBJECT){
                        cls.qualifiedName!!.asString()
                    } else "null"
                    val valueType = if(configClsPrefix == "Bool") "Boolean" else configClsPrefix
                    "ConfigImplItem<${qualifiedName}, $valueType>(${cls.qualifiedName!!.asString()}::class.java,${instanceField},${defaultValue(annotation)})"
                }
            }.joinToString(",")
            if (pairs.isNotBlank()) {
                writeLine("val pairs${index} = listOf($pairs)")
                writeLine("implMap[$qualifiedName::class.java] = ${configClsPrefix}ClsConfigImplResolver<$qualifiedName>(pairs$index, prodMode, action$index)")
            }
        }
    }
}