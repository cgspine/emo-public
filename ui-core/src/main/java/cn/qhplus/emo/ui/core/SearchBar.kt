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

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import cn.qhplus.emo.ui.core.modifier.throttleClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Stable
class SearchState internal constructor(
    placeHolder: String,
    text: String,
    private val onSearch: suspend (String) -> Unit
) {

    internal var placeHolder by mutableStateOf(placeHolder)

    internal var searchTextInner by mutableStateOf(text)

    val searchText by derivedStateOf {
        searchTextInner
    }

    private var searchJob: Job? = null

    fun searchIn(scope: CoroutineScope) {
        searchJob?.cancel()
        searchJob = scope.launch {
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
    onSearch: suspend (String) -> Unit
): SearchState {
    val state = remember(text) {
        SearchState(placeHolder, text, onSearch)
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
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(50),
    textStyle: TextStyle = LocalTextStyle.current,
    textFieldColors: TextFieldColors = TextFieldDefaults.textFieldColors(
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent
    ),
    state: SearchState
) {
    val scope = rememberCoroutineScope()
    TextField(
        value = state.searchText,
        textStyle = textStyle,
        onValueChange = {
            state.searchTextInner = it
        },
        maxLines = 1,
        modifier = modifier,
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "search"
            )
        },
        placeholder = {
            Text(text = state.placeHolder)
        },
        trailingIcon = {
            if (state.searchText.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Filled.Cancel,
                    contentDescription = "clear",
                    modifier = Modifier.throttleClick {
                        state.searchTextInner = ""
                        state.searchIn(scope)
                    }
                )
            }
        },
        keyboardOptions = remember {
            KeyboardOptions(
                KeyboardCapitalization.None,
                imeAction = ImeAction.Search
            )
        },
        keyboardActions = remember {
            KeyboardActions(onSearch = {
                state.searchIn(scope)
            })
        },
        shape = shape,
        colors = textFieldColors

    )
}
