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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.navigation.NavHostController
import cn.qhplus.emo.core.EmoConfig
import cn.qhplus.emo.photo.data.MediaModel
import cn.qhplus.emo.photo.data.MediaPhotoBucketSelectedId
import cn.qhplus.emo.photo.data.MediaPhotoBucketVO
import cn.qhplus.emo.photo.data.MediaPhotoVO
import cn.qhplus.emo.photo.vm.PhotoPickerViewModel
import cn.qhplus.emo.ui.core.Loading
import cn.qhplus.emo.ui.core.TopBarBackIconItem
import cn.qhplus.emo.ui.core.TopBarWithLazyGridScrollState
import cn.qhplus.emo.ui.core.helper.OnePx
import cn.qhplus.emo.ui.core.modifier.throttleClick
import cn.qhplus.emo.ui.core.modifier.windowInsetsCommonNavPadding
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PhotoPickerGridPage(
    navController: NavHostController,
    viewModel: PhotoPickerViewModel,
    permissions: List<String>
) {
    val systemUiController = rememberSystemUiController()
    SideEffect {
        if (!systemUiController.isSystemBarsVisible) {
            systemUiController.isSystemBarsVisible = true
        }
    }
    val permission = rememberMultiplePermissionsState(permissions = permissions)
    when {
        permission.allPermissionsGranted -> {
            LaunchedEffect("") {
                viewModel.loadData()
            }
            val pickerData by viewModel.photoPickerDataFlow.collectAsState()
            if (pickerData.loading) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Loading(
                        modifier = Modifier.align(Alignment.Center),
                        lineColor = LocalPhotoPickerConfig.current.loadingColor
                    )
                }
            } else if (pickerData.error != null) {
                val text = if (EmoConfig.debug) {
                    "读取数据发生错误, ${pickerData.error?.message}"
                } else {
                    "读取数据发生错误"
                }
                CommonPickerTip(text = text)
            } else {
                val list = pickerData.data
                if (list == null || list.isEmpty()) {
                    CommonPickerTip(text = "你的相册空空如也~")
                } else {
                    PhotoPickerGridContent(navController, viewModel, list)
                }
            }
        }
        else -> {
            CommonPickerTip("选择图片需要相册权限\n请先打开权限")
            LaunchedEffect("") {
                permission.launchMultiplePermissionRequest()
            }
        }
    }
}

@Composable
fun PhotoPickerGridContent(
    navController: NavHostController,
    viewModel: PhotoPickerViewModel,
    data: List<MediaPhotoBucketVO>
) {
    var currentBucketId by rememberSaveable {
        mutableStateOf(data.first().id)
    }

    val currentBucket = data.find { it.id == currentBucketId } ?: data.first()

    val bucketFlow = remember {
        MutableStateFlow(currentBucket.name)
    }.apply {
        value = currentBucket.name
    }

    val isFocusBucketFlow = remember {
        MutableStateFlow(false)
    }

    val config = LocalPhotoPickerConfig.current

    val isFocusBucketChooser by isFocusBucketFlow.collectAsState()

    val topBarLeftItems = remember(config, isFocusBucketFlow) {
        val backItem = TopBarBackIconItem {
            viewModel.handleFinish(null)
        }
        val bucketItem = config.topBarBucketFactory(bucketFlow, isFocusBucketFlow) {
            isFocusBucketFlow.value = !isFocusBucketFlow.value
        }
        persistentListOf(backItem, bucketItem)
    }

    val topBarRightItems = remember(config) {
        val sendItem = config.topBarSendFactory(false, viewModel.pickLimitCount, viewModel.pickedCountFlow) {
            viewModel.handleFinish(viewModel.getPickedResultList())
        }
        persistentListOf(sendItem)
    }

    val gridState = rememberLazyGridState()
    val pickedItems by viewModel.pickedListFlow.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopBarWithLazyGridScrollState(
            scrollState = gridState,
            paddingEnd = 16.dp,
            separatorHeight = 0.dp,
            backgroundColor = LocalPhotoPickerConfig.current.topBarBgColor,
            leftItems = topBarLeftItems,
            rightItems = topBarRightItems
        )
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val (content, toolbar) = createRefs()
            LazyVerticalGrid(
                columns = GridCells.Adaptive(config.gridPreferredSize),
                state = gridState,
                modifier = Modifier
                    .fillMaxSize()
                    .constrainAs(content) {
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(toolbar.top)
                    },
                horizontalArrangement = Arrangement.spacedBy(config.gridGap),
                verticalArrangement = Arrangement.spacedBy(config.gridGap)
            ) {
                items(currentBucket.list, key = { it.model.id }) { item ->
                    PhotoPickerGridCell(
                        data = item,
                        pickedItems = pickedItems,
                        onPickItem = { _, model ->
                            viewModel.togglePick(model)
                        },
                        onPreview = {
                            navController.navigate("${Route.PREVIEW}/${currentBucket.id}/${it.id}")
                        }
                    )
                }
            }
            PhotoPickerGridToolBar(
                modifier = Modifier
                    .constrainAs(toolbar) {
                        width = Dimension.fillToConstraints
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom)
                    },
                enableOrigin = viewModel.enableOrigin,
                pickedItems = pickedItems,
                isOriginOpenFlow = viewModel.isOriginOpenFlow,
                onToggleOrigin = {
                    viewModel.toggleOrigin(it)
                }
            ) {
                // TODO spotless issue
                val mediaId = MediaPhotoBucketSelectedId
                navController.navigate("${Route.PREVIEW}/$mediaId/${pickedItems.first()}")
            }
            PhotoPickerBucketChooser(
                focus = isFocusBucketChooser,
                data = data,
                currentId = currentBucket.id,
                onBucketClick = {
                    currentBucketId = it.id
                    isFocusBucketFlow.value = false
                }
            ) {
                isFocusBucketFlow.value = false
            }
        }
    }
}

@Composable
private fun PhotoPickerGridCell(
    data: MediaPhotoVO,
    pickedItems: List<Long>,
    onPickItem: (toPick: Boolean, model: MediaPhotoVO) -> Unit,
    onPreview: (model: MediaModel) -> Unit
) {
    val pickedIndex = remember(pickedItems) {
        pickedItems.indexOfFirst {
            it == data.model.id
        }
    }
    BoxWithConstraints() {
        Box(
            modifier = Modifier
                .size(maxWidth)
                .fillMaxSize()
                .border(OnePx(), LocalPhotoPickerConfig.current.gridBorderColor)
                .clickable(
                    interactionSource = remember {
                        MutableInteractionSource()
                    },
                    indication = null,
                    enabled = true
                ) {
                    onPreview.invoke(data.model)
                }
        ) {
            val thumbnail = remember(data) {
                data.photoProvider.thumbnail(true)
            }
            thumbnail?.Compose(
                contentScale = ContentScale.Crop,
                isContainerDimenExactly = true,
                onSuccess = null,
                onError = null
            )

            PhotoPickerGridCellMask(pickedIndex)

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .throttleClick(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onPickItem(pickedIndex < 0, data)
                    }
                    .padding(4.dp)
                    .size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                PhotoPickCheckBox(pickedIndex)
            }
        }
    }
}

@Composable
fun PhotoPickerGridCellMask(pickedIndex: Int) {
    val maskAlpha = animateFloatAsState(targetValue = if (pickedIndex >= 0) 0.36f else 0.15f)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = maskAlpha.value))
    )
}

@Composable
fun PhotoPickerGridToolBar(
    modifier: Modifier = Modifier,
    enableOrigin: Boolean,
    pickedItems: List<Long>,
    isOriginOpenFlow: StateFlow<Boolean>,
    onToggleOrigin: (toOpen: Boolean) -> Unit,
    onPreview: () -> Unit
) {
    val config = LocalPhotoPickerConfig.current
    Box(
        modifier = modifier
            .background(config.toolBarBgColor)
            .windowInsetsCommonNavPadding()
            .height(44.dp)
    ) {
        CommonTextButton(
            modifier = Modifier.align(Alignment.CenterStart),
            enable = pickedItems.isNotEmpty(),
            text = "预览",
            onClick = onPreview
        )

        if (enableOrigin) {
            OriginOpenButton(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp)
                    .align(Alignment.Center),
                isOriginOpenFlow = isOriginOpenFlow,
                onToggleOrigin = onToggleOrigin
            )
        }
    }
}
