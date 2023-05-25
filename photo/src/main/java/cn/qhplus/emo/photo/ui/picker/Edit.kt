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

package cn.qhplus.emo.photo.ui.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalView
import androidx.navigation.NavHostController
import cn.qhplus.emo.photo.data.MediaPhotoBucketAllId
import cn.qhplus.emo.photo.ui.edit.EditBox
import cn.qhplus.emo.photo.ui.edit.EditState
import cn.qhplus.emo.photo.ui.saveEditBitmapToStore
import cn.qhplus.emo.photo.vm.PhotoPickerViewModel
import kotlinx.coroutines.launch


@Composable
fun PhotoPickerEditPage(
    navController: NavHostController,
    viewModel: PhotoPickerViewModel,
    id: Long
) {

    val item = remember(id) {
        viewModel.photoPickerDataFlow.value.data
            ?.find { it.id == MediaPhotoBucketAllId }
            ?.list
            ?.find { it.model.id == id }
    }
    if (item != null) {
        val view = LocalView.current
        val scope = rememberCoroutineScope()
        val config = LocalPhotoPickerConfig.current.editConfig
        val editState = remember(config) {
            EditState(config)
        }
        EditBox(
            photoProvider = item.photoProvider,
            state = editState,
            onBack = {
                navController.popBackStack()
            }
        ) { drawable, editLayers ->  
            if(!editLayers.isEmpty()){
                navController.popBackStack()
                return@EditBox
            }
            scope.launch {
                view.saveEditBitmapToStore(
                    drawable,
                    editLayers,
                    "edit-${System.currentTimeMillis()}",
                    view.width,
                )
                viewModel.loadData()
                navController.popBackStack()
            }
        }
    }
}
