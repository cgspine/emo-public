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

package cn.qhplus.emo.scheme.ksp

import cn.qhplus.emo.scheme.AbstractSchemeDefStorage
import cn.qhplus.emo.scheme.ActivityScheme
import cn.qhplus.emo.scheme.ComposeScheme
import cn.qhplus.emo.scheme.SchemeBoolArg
import cn.qhplus.emo.scheme.SchemeBoolArgParser
import cn.qhplus.emo.scheme.SchemeDef
import cn.qhplus.emo.scheme.SchemeFloatArg
import cn.qhplus.emo.scheme.SchemeFloatArgParser
import cn.qhplus.emo.scheme.SchemeIntArg
import cn.qhplus.emo.scheme.SchemeIntArgParser
import cn.qhplus.emo.scheme.SchemeLongArg
import cn.qhplus.emo.scheme.SchemeLongArgParser
import cn.qhplus.emo.scheme.SchemeStringArg
import cn.qhplus.emo.scheme.SchemeStringArgParser
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import java.io.OutputStream

class SchemeProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val activityList = resolver
            .getSymbolsWithAnnotation(ActivityScheme::class.java.name)
            .filterIsInstance<KSClassDeclaration>()
            .toSet()
            .sortedBy { it.simpleName.getShortName() }

        val composeList = resolver
            .getSymbolsWithAnnotation(ComposeScheme::class.java.name)
            .filterIsInstance<KSFunctionDeclaration>()
            .toSet()
            .sortedBy { it.simpleName.getShortName() }

        if (activityList.isEmpty() && composeList.isEmpty()) {
            return emptyList()
        }
        val composeGraph = processSchemeStorage(activityList, composeList)
        processComposeGraphBuilder(composeGraph)

        return emptyList()
    }

    @OptIn(KspExperimental::class)
    private fun processSchemeStorage(
        activityList: List<KSClassDeclaration>,
        composeList: List<KSFunctionDeclaration>
    ): List<ComposeGraphItem> {
        val storageInputFlies = sequenceOf(activityList, composeList)
            .flatMap { it.asSequence() }
            .map { it.containingFile!! }
            .toSet()
            .toTypedArray()
        val storagePackageName = "cn.qhplus.emo.scheme.impl"
        val osForStorage: OutputStream = codeGenerator.createNewFile(
            dependencies = Dependencies(true, *storageInputFlies),
            packageName = storagePackageName,
            fileName = "GeneratedSchemeDefStorage"
        )
        val composeGraph = mutableListOf<ComposeGraphItem>()
        osForStorage.writeLine("package $storagePackageName")
        osForStorage.writeLine("import androidx.annotation.Keep")
        osForStorage.writeLine("import cn.qhplus.emo.scheme.AbstractSchemeDefStorage")
        osForStorage.writeLine("import cn.qhplus.emo.scheme.SchemeDef")
        osForStorage.writeLine("import cn.qhplus.emo.scheme.SchemeArgDefine")
        osForStorage.writeLine("@Keep")
        osForStorage.write("class GeneratedSchemeDefStorage:${AbstractSchemeDefStorage::class.java.simpleName}()")
        osForStorage.writeBlock {
            osForStorage.write("init")
            osForStorage.writeBlock {
                var nextSchemeId = 1
                activityList.asSequence().forEach { cls ->
                    val schemes = cls.getAnnotationsByType(ActivityScheme::class)
                    val args = cls.buildArgDefineList()
                    schemes.forEach {
                        writeLine(
                            "add(SchemeDef(${nextSchemeId++}, \"${it.action}\", " +
                                "emptyList(), $args, \"${cls.qualifiedName!!.asString()}\"," +
                                "${it.enterTransition}, ${it.exitTransition}, ${it.enterTransition}, ${it.exitTransition}))"
                        )
                    }
                }

                composeList.asSequence().forEach { fn ->
                    // TODO can not use getAnnotationsByType because of usage of kClass. Just wait ksp's new version and check.
                    val schemes = fn.annotations.filter {
                        it.shortName.getShortName() == ComposeScheme::class.simpleName && it.annotationType.resolve().declaration
                            .qualifiedName?.asString() == ComposeScheme::class.qualifiedName
                    }
                    val args = fn.buildArgDefineList()
                    schemes.forEach {
                        val action = it.arguments.find { arg -> arg.name!!.asString() == "action" }!!.value as String
                        val host = it.arguments.find { arg -> arg.name!!.asString() == "alternativeHosts" }!!.value as List<*>
                        val enterTransition = it.arguments.find { arg -> arg.name!!.asString() == "enterTransition" }!!.value as Int
                        val exitTransition = it.arguments.find { arg -> arg.name!!.asString() == "exitTransition" }!!.value as Int
                        val popEnterTransition = it.arguments.find { arg -> arg.name!!.asString() == "popEnterTransition" }!!.value as Int
                        val popExitTransition = it.arguments.find { arg -> arg.name!!.asString() == "popExitTransition" }!!.value as Int
                        if (host.isEmpty()) {
                            throw RuntimeException("ComposeScheme.alternativeHosts for ${fn.simpleName.getShortName()} can not be empty")
                        }
                        val alternativeHosts = host.joinToString(",") { h ->
                            "${(h as KSType).declaration.qualifiedName!!.asString()}::class"
                        }
                        writeLine(
                            "add(SchemeDef($nextSchemeId, \"${action}\", " +
                                "listOf($alternativeHosts), $args, \"${SchemeDef.COMPOSE_CLASS_SUFFIX}\", " +
                                "$enterTransition, $exitTransition, $popEnterTransition, $popExitTransition))"
                        )
                        host.forEach { h ->
                            composeGraph.add(ComposeGraphItem(fn, h as KSType, nextSchemeId))
                        }
                        nextSchemeId++
                    }
                }
            }
        }
        osForStorage.close()
        return composeGraph
    }

    @OptIn(KspExperimental::class)
    private fun KSDeclaration.buildArgDefineList(): String {
        val boolArg = getAnnotationsByType(SchemeBoolArg::class)
            .map {
                "SchemeArgDefine(\"${it.name}\", ${it.required}, ${SchemeBoolArgParser::class.qualifiedName}, ${it.default})"
            }
        val intArg = getAnnotationsByType(SchemeIntArg::class)
            .map {
                "SchemeArgDefine(\"${it.name}\", ${it.required}, ${SchemeIntArgParser::class.qualifiedName}, ${it.default})"
            }
        val longArg = getAnnotationsByType(SchemeLongArg::class)
            .map {
                "SchemeArgDefine(\"${it.name}\", ${it.required}, ${SchemeLongArgParser::class.qualifiedName}, ${it.default})"
            }
        val floatArg = getAnnotationsByType(SchemeFloatArg::class)
            .map {
                "SchemeArgDefine(\"${it.name}\", ${it.required}, ${SchemeFloatArgParser::class.qualifiedName}, ${it.default}f)"
            }
        val stringArg = getAnnotationsByType(SchemeStringArg::class)
            .map {
                "SchemeArgDefine(\"${it.name}\", ${it.required}, ${SchemeStringArgParser::class.qualifiedName}, \"${it.default}\")"
            }
        val list = sequenceOf(boolArg, intArg, longArg, floatArg, stringArg)
            .flatMap { it.asSequence() }
            .joinToString(",")
        return "listOf($list)"
    }

    private fun processComposeGraphBuilder(
        composeGraph: List<ComposeGraphItem>
    ) {
        composeGraph.groupBy {
            it.host
        }.forEach { (host, items) ->
            val inputFiles = items.asSequence()
                .map { it.fn.containingFile!! }
                .toSet()
                .toTypedArray()
            val packageName = host.declaration.qualifiedName!!.asString().let { it.substring(0, it.lastIndexOf('.')) }
            val clsName = host.declaration.simpleName.asString() + SchemeDef.COMPOSE_CLASS_SUFFIX
            val os: OutputStream = codeGenerator.createNewFile(
                dependencies = Dependencies(true, *inputFiles),
                packageName = packageName,
                fileName = clsName
            )

            os.writeLine("package $packageName")
            os.writeLine("import androidx.annotation.Keep")
            os.writeLine("import androidx.navigation.NavGraphBuilder")
            os.writeLine("import androidx.compose.runtime.Composable")
            os.writeLine("import androidx.compose.animation.ExperimentalAnimationApi")
            os.writeLine("import com.google.accompanist.navigation.animation.composable")
            os.writeLine("import cn.qhplus.emo.scheme.SchemeClient")
            os.writeLine("import cn.qhplus.emo.scheme.impl.toComposeRouteDefine")
            os.writeLine("import cn.qhplus.emo.scheme.impl.toComposeNavArg")
            os.writeLine("import cn.qhplus.emo.scheme.impl.ComposeSchemeNavGraphBuilder")
            os.writeLine("import cn.qhplus.emo.scheme.impl.SchemeTransitionConverter")
            os.writeLine("@Keep")
            os.write("class $clsName: ComposeSchemeNavGraphBuilder")
            os.writeBlock {
                os.writeLine("@OptIn(ExperimentalAnimationApi::class)")
                os.write(
                    "override fun build(" +
                        "client: SchemeClient, " +
                        "navGraphBuilder: NavGraphBuilder, " +
                        "transitionConverter: SchemeTransitionConverter)"
                )
                os.writeBlock {
                    items.forEach { item ->
                        os.write("client.storage.findById(${item.schemeId})!!.let")
                        os.writeBlock {
                            os.write(
                                "navGraphBuilder.composable(" +
                                    "it.toComposeRouteDefine()," +
                                    "it.toComposeNavArg()," +
                                    "enterTransition = transitionConverter.enterTransition(it.enterTransition)," +
                                    "exitTransition = transitionConverter.exitTransition(it.exitTransition)," +
                                    "popEnterTransition = transitionConverter.enterTransition(it.popEnterTransition)," +
                                    "popExitTransition = transitionConverter.exitTransition(it.popExitTransition)" +
                                    "){"
                            )
                            if (item.fn.parameters.size == 1) {
                                os.writeLine(" entry ->")
                                os.writeLine("${item.fn.qualifiedName!!.asString()}(entry)")
                            } else if (item.fn.parameters.isEmpty()) {
                                os.writeLine("${item.fn.qualifiedName!!.asString()}()")
                            } else {
                                throw RuntimeException(
                                    "${item.fn.simpleName} can have one param with type NavBackStackEntry or have any param."
                                )
                            }
                            os.writeLine("}")
                        }
                    }
                }
            }
            os.close()
        }
    }

    private fun OutputStream.write(str: String) = write(str.toByteArray())
    private fun OutputStream.writeLine(str: String) = write(str + "\n")

    private fun OutputStream.writeBlock(block: OutputStream.() -> Unit) {
        writeLine("{")
        block()
        writeLine("}")
    }
}

class ComposeGraphItem(
    val fn: KSFunctionDeclaration,
    val host: KSType,
    val schemeId: Int
)
