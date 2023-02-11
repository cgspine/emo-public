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

package cn.qhplus.emo.scheme

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class ActivityScheme(
    val action: String,
    val enterTransition: Int = SchemeTransition.SLIDE_IN_RIGHT,
    val exitTransition: Int = SchemeTransition.SLIDE_OUT_LEFT
)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class ComposeScheme(
    val action: String,
    val alternativeHosts: Array<KClass<*>>,
    val enterTransition: Int = SchemeTransition.SLIDE_IN_RIGHT,
    val exitTransition: Int = SchemeTransition.SLIDE_OUT_LEFT,
    val popEnterTransition: Int = SchemeTransition.SLIDE_IN_LEFT,
    val popExitTransition: Int = SchemeTransition.SLIDE_OUT_RIGHT
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class SchemeHost(
    val requiredArgs: Array<String>
)

@Repeatable
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class SchemeBoolArg(
    val name: String,
    val special: Boolean = false,
    val default: Boolean = false
)

@Repeatable
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class SchemeIntArg(
    val name: String,
    val special: Boolean = false,
    val default: Int = 0
)

@Repeatable
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class SchemeLongArg(
    val name: String,
    val special: Boolean = false,
    val default: Long = 0
)

@Repeatable
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class SchemeFloatArg(
    val name: String,
    val special: Boolean = false,
    val default: Float = 0.0f
)

@Repeatable
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class SchemeStringArg(
    val name: String,
    val special: Boolean = false,
    val default: String = ""
)
