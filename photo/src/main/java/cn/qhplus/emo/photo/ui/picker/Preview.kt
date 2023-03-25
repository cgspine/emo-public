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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cn.qhplus.emo.photo.activity.PhotoPickItemInfo
import cn.qhplus.emo.photo.data.MediaPhotoBucketSelectedId
import cn.qhplus.emo.photo.data.MediaPhotoVO
import cn.qhplus.emo.photo.data.PhotoLoadStatus
import cn.qhplus.emo.photo.ui.GesturePhoto
import cn.qhplus.emo.photo.vm.PhotoPickerViewModel
import cn.qhplus.emo.ui.core.Loading
import cn.qhplus.emo.ui.core.TopBar
import cn.qhplus.emo.ui.core.TopBarBackIconItem
import cn.qhplus.emo.ui.core.modifier.windowInsetsCommonNavPadding
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoPickerPreviewPage(
    navController: NavHostController,
    viewModel: PhotoPickerViewModel,
    bucketId: String,
    currentId: Long
) {
    val systemUiController = rememberSystemUiController()
    var isFullPageState by remember {
        mutableStateOf(false)
    }
    LaunchedEffect(isFullPageState) {
        systemUiController.isSystemBarsVisible = !isFullPageState
    }
    val list = remember {
        if (bucketId == MediaPhotoBucketSelectedId) {
            viewModel.getPickedVOList()
        } else {
            viewModel.photoPickerDataFlow.value.data?.find { it.id == bucketId }?.list ?: emptyList()
        }
    }

    val config = LocalPhotoPickerConfig.current

    val pickedItems by viewModel.pickedListFlow.collectAsState()

    val pagerState = rememberPagerState(list.indexOfFirst { it.model.id == currentId }.coerceAtLeast(0))

    val topBarLeftItems = remember {
        persistentListOf(
            TopBarBackIconItem {
                navController.popBackStack()
            }
        )
    }

    val topBarRightItems = remember(config) {
        persistentListOf(
            config.topBarSendFactory(true, viewModel.pickLimitCount, viewModel.pickedCountFlow) {
                val pickedList = viewModel.getPickedResultList()
                if (pickedList.isEmpty()) {
                    viewModel.handleFinish(
                        listOf(
                            list[pagerState.currentPage].let {
                                PhotoPickItemInfo(
                                    it.model.id,
                                    it.model.name,
                                    it.model.width,
                                    it.model.height,
                                    it.model.uri,
                                    it.model.rotation
                                )
                            }
                        )
                    )
                } else {
                    viewModel.handleFinish(pickedList)
                }
            }
        )
    }

    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        PhotoPickerPreviewContent(
            pagerState,
            list,
            loading = {
                Box(modifier = Modifier.fillMaxSize()) {
                    Loading(
                        modifier = Modifier.align(Alignment.Center),
                        lineColor = LocalPhotoPickerConfig.current.loadingColor
                    )
                }
            },
            loadingFailed = {}
        ) {
            isFullPageState = !isFullPageState
        }

        AnimatedVisibility(
            visible = !isFullPageState,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it })
        ) {
            TopBar(
                title = { "${pagerState.currentPage + 1}/${list.size}" },
                separatorHeight = 0.dp,
                paddingEnd = 16.dp,
                backgroundColor = config.topBarBgColor,
                leftItems = topBarLeftItems,
                rightItems = topBarRightItems
            )
        }

        AnimatedVisibility(
            visible = !isFullPageState,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                PhotoPickerPreviewPickedItems(list, pickedItems, list[pagerState.currentPage].model.id) {
                    scope.launch {
                        pagerState.scrollToPage(list.indexOf(it))
                    }
                }

                val isCurrentPicked = remember(list, pickedItems, pagerState.currentPage) {
                    pickedItems.indexOf(list[pagerState.currentPage].model.id) >= 0
                }

                PhotoPickerPreviewToolBar(
                    modifier = Modifier.fillMaxWidth(),
                    current = list[pagerState.currentPage],
                    isCurrentPicked = isCurrentPicked,
                    enableOrigin = viewModel.enableOrigin,
                    isOriginOpenFlow = viewModel.isOriginOpenFlow,
                    onToggleOrigin = {
                        viewModel.toggleOrigin(it)
                    },
                    onEdit = {
                        navController.navigate("${Route.EDIT}/${list[pagerState.currentPage].model.id}")
                    },
                    onToggleSelect = {
                        viewModel.togglePick(list[pagerState.currentPage])
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoPickerPreviewContent(
    pagerState: PagerState,
    data: List<MediaPhotoVO>,
    loading: @Composable BoxScope.() -> Unit,
    loadingFailed: @Composable BoxScope.() -> Unit,
    onTap: () -> Unit
) {
    HorizontalPager(
        pageCount = data.size,
        state = pagerState,
        key = { data[it].model.id }
    ) { page ->
        val item = data[page]
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            GesturePhoto(
                containerWidth = maxWidth,
                containerHeight = maxHeight,
                imageRatio = item.model.ratio(),
                shouldTransitionEnter = false,
                shouldTransitionExit = false,
                isLongImage = item.photoProvider.isLongImage(),
                onBeginPullExit = {
                    false
                },
                onTapExit = {
                    onTap()
                }
            ) { _, _, _, onImageRatioEnsured ->
                PhotoPickerPreviewItemContent(item, onImageRatioEnsured, loadingFailed, loading)
            }
        }
    }
}

@Composable
private fun PhotoPickerPreviewItemContent(
    item: MediaPhotoVO,
    onImageRatioEnsured: (Float) -> Unit,
    loading: @Composable BoxScope.() -> Unit,
    loadingFailed: @Composable BoxScope.() -> Unit
) {
    val photo = remember(item) {
        item.photoProvider.photo()
    }

    var loadStatus by remember {
        mutableStateOf(PhotoLoadStatus.Loading)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        photo?.Compose(
            contentScale = ContentScale.Fit,
            isContainerDimenExactly = true,
            onSuccess = {
                if (it.drawable.intrinsicWidth > 0 && it.drawable.intrinsicHeight > 0) {
                    onImageRatioEnsured(it.drawable.intrinsicWidth.toFloat() / it.drawable.intrinsicHeight)
                }
                loadStatus = PhotoLoadStatus.Success
            },
            onError = {
                loadStatus = PhotoLoadStatus.Failed
            }
        )

        if (loadStatus == PhotoLoadStatus.Loading) {
            loading()
        } else if (loadStatus == PhotoLoadStatus.Failed) {
            loadingFailed()
        }
    }
}

@Composable
fun PhotoPickerPreviewPickedItems(
    data: List<MediaPhotoVO>,
    pickedItems: List<Long>,
    currentId: Long,
    onClick: (MediaPhotoVO) -> Unit
) {
    if (pickedItems.isNotEmpty()) {
        val list = remember(data, pickedItems) {
            data.filter { pickedItems.contains(it.model.id) }
        }
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(LocalPhotoPickerConfig.current.toolBarBgColor),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = spacedBy(5.dp),
            contentPadding = PaddingValues(horizontal = 5.dp)
        ) {
            items(list, { it.model.id }) {
                PhotoPickerPreviewPickedItem(it, it.model.id == currentId, onClick)
            }
        }
    }
}

@Composable
private fun PhotoPickerPreviewPickedItem(
    item: MediaPhotoVO,
    isCurrent: Boolean,
    onClick: (MediaPhotoVO) -> Unit
) {
    val thumb = remember(item) {
        item.photoProvider.thumbnail(true)
    }
    Box(
        modifier = Modifier
            .size(50.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onClick(item)
            }
            .let {
                if (isCurrent) {
                    it.border(2.dp, LocalPhotoPickerConfig.current.commonIconCheckedTintColor)
                } else {
                    it
                }
            }
    ) {
        thumb?.Compose(
            contentScale = ContentScale.Crop,
            isContainerDimenExactly = true,
            onSuccess = null,
            onError = null
        )
    }
}

@Composable
fun PhotoPickerPreviewToolBar(
    modifier: Modifier = Modifier,
    current: MediaPhotoVO,
    isCurrentPicked: Boolean,
    enableOrigin: Boolean,
    isOriginOpenFlow: StateFlow<Boolean>,
    onToggleOrigin: (toOpen: Boolean) -> Unit,
    onEdit: () -> Unit,
    onToggleSelect: (toSelect: Boolean) -> Unit
) {
    val config = LocalPhotoPickerConfig.current
    Box(
        modifier = modifier
            .background(config.toolBarBgColor)
            .windowInsetsCommonNavPadding()
            .height(44.dp)
    ) {
        if (current.model.editable && config.editable) {
            CommonTextButton(
                modifier = Modifier.align(Alignment.CenterStart),
                enable = true,
                text = "编辑",
                onClick = onEdit
            )
        }

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

        PickCurrentCheckButton(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp)
                .align(Alignment.CenterEnd),
            isPicked = isCurrentPicked,
            onPicked = onToggleSelect
        )
    }
}
