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

package cn.qhplus.emo.ui.core

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.VisualTransformation
import cn.qhplus.emo.ui.core.modifier.throttleClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Stable
class SearchState internal constructor(
    placeHolder: String,
    text: String,
    private val autoSearchIfValueChange: Boolean,
    private val onSearch: suspend (String) -> Unit
) {

    internal var placeHolder by mutableStateOf(placeHolder)

    internal var searchTextInner by mutableStateOf(text)

    val searchText by derivedStateOf {
        searchTextInner
    }

    private var searchJob: Job? = null

    fun updateSearchText(scope: CoroutineScope, text: String) {
        searchTextInner = text
        if (autoSearchIfValueChange) {
            searchIn(scope)
        }
    }

    fun clear(scope: CoroutineScope) {
        searchTextInner = ""
        searchIn(scope)
    }

    fun searchIn(scope: CoroutineScope) {
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(10)
            onSearch(searchTextInner)
        }
    }

    fun dispose() {
        searchJob?.cancel()
    }
}

@Composable
fun rememberSearchState(
    placeHolder: String = "",
    text: String = "",
    autoSearchIfValueChange: Boolean = true,
    onSearch: suspend (String) -> Unit
): SearchState {
    val state = remember(text) {
        SearchState(placeHolder, text, autoSearchIfValueChange, onSearch)
    }
    state.placeHolder = placeHolder

    DisposableEffect(state) {
        onDispose {
            state.dispose()
        }
    }
    return state
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    modifier: Modifier,
    shape: Shape = RoundedCornerShape(50),
    textStyle: TextStyle = LocalTextStyle.current,
    cursorColor: Color = MaterialTheme.colorScheme.primary,
    textFieldColors: TextFieldColors = TextFieldDefaults.colors(
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent
    ),
    state: SearchState,
    leadingIcon: @Composable (() -> Unit)? = {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = "search"
        )
    },
    placeholder: @Composable ((String) -> Unit)? = { hint ->
        Text(text = hint, style = textStyle)
    },
    trailingIcon: @Composable ((CoroutineScope) -> Unit)? = { scope ->
        if (state.searchText.isNotEmpty()) {
            Icon(
                imageVector = Icons.Filled.Cancel,
                contentDescription = "clear",
                modifier = Modifier.throttleClick {
                    state.clear(scope)
                }
            )
        }
    },
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val scope = rememberCoroutineScope()
    BasicTextField(
        value = state.searchText,
        modifier = modifier,
        onValueChange = {
            state.updateSearchText(scope, it)
        },
        textStyle = textStyle,
        cursorBrush = SolidColor(cursorColor),
        keyboardOptions = remember {
            KeyboardOptions(
                KeyboardCapitalization.None,
                imeAction = ImeAction.Search
            )
        },
        interactionSource = interactionSource,
        keyboardActions = remember {
            KeyboardActions(onSearch = {
                state.searchIn(scope)
            })
        },
        singleLine = true,
        maxLines = 1,
        decorationBox = @Composable { innerTextField ->
            // places leading icon, text field with label and placeholder, trailing icon
            TextFieldDefaults.DecorationBox(
                value = state.searchText,
                visualTransformation = VisualTransformation.None,
                innerTextField = innerTextField,
                placeholder = if (placeholder != null) {
                    { placeholder.invoke(state.placeHolder) }
                } else null,
                leadingIcon = leadingIcon,
                trailingIcon = { trailingIcon?.invoke(scope) },
                shape = shape,
                enabled = true,
                singleLine = true,
                contentPadding = PaddingValues(),
                interactionSource = interactionSource,
                colors = textFieldColors
            )
        }
    )
}
