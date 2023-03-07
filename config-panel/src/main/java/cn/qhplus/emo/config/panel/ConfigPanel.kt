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

package cn.qhplus.emo.config.panel

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.qhplus.emo.config.BoolConfigAction
import cn.qhplus.emo.config.ConfigAction
import cn.qhplus.emo.config.ConfigCenter
import cn.qhplus.emo.config.ConfigImplResolver
import cn.qhplus.emo.ui.core.Item
import cn.qhplus.emo.ui.core.PressWithAlphaBox
import cn.qhplus.emo.ui.core.SearchBar
import cn.qhplus.emo.ui.core.ex.drawBottomSeparator
import cn.qhplus.emo.ui.core.modifier.bottomSeparator
import cn.qhplus.emo.ui.core.modifier.throttleClick
import cn.qhplus.emo.ui.core.rememberSearchState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ConfigImplDisplayable {
    fun displayName(): String
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ConfigPanel(configCenter: ConfigCenter) {
    var configList by remember {
        mutableStateOf(configCenter.getAll().groupBy { it.meta.category }.toList())
    }
    val searchState = rememberSearchState(placeHolder = "搜索 Config") { text ->
        withContext(Dispatchers.IO) {
            val list = if (text.isBlank()) {
                configCenter.getAll()
            } else {
                configCenter
                    .getAll()
                    .filter {
                        if (text.startsWith("tag:")) {
                            it.meta.tags.contains(text.substring(4))
                        } else {
                            it.meta.name.lowercase().contains(text) || it.meta.humanName.lowercase().contains(text)
                        }
                    }
            }
            configList = list.groupBy { it.meta.category }.toList()
        }
    }
    val derivedConfigList by remember {
        derivedStateOf {
            configList
        }
    }
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .bottomSeparator(Color.Gray)
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            SearchBar(
                state = searchState,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = TextFieldDefaults.MinHeight)
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            derivedConfigList.forEach {
                stickyHeader(it.first) {
                    Text(
                        text = it.first,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.LightGray)
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                it.second.forEach { action ->
                    item(
                        key = action.meta.name,
                        contentType = action.javaClass
                    ) {
                        when (action) {
                            is BoolConfigAction -> {
                                BoolConfigActionItem(action)
                            }
                            else -> {
                                GeneralConfigActionItem(configCenter, action)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BoolConfigActionItem(action: BoolConfigAction) {
    val isSelected by action.stateFlowOf().collectAsStateWithLifecycle()
    Item(
        action.meta.humanName,
        drawBehind = {
            drawBottomSeparator(Color.LightGray, insetStart = 20.dp)
        },
        accessory = {
            Switch(
                checked = isSelected,
                onCheckedChange = {
                    action.write(it)
                }
            )
        }
    )
}

@Composable
fun GeneralConfigActionItem(configCenter: ConfigCenter, action: ConfigAction) {
    var value by remember {
        mutableStateOf(action.readAsString())
    }
    Item(
        action.meta.humanName,
        drawBehind = {
            drawBottomSeparator(Color.LightGray, insetStart = 20.dp)
        },
        accessory = {
            ConfigItemValueAccessory(
                configCenter,
                KeyboardOptions(
                    keyboardType = if (Number::class.java.isAssignableFrom(action.valueType())) KeyboardType.Number else KeyboardType.Text
                ),
                action.meta.name,
                value
            ) {
                value = it
                action.writeFromString(it)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigItemValueAccessory(
    configCenter: ConfigCenter,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
    configName: String,
    value: String,
    onValueChange: (value: String) -> Unit
) {
    val cls = configCenter.clsByName(configName)
    if (cls != null && ConfigImplDisplayable::class.java.isAssignableFrom(cls)) {
        val resolver = configCenter.resolverOf(cls)
        if (resolver != null) {
            ConfigDisplayInfo(resolver = resolver)
            return
        }
    }
    TextField(
        modifier = Modifier
            .widthIn(0.dp, 100.dp),
        colors = TextFieldDefaults.textFieldColors(containerColor = Color.Transparent),
        keyboardOptions = keyboardOptions,
        value = value,
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
        maxLines = 1,
        onValueChange = {
            onValueChange(it)
        }
    )
}

@Composable
fun ConfigDisplayInfo(resolver: ConfigImplResolver<*>) {
    var display by remember {
        mutableStateOf(resolver.resolve() as ConfigImplDisplayable)
    }
    PressWithAlphaBox(
        modifier = Modifier
            .throttleClick {
                display = resolver.setToNext() as ConfigImplDisplayable
            }
            .padding(start = 32.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Text(text = display.displayName())
    }
}
