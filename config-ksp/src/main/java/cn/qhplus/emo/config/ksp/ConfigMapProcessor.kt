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
            .toSet()
            .sortedBy { it.simpleName.getShortName() }

        if (configs.isEmpty()) return emptyList()

        val inputFlies = configs.map { it.containingFile!! }.toTypedArray()
        val osForConfigMapFactory: OutputStream = codeGenerator.createNewFile(
            dependencies = Dependencies(true, *inputFlies),
            packageName = ConfigMapFactory::class.java.packageName,
            fileName = "ConfigMapFactoryGenerated"
        )

        val osForConfigCenterEx: OutputStream = codeGenerator.createNewFile(
            dependencies = Dependencies(true, *inputFlies),
            packageName = ConfigMapFactory::class.java.packageName,
            fileName = "ConfigCenterEx"
        )

        osForConfigMapFactory.writeLine("package ${ConfigMapFactory::class.java.packageName}")
        osForConfigMapFactory.writeLine("import androidx.annotation.Keep")
        osForConfigMapFactory.writeLine("@Keep")
        osForConfigMapFactory.write("class ConfigMapFactoryGenerated:${ConfigMapFactory::class.java.simpleName}")
        osForConfigMapFactory.writeBlock {
            osForConfigMapFactory.write("override fun factory(storage: ConfigStorage, prodMode: Boolean): ConfigMap")
            osForConfigMapFactory.writeBlock {
                writeFactoryBody(configs)
            }
        }
        osForConfigMapFactory.close()

        osForConfigCenterEx.writeLine("package ${ConfigMapFactory::class.java.packageName}")
        osForConfigCenterEx.writeConfigListMethodEx(configs)
        osForConfigCenterEx.close()

        return emptyList()
    }

    private fun OutputStream.write(str: String) = write(str.toByteArray())
    private fun OutputStream.writeLine(str: String) = write(str + "\n")

    private fun OutputStream.writeBlock(block: OutputStream.() -> Unit) {
        writeLine("{")
        block()
        writeLine("}")
    }

    private fun OutputStream.writeConfigListMethodEx(configs: List<KSClassDeclaration>) {
        configs.forEach { cls ->
            logger.info("Generate config ex for ${cls.simpleName.getShortName()}.")

            sequenceOf(
                {
                    writeMethodEx(cls, ConfigWithBoolValue::class, "Bool")
                },
                {
                    writeMethodEx(cls, ConfigWithIntValue::class, "Int")
                },
                {
                    writeMethodEx(cls, ConfigWithLongValue::class, "Long")
                },
                {
                    writeMethodEx(cls, ConfigWithFloatValue::class, "Float")
                },
                {
                    writeMethodEx(cls, ConfigWithDoubleValue::class, "Double")
                },
                {
                    writeMethodEx(cls, ConfigWithStringValue::class, "String")
                }
            ).firstOrNull { it.invoke() }
        }
    }

    @OptIn(KspExperimental::class)
    private fun <T : Annotation> OutputStream.writeMethodEx(
        cls: KSClassDeclaration,
        annotationKClass: KClass<T>,
        type: String
    ): Boolean {
        return cls.getAnnotationsByType(annotationKClass).firstOrNull()?.let {
            write("fun ConfigCenter.actionOf${cls.simpleName.getShortName()}(): ${type}ConfigAction")
            writeBlock {
                writeLine("return actionOf(${cls.simpleName.getShortName()}::class.java).concrete$type()")
            }
        } != null
    }

    @OptIn(KspExperimental::class)
    private fun OutputStream.writeFactoryBody(configs: List<KSClassDeclaration>) {
        writeLine("val actionMap = mutableMapOf<Class<*>, ConfigAction>()")
        writeLine("val implMap = mutableMapOf<Class<*>, ConfigImplResolver<*>>()")
        configs.forEachIndexed { index, t ->
            logger.info("Generate config info for ${t.simpleName.getShortName()}.")

            if (t.modifiers.find { it == Modifier.SEALED } == null) {
                logger.exception(RuntimeException("${t.simpleName.getShortName()} must declared as sealed."))
            }

            val configBasicList = t.getAnnotationsByType(ConfigBasic::class).toList()
            if (configBasicList.size > 1) {
                logger.exception(RuntimeException("${t.simpleName.getShortName()} only can have one annotation with ConfigBasic."))
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
                t,
                subClasses,
                qualifiedName,
                index,
                "Bool",
                ConfigWithBoolValue::class
            ) {
                it.default.toString()
            }
            isConfigValueParsed = writeSuccess
            val mutiConfigValueMsg = "${t.simpleName.getShortName()} only can have one annotation in ConfigWith*Value"
            fun checkConfigValueParsed() {
                if (isConfigValueParsed && writeSuccess) {
                    logger.exception(RuntimeException(mutiConfigValueMsg))
                } else if (writeSuccess) {
                    isConfigValueParsed = true
                }
            }
            writeSuccess = tryWriteConfigInfoWithType(
                t,
                subClasses,
                qualifiedName,
                index,
                "Int",
                ConfigWithIntValue::class
            ) {
                it.default.toString()
            }
            checkConfigValueParsed()

            writeSuccess = tryWriteConfigInfoWithType(
                t,
                subClasses,
                qualifiedName,
                index,
                "Long",
                ConfigWithLongValue::class
            ) {
                it.default.toString()
            }
            checkConfigValueParsed()

            writeSuccess = tryWriteConfigInfoWithType(
                t,
                subClasses,
                qualifiedName,
                index,
                "Float",
                ConfigWithFloatValue::class
            ) {
                "${it.default}f"
            }
            checkConfigValueParsed()

            writeSuccess = tryWriteConfigInfoWithType(
                t,
                subClasses,
                qualifiedName,
                index,
                "Double",
                ConfigWithDoubleValue::class
            ) {
                it.default.toString()
            }
            checkConfigValueParsed()

            writeSuccess = tryWriteConfigInfoWithType(
                t,
                subClasses,
                qualifiedName,
                index,
                "String",
                ConfigWithStringValue::class
            ) {
                "\"${it.default}\""
            }
            checkConfigValueParsed()

            if (!isConfigValueParsed) {
                logger.exception(RuntimeException("${t.simpleName.getShortName()} must annotated by ConfigWith*Value"))
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
                    logger.warn("${cls.simpleName.getShortName()} should annotated with ConfigWithBoolValue")
                    null
                } else {
                    val instanceField = if (cls.classKind == ClassKind.OBJECT) {
                        cls.qualifiedName!!.asString()
                    } else "null"
                    val valueType = if (configClsPrefix == "Bool") "Boolean" else configClsPrefix
                    "ConfigImplItem<$qualifiedName, $valueType>(" +
                        "${cls.qualifiedName!!.asString()}::class.java,$instanceField,${defaultValue(annotation)})"
                }
            }.joinToString(",")
            if (pairs.isNotBlank()) {
                writeLine("val pairs$index = listOf($pairs)")
                writeLine(
                    "implMap[$qualifiedName::class.java] = " +
                        "${configClsPrefix}ClsConfigImplResolver<$qualifiedName>(pairs$index, prodMode, action$index)"
                )
            }
        }
    }
}
