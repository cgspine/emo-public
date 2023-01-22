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

package cn.qhplus.emo.scheme.impl

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import cn.qhplus.emo.scheme.Scheme
import cn.qhplus.emo.scheme.SchemeDef
import cn.qhplus.emo.scheme.SchemeDefStorage
import cn.qhplus.emo.scheme.SchemeHost
import cn.qhplus.emo.scheme.SchemeParts
import cn.qhplus.emo.scheme.SchemeTransaction
import cn.qhplus.emo.scheme.SchemeTransactionFactory
import java.util.ArrayList
import kotlin.reflect.KClass

class SchemeExecSingle(
    private val activity: Activity,
    private val storage: SchemeDefStorage,
    private val transitionConverter: SchemeTransitionConverter
) : SchemeTransaction {

    override fun exec(schemeParts: SchemeParts): Boolean {
        val schemeDef = storage.find(schemeParts) ?: return false
        val scheme = schemeParts.parse(schemeDef)
        if (schemeDef.targetId != SchemeDef.COMPOSE_CLASS_SUFFIX) {
            val cls = Class.forName(schemeDef.targetId)
            val intent = Intent(activity, cls)
            scheme.args.forEach { (key, value) ->
                intent.putAny(key, value)
            }
            intent.putExtra(SchemeKeys.KEY_ORIGIN, schemeParts.origin)
            activity.startActivity(intent)
            activity.overridePendingTransition(
                transitionConverter.enterRes(schemeDef.enterTransition),
                transitionConverter.exitRes(schemeDef.exitTransition)
            )
            return true
        } else {
            val routeValue = scheme.toComposeRouteValue()
            if (activity is ComposeHostActivity && schemeDef.alternativeHosts.contains(activity::class)) {
                val navController = activity.navController ?: throw RuntimeException("Not call SchemeNavHost in method Content.")
                if (scheme.isMatchToCurrentHost(activity::class, activity.intent)) {
                    navController.navigate(routeValue) {
                        anim {
                            enter = transitionConverter.enterRes(schemeDef.enterTransition)
                            exit = transitionConverter.exitRes(schemeDef.exitTransition)
                            popEnter = transitionConverter.exitRes(schemeDef.popEnterTransition)
                            popExit = transitionConverter.exitRes(schemeDef.popExitTransition)
                        }
                    }
                    return true
                }
            }
            val intent = scheme.createIntentForCompose(activity)
            if (intent != null) {
                activity.startActivity(intent.first)
                activity.overridePendingTransition(
                    transitionConverter.enterRes(schemeDef.enterTransition),
                    transitionConverter.exitRes(schemeDef.exitTransition)
                )
                return true
            }
        }
        return false
    }

    override fun finish(): Boolean {
        throw RuntimeException("should not be called.")
    }
}

class SchemeExecBatch(
    private val activity: Activity,
    private val storage: SchemeDefStorage,
    private val transitionConverter: SchemeTransitionConverter
) : SchemeTransaction {

    private val intentList: MutableList<Intent> = ArrayList()
    private var buildingComposeIntent: BuildingComposeIntent? = null
    private var lastSchemeDef: SchemeDef? = null

    override fun exec(schemeParts: SchemeParts): Boolean {
        val schemeDef = storage.find(schemeParts) ?: return false
        val scheme = schemeParts.parse(schemeDef)
        lastSchemeDef = schemeDef
        if (schemeDef.targetId != SchemeDef.COMPOSE_CLASS_SUFFIX) {
            buildingComposeIntent?.let {
                intentList.add(it.build())
                buildingComposeIntent = null
            }
            val cls = Class.forName(schemeDef.targetId)
            val intent = Intent(activity, cls)
            scheme.args.forEach { (key, value) ->
                intent.putAny(key, value)
            }
            intent.putExtra(SchemeKeys.KEY_ORIGIN, schemeParts.origin)
            intentList.add(intent)
            return true
        } else {
            val routeValue = scheme.toComposeRouteValue()
            if (intentList.isEmpty() && buildingComposeIntent == null &&
                activity is ComposeHostActivity &&
                schemeDef.alternativeHosts.contains(activity::class)
            ) {
                val navController = activity.navController ?: throw RuntimeException("Not call SchemeNavHost in method Content.")
                if (scheme.isMatchToCurrentHost(activity::class, activity.intent)) {
                    navController.navigate(routeValue) {
                        anim {
                            enter = transitionConverter.enterRes(schemeDef.enterTransition)
                            exit = transitionConverter.exitRes(schemeDef.exitTransition)
                            popEnter = transitionConverter.exitRes(schemeDef.popEnterTransition)
                            popExit = transitionConverter.exitRes(schemeDef.popExitTransition)
                        }
                    }
                    return true
                }
            }
            val buildingIntent = buildingComposeIntent
            if (buildingIntent != null && schemeDef.alternativeHosts.contains(buildingIntent.activityCls)) {
                if (scheme.isMatchToCurrentHost(buildingIntent.activityCls, buildingIntent.intent)) {
                    buildingIntent.composeRoutes.add(routeValue)
                    return true
                }
            }
            buildingComposeIntent?.let {
                intentList.add(it.build())
                buildingComposeIntent = null
            }
            val intent = scheme.createIntentForCompose(activity)
            if (intent != null) {
                buildingComposeIntent = BuildingComposeIntent(intent.second, intent.first)
                return true
            }
        }
        return false
    }

    override fun finish(): Boolean {
        buildingComposeIntent?.let {
            intentList.add(it.build())
            buildingComposeIntent = null
        }
        if (intentList.isNotEmpty()) {
            activity.startActivities(intentList.toTypedArray())
            lastSchemeDef?.apply {
                activity.overridePendingTransition(transitionConverter.enterRes(enterTransition), transitionConverter.exitRes(exitTransition))
            }
        }
        return true
    }

    private class BuildingComposeIntent(
        val activityCls: KClass<*>,
        val intent: Intent,
        val composeRoutes: MutableList<String> = mutableListOf()
    ) {
        fun build(): Intent {
            return intent.apply {
                if (composeRoutes.isNotEmpty()) {
                    putExtra(SchemeKeys.KEY_BATCH_SCHEME_LIST, composeRoutes.toTypedArray())
                }
            }
        }
    }
}

private fun Intent.putAny(key: String, value: Any) {
    when (value) {
        is Boolean -> putExtra(key, value)
        is Int -> putExtra(key, value)
        is Long -> putExtra(key, value)
        is Float -> putExtra(key, value)
        is String -> putExtra(key, value)
        else -> throw RuntimeException("Not support type(${value::class.java} for $key")
    }
}

private fun Bundle.putAny(key: String, value: Any) {
    when (value) {
        is Boolean -> putBoolean(key, value)
        is Int -> putInt(key, value)
        is Long -> putLong(key, value)
        is Float -> putFloat(key, value)
        is String -> putString(key, value)
        else -> throw RuntimeException("Not support type(${value::class.java} for $key")
    }
}

private fun Scheme.createIntentForCompose(activity: Activity): Pair<Intent, KClass<*>>? {
    for (cls in def.alternativeHosts) {
        val intent = Intent(activity, cls.java)
        val schemeHost = cls.java.getAnnotation(SchemeHost::class.java)
        val matched = schemeHost == null ||
            schemeHost.requiredArgs.all {
                    name ->
                args.entries.find { it.key == name }?.also { intent.putAny(it.key, it.value) } != null
            }
        if (matched) {
            intent.putExtra(SchemeKeys.KEY_START_DESTINATION, def.toComposeRouteDefine())
            intent.putExtra(
                SchemeKeys.KEY_START_ARGUMENTS,
                Bundle().apply {
                    args.forEach { (key, value) ->
                        if (def.args.find { it.name == key }?.special != true) {
                            putAny(key, value)
                        }
                    }
                    putString(SchemeKeys.KEY_ORIGIN, Uri.encode(origin))
                }
            )
            return intent to cls
        }
    }
    return null
}

private fun Scheme.isMatchToCurrentHost(host: KClass<*>, intent: Intent): Boolean {
    val schemeHost = host.java.getAnnotation(SchemeHost::class.java)
    return schemeHost == null || schemeHost.requiredArgs.asSequence().all {
        val value = args[it] ?: return@all false
        when (value) {
            is Boolean -> intent.getBooleanExtra(it, false) == value
            is Int -> intent.getIntExtra(it, 0) == value
            is Long -> intent.getLongExtra(it, 0) == value
            is Float -> intent.getFloatExtra(it, 0.0f) == value
            else -> intent.getStringExtra(it) == value
        }
    }
}

class AndroidSchemeExecTransactionFactory(
    val application: Application,
    val transitionConverter: SchemeTransitionConverter
) : SchemeTransactionFactory {

    private var currentActivity: Activity? = null
    private val callback = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            if (currentActivity == null) {
                currentActivity = activity
            }
        }

        override fun onActivityStarted(activity: Activity) {
        }

        override fun onActivityResumed(activity: Activity) {
            currentActivity = activity
        }

        override fun onActivityPaused(activity: Activity) {
        }

        override fun onActivityStopped(activity: Activity) {
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        }

        override fun onActivityDestroyed(activity: Activity) {
            if (currentActivity == activity) {
                currentActivity = null
            }
        }
    }

    init {
        application.registerActivityLifecycleCallbacks(callback)
    }

    override fun pop() {
        val activity = currentActivity ?: return
        if (activity is ComposeHostActivity) {
            if (activity.navController?.popBackStack() != true) {
                activity.finish()
            }
        } else {
            activity.finish()
        }
    }

    override fun factory(storage: SchemeDefStorage, batch: Boolean): SchemeTransaction {
        val activity = currentActivity ?: throw RuntimeException("current activity is null")
        if (batch) {
            return SchemeExecBatch(activity, storage, transitionConverter)
        }
        return SchemeExecSingle(activity, storage, transitionConverter)
    }

    protected fun finalize() {
        application.unregisterActivityLifecycleCallbacks(callback)
        currentActivity = null
    }
}
