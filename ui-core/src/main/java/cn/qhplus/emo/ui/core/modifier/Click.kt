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

package cn.qhplus.emo.ui.core.modifier

import android.os.SystemClock
import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun Modifier.throttleClick(
    timeout: Int = 250,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "throttleClick"
        properties["timeout"] = timeout
        properties["enabled"] = enabled
        properties["onClickLabel"] = onClickLabel
        properties["role"] = role
        properties["onClick"] = onClick
    }
) {
    val throttleHandler = remember(timeout) { ThrottleHandler(timeout) }
    Modifier.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        onClick = { throttleHandler.process(onClick) }
    )
}

fun Modifier.throttleClick(
    interactionSource: MutableInteractionSource,
    indication: Indication?,
    timeout: Int = 250,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "throttleClick"
        properties["timeout"] = timeout
        properties["enabled"] = enabled
        properties["onClickLabel"] = onClickLabel
        properties["role"] = role
        properties["onClick"] = onClick
        properties["indication"] = indication
        properties["interactionSource"] = interactionSource
    }
) {
    val throttleHandler = remember(timeout) { ThrottleHandler(timeout) }
    Modifier.clickable(
        interactionSource = interactionSource,
        indication = indication,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        onClick = { throttleHandler.process(onClick) }
    )
}

fun Modifier.debounceClick(
    timeout: Int = 250,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "debounceClick"
        properties["timeout"] = timeout
        properties["enabled"] = enabled
        properties["onClickLabel"] = onClickLabel
        properties["role"] = role
        properties["onClick"] = onClick
    }
) {
    val coroutineScope = rememberCoroutineScope()
    val debounceHandler = remember(timeout) { DebounceHandler(coroutineScope, timeout) }
    Modifier.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        onClick = { debounceHandler.process(onClick) }
    )
}

fun Modifier.debounceClick(
    interactionSource: MutableInteractionSource,
    indication: Indication?,
    timeout: Int = 250,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "debounceClick"
        properties["timeout"] = timeout
        properties["enabled"] = enabled
        properties["onClickLabel"] = onClickLabel
        properties["role"] = role
        properties["onClick"] = onClick
        properties["indication"] = indication
        properties["interactionSource"] = interactionSource
    }
) {
    val coroutineScope = rememberCoroutineScope()
    val debounceHandler = remember(timeout) { DebounceHandler(coroutineScope, timeout) }
    Modifier.clickable(
        interactionSource = interactionSource,
        indication = indication,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        onClick = { debounceHandler.process(onClick) }
    )
}

internal class ThrottleHandler(private val timeout: Int = 200) {

    private var last: Long = 0

    fun process(event: () -> Unit) {
        val now = SystemClock.uptimeMillis()
        if (now - last > timeout) {
            event.invoke()
            last = now
        }
    }
}

internal class DebounceHandler(
    private val coroutineScope: CoroutineScope,
    private val timeout: Int = 200
) {
    private var job: Job? = null

    fun process(event: () -> Unit) {
        job?.cancel()
        job = coroutineScope.launch {
            delay(timeout.toLong())
            event.invoke()
        }
    }
}
