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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference

enum class SchemeHandleStrategy {
    WaitPrevAndRun,
    CancelPrevAndRun,
    ContinuePrevOrRun
}

class SchemeClient(
    private val blockSameSchemeTimeout: Long,
    val storage: SchemeDefStorage,
    private val debug: Boolean = false,
    private val handler: SchemeHandler,
    private val transactionFactory: SchemeTransactionFactory
) {
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var lastHandledSchemes: List<String>? = null
    private var lastHandledTime: Long = 0
    private var handlingJob = AtomicReference<Job?>(null)

    fun pop() {
        transactionFactory.pop()
    }

    fun handleQuietly(
        scheme: String,
        strategy: SchemeHandleStrategy = SchemeHandleStrategy.WaitPrevAndRun
    ) {
        scope.launch {
            handle(scheme, strategy)
        }
    }

    suspend fun handle(
        scheme: String,
        strategy: SchemeHandleStrategy = SchemeHandleStrategy.WaitPrevAndRun
    ): Boolean {
        return handle(strategy, listOf(scheme)) {
            val env = transactionFactory.factory(storage, false)
            doHandle(env, scheme)
        }
    }

    fun batchHandleQuietly(
        scheme: List<String>,
        strategy: SchemeHandleStrategy = SchemeHandleStrategy.WaitPrevAndRun
    ) {
        scope.launch {
            batchHandle(scheme, strategy)
        }
    }

    suspend fun batchHandle(
        schemes: List<String>,
        strategy: SchemeHandleStrategy = SchemeHandleStrategy.WaitPrevAndRun
    ): Boolean {
        if (schemes.isEmpty()) {
            if (debug) {
                throw RuntimeException("schemes is empty.")
            }
            return false
        }
        if (schemes.size == 1) {
            return handle(schemes[0], strategy)
        }
        return handle(strategy, schemes) {
            val env = transactionFactory.factory(storage, true)
            for (element in schemes) {
                if (!doHandle(env, element)) {
                    return@handle false
                }
            }
            env.finish()
        }
    }

    private suspend fun handle(
        strategy: SchemeHandleStrategy,
        schemes: List<String>,
        handle: suspend () -> Boolean
    ): Boolean = withContext(Dispatchers.Main.immediate) {
        val current = System.currentTimeMillis()
        if (lastHandledSchemes == schemes && current - lastHandledTime < blockSameSchemeTimeout) {
            return@withContext true
        }

        val curHandlingJob = handlingJob.get()
        if (curHandlingJob != null) {
            when (strategy) {
                SchemeHandleStrategy.CancelPrevAndRun -> {
                    curHandlingJob.cancelAndJoin()
                }
                SchemeHandleStrategy.WaitPrevAndRun -> {
                    curHandlingJob.join()
                }
                SchemeHandleStrategy.ContinuePrevOrRun -> {
                    return@withContext true
                }
            }
        }

        lastHandledTime = current
        lastHandledSchemes = schemes
        val job = scope.async(start = CoroutineStart.LAZY) {
            handle()
        }
        job.invokeOnCompletion {
            handlingJob.compareAndSet(job, null)
        }
        handlingJob.set(job)
        try {
            job.await()
        } catch (e: Throwable) {
            if (debug) {
                throw e
            }
            false
        }
    }

    private suspend fun doHandle(env: SchemeTransaction, scheme: String): Boolean {
        val parts = scheme.parse()
        return handler.run(env, parts)
    }
}

interface SchemeHandler {
    suspend fun run(env: SchemeTransaction, schemeParts: SchemeParts): Boolean
}

interface SchemeInterceptor {
    suspend fun intercept(env: SchemeTransaction, schemeParts: SchemeParts, next: SchemeHandler): Boolean
}

object CoreSchemeHandler : SchemeHandler {
    override suspend fun run(env: SchemeTransaction, schemeParts: SchemeParts): Boolean {
        if (schemeParts.queries[SCHEME_ARG_BAD] == "1") {
            return false
        }
        return env.exec(schemeParts)
    }
}

class InterceptorSchemeHandler(
    private val delegate: SchemeHandler,
    private val interceptor: SchemeInterceptor
) : SchemeHandler {
    override suspend fun run(env: SchemeTransaction, schemeParts: SchemeParts): Boolean {
        return interceptor.intercept(env, schemeParts, delegate)
    }
}
