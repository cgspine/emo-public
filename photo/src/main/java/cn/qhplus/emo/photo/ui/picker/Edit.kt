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

import android.graphics.drawable.Drawable
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.consumeDownChange
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavHostController
import cn.qhplus.emo.photo.data.MediaPhotoBucketAllId
import cn.qhplus.emo.photo.data.MediaPhotoVO
import cn.qhplus.emo.photo.ui.GesturePhoto
import cn.qhplus.emo.photo.vm.PhotoPickerViewModel
import cn.qhplus.emo.ui.core.R
import cn.qhplus.emo.ui.core.helper.OnePx
import cn.qhplus.emo.ui.core.modifier.windowInsetsCommonNavPadding
import cn.qhplus.emo.ui.core.modifier.windowInsetsCommonTopPadding
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

private sealed class PickerEditScene

private object PickerEditSceneNormal : PickerEditScene()
private object PickerEditScenePaint : PickerEditScene()
private class PickerEditSceneText(val editLayer: TextEditLayer? = null) : PickerEditScene()
private class PickerEditSceneClip(val area: Rect) : PickerEditScene()

private class EditSceneHolder<T : PickerEditScene>(var scene: T? = null)

private class MutablePickerPhotoInfo(
    var drawable: Drawable?,
    var mosaicBitmapCache: MutableMap<Int, ImageBitmap> = mutableMapOf()
)

internal data class PickerPhotoLayoutInfo(val scale: Float, val rect: Rect)

@Composable
fun PhotoPickerEditPage(
    navController: NavHostController,
    viewModel: PhotoPickerViewModel,
    id: Long
) {
    val systemUiController = rememberSystemUiController()
    SideEffect {
        if (systemUiController.isSystemBarsVisible) {
            systemUiController.isSystemBarsVisible = false
        }
    }
    val item = remember(id) {
        viewModel.photoPickerDataFlow.value.data
            ?.find { it.id == MediaPhotoBucketAllId }
            ?.list
            ?.find { it.model.id == id }
    }
    if (item != null) {
        PhotoPickerEditContent(item) {
            navController.popBackStack()
        }
    }
}

@Composable
fun PhotoPickerEditContent(
    data: MediaPhotoVO,
    onBack: () -> Unit
) {
    val sceneState = remember(data) {
        mutableStateOf<PickerEditScene>(PickerEditSceneNormal)
    }
    val scene = sceneState.value
    val photoInfo = remember(data) {
        MutablePickerPhotoInfo(null)
    }

    var photoLayoutInfo by remember(data) {
        mutableStateOf(PickerPhotoLayoutInfo(1f, Rect.Zero))
    }

    val paintEditLayers = remember(data) {
        mutableStateListOf<PaintEditLayer>()
    }

    val textEditLayers = remember(data) {
        mutableStateListOf<TextEditLayer>()
    }

    val config = LocalPhotoPickerConfig.current

    var forceHideTools by remember {
        mutableStateOf(false)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        GesturePhoto(
            containerWidth = maxWidth,
            containerHeight = maxHeight,
            imageRatio = data.model.ratio(),
            shouldTransitionEnter = false,
            shouldTransitionExit = false,
            isLongImage = data.photoProvider.isLongImage(),
            onBeginPullExit = {
                false
            },
            onTapExit = {
            },
            onPress = {
                textEditLayers.forEach {
                    it.isFocusFlow.value = false
                }
            }
        ) { _, scale, rect, onImageRatioEnsured ->
            photoLayoutInfo = PickerPhotoLayoutInfo(scale, rect)
            PhotoPickerEditPhotoContent(data) {
                photoInfo.drawable = it
                onImageRatioEnsured(it.intrinsicWidth.toFloat() / it.intrinsicHeight)
            }
        }

        PhotoEditHistoryList(
            photoLayoutInfo,
            paintEditLayers,
            textEditLayers,
            onFocusLayer = { focusLayer ->
                textEditLayers.forEach {
                    if (it != focusLayer) {
                        it.isFocusFlow.value = false
                    }
                }
            },
            onEditTextLayer = {
                sceneState.value = PickerEditSceneText(it)
            },
            onDeleteTextLayer = {
                textEditLayers.remove(it)
            },
            onToggleDragging = {
                forceHideTools = it
            }
        )

        AnimatedVisibility(
            visible = scene == PickerEditSceneNormal || scene == PickerEditScenePaint,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PhotoPickerEditPaintScreen(
                paintState = scene == PickerEditScenePaint,
                photoInfo = photoInfo,
                editLayers = paintEditLayers,
                layoutInfo = photoLayoutInfo,
                forceHideTools = forceHideTools,
                onBack = onBack,
                onPaintClick = {
                    sceneState.value = if (it) PickerEditScenePaint else PickerEditSceneNormal
                },
                onTextClick = {
                    sceneState.value = PickerEditSceneText()
                },
                onClipClick = {
                    sceneState.value = PickerEditSceneClip(Rect(Offset.Zero, photoLayoutInfo.rect.size))
                },
                onFinishPaintLayer = {
                    paintEditLayers.add(it)
                },
                onEnsureClick = {
                },
                onRevoke = {
                    paintEditLayers.removeLastOrNull()
                }
            )
        }

        AnimatedVisibility(
            visible = scene is PickerEditSceneText,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            // For exit animation
            val sceneHolder = remember {
                EditSceneHolder(scene as? PickerEditSceneText)
            }
            if (scene is PickerEditSceneText) {
                sceneHolder.scene = scene
            }
            val textScene = sceneHolder.scene
            if (textScene != null) {
                PhotoPickerEditTextScreen(
                    photoLayoutInfo,
                    constraints,
                    textScene.editLayer,
                    textScene.editLayer?.color ?: config.textEditColorOptions[0].color,
                    textScene.editLayer?.reverse ?: false,
                    onCancel = {
                        sceneState.value = PickerEditSceneNormal
                    },
                    onFinishTextLayer = { toReplace, target ->
                        if (toReplace != null) {
                            val index = textEditLayers.indexOf(toReplace)
                            if (index >= 0) {
                                textEditLayers[index] = target
                            } else {
                                textEditLayers.add(target)
                            }
                        } else {
                            textEditLayers.add(target)
                        }
                        sceneState.value = PickerEditSceneNormal
                    }
                )
            }
        }
    }
}

@Composable
private fun PhotoPickerEditPaintScreen(
    paintState: Boolean,
    editLayers: List<PaintEditLayer>,
    photoInfo: MutablePickerPhotoInfo,
    layoutInfo: PickerPhotoLayoutInfo,
    forceHideTools: Boolean,
    onBack: () -> Unit,
    onPaintClick: (toPaint: Boolean) -> Unit,
    onTextClick: () -> Unit,
    onClipClick: () -> Unit,
    onFinishPaintLayer: (PaintEditLayer) -> Unit,
    onEnsureClick: () -> Unit,
    onRevoke: () -> Unit
) {
    val paintEditOptions = LocalPhotoPickerConfig.current.editPaintOptions
    var paintEditCurrentIndex by remember {
        mutableStateOf(4)
    }

    if (paintEditCurrentIndex >= paintEditOptions.size) {
        paintEditCurrentIndex = paintEditOptions.size - 1
    }

    var showTools by remember {
        mutableStateOf(true)
    }

    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .constrainAs(createRef()) {
                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                    visibility = if (paintState) Visibility.Visible else Visibility.Gone
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
        ) {
            PhotoPaintCanvas(
                paintEditOptions[paintEditCurrentIndex],
                photoInfo,
                layoutInfo,
                editLayers,
                onTouchBegin = {
                    showTools = false
                },
                onTouchEnd = {
                    showTools = true
                    onFinishPaintLayer(it)
                }
            )
        }

        AnimatedVisibility(
            visible = showTools && !forceHideTools,
            modifier = Modifier.constrainAs(createRef()) {
                width = Dimension.fillToConstraints
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                top.linkTo(parent.top)
            },
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = Brush.verticalGradient(listOf(Color.Black.copy(0.2f), Color.Transparent)))
                    .windowInsetsCommonTopPadding()
                    .height(60.dp)
            )
        }

        AnimatedVisibility(
            visible = showTools && !forceHideTools,
            modifier = Modifier.constrainAs(createRef()) {
                width = Dimension.fillToConstraints
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(parent.bottom)
            },
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.2f))))
                    .windowInsetsCommonNavPadding()
                    .height(150.dp)
            )
        }

        AnimatedVisibility(
            visible = showTools && !forceHideTools,
            modifier = Modifier.constrainAs(createRef()) {
                start.linkTo(parent.start)
                top.linkTo(parent.top)
            },
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            CommonImageButton(
                modifier = Modifier
                    .windowInsetsPadding(
                        WindowInsets.statusBars
                            .union(WindowInsets.displayCutout)
                            .only(WindowInsetsSides.Start + WindowInsetsSides.Top)
                    )
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
                res = R.drawable.ic_topbar_back
            ) {
                onBack()
            }
        }

        val (toolBar, paintChooser) = createRefs()

        AnimatedVisibility(
            visible = showTools && !forceHideTools,
            modifier = Modifier.constrainAs(toolBar) {
                width = Dimension.fillToConstraints
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(parent.bottom)
            },
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PhotoPickerEditToolBar(
                modifier = Modifier.windowInsetsCommonNavPadding(),
                isPaintState = paintState,
                onPaintClick = onPaintClick,
                onTextClick = onTextClick,
                onClipClick = onClipClick,
                onEnsureClick = onEnsureClick
            )
        }

        AnimatedVisibility(
            visible = showTools && paintState && !forceHideTools,
            modifier = Modifier.constrainAs(paintChooser) {
                width = Dimension.fillToConstraints
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(toolBar.top, 8.dp)
            },
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PhotoPickerEditPaintOptions(
                paintEditOptions,
                24.dp,
                paintEditCurrentIndex
            ) {
                paintEditCurrentIndex = it
            }
        }

        AnimatedVisibility(
            visible = showTools && paintState && !forceHideTools,
            modifier = Modifier.constrainAs(createRef()) {
                end.linkTo(parent.end)
                bottom.linkTo(paintChooser.top)
            },
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            CommonImageButton(
                modifier = Modifier
                    .windowInsetsPadding(
                        WindowInsets.statusBars
                            .union(WindowInsets.displayCutout)
                            .only(WindowInsetsSides.Right)
                    )
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
                res = R.drawable.ic_topbar_back
            ) {
                onRevoke()
            }
        }
    }
}

@Composable
private fun PhotoPickerEditTextScreen(
    photoLayoutInfo: PickerPhotoLayoutInfo,
    constraints: Constraints,
    editLayer: TextEditLayer?,
    color: Color,
    isReverse: Boolean,
    onCancel: () -> Unit,
    onFinishTextLayer: (toReplace: TextEditLayer?, target: TextEditLayer) -> Unit
) {
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    DisposableEffect("") {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onCancel()
            }
        }
        onBackPressedDispatcher?.addCallback(callback)
        object : DisposableEffectResult {
            override fun dispose() {
                callback.remove()
            }
        }
    }

    var input by remember(editLayer) {
        val text = editLayer?.text ?: ""
        mutableStateOf(TextFieldValue(text, TextRange(text.length)))
    }

    val config = LocalPhotoPickerConfig.current

    var usedColor by remember(color) {
        mutableStateOf(color)
    }

    var usedReverse by remember(isReverse) {
        mutableStateOf(isReverse)
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .background(config.textEditMaskColor)
            .clickable(
                interactionSource = remember {
                    MutableInteractionSource()
                },
                indication = null
            ) {
                if (input.text.isNotBlank()) {
                    if (editLayer != null) {
                        onFinishTextLayer(
                            editLayer,
                            TextEditLayer(
                                input.text,
                                editLayer.fontSize,
                                editLayer.center,
                                usedColor,
                                usedReverse,
                                editLayer.offsetFlow,
                                editLayer.scaleFlow,
                                editLayer.rotationFlow
                            )
                        )
                    } else {
                        onFinishTextLayer(
                            null,
                            TextEditLayer(
                                input.text,
                                config.textEditFontSize,
                                Offset(
                                    (constraints.maxWidth / 2 - photoLayoutInfo.rect.left) / photoLayoutInfo.scale,
                                    constraints.maxHeight / 2 - photoLayoutInfo.rect.top
                                ),
                                usedColor,
                                usedReverse,
                                scaleFlow = MutableStateFlow(1 / photoLayoutInfo.scale)
                            )
                        )
                    }
                } else {
                    onCancel()
                }
            }
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val optionsId = createRef()
        PhotoPickerEditTextPaintOptions(
            config.textEditColorOptions,
            24.dp,
            usedColor,
            isReverse = usedReverse,
            modifier = Modifier.constrainAs(optionsId) {
                width = Dimension.fillToConstraints
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(parent.bottom)
            },
            onSelect = {
                usedColor = it
            },
            onReverseClick = {
                usedReverse = it
            }
        )
        BasicTextField(
            value = input,
            onValueChange = {
                input = it
            },
            modifier = Modifier
                .padding(16.dp)
                .let {
                    if (usedReverse && input.text.isNotBlank()) {
                        it.background(color = usedColor, shape = RoundedCornerShape(10.dp))
                    } else {
                        it
                    }
                }
                .padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 3.dp)
                .defaultMinSize(8.dp, 48.dp)
                .width(IntrinsicSize.Min)
                .focusRequester(focusRequester)
                .constrainAs(createRef()) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(optionsId.top)
                    top.linkTo(parent.top)
                },
            textStyle = TextStyle(
                color = if (usedReverse) {
                    if (usedColor == Color.White) Color.Black else Color.White
                } else usedColor,
                fontSize = config.textEditFontSize,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            ),
            cursorBrush = SolidColor(config.textCursorColor)
        )
    }
}

@Composable
fun PhotoPickerEditPhotoContent(
    data: MediaPhotoVO,
    onSuccess: (Drawable) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val photo = remember(data) {
            data.photoProvider.photo()
        }

        photo?.Compose(
            contentScale = ContentScale.Fit,
            isContainerDimenExactly = true,
            onSuccess = {
                if (it.drawable.intrinsicWidth > 0 && it.drawable.intrinsicHeight > 0) {
                    onSuccess(it.drawable)
                }
            },
            onError = null
        )
    }
}

@Composable
private fun PhotoEditHistoryList(
    layoutInfo: PickerPhotoLayoutInfo,
    editLayers: List<PaintEditLayer>,
    textEditLayers: List<TextEditLayer>,
    onFocusLayer: (TextEditLayer) -> Unit,
    onEditTextLayer: (TextEditLayer) -> Unit,
    onDeleteTextLayer: (TextEditLayer) -> Unit,
    onToggleDragging: (Boolean) -> Unit
) {
    if (layoutInfo.rect == Rect.Zero) {
        return
    }
    val (w, h) = with(LocalDensity.current) {
        arrayOf(
            layoutInfo.rect.width.toDp(),
            layoutInfo.rect.height.toDp()
        )
    }
    Canvas(
        modifier = Modifier
            .width(w / layoutInfo.scale)
            .height(h / layoutInfo.scale)
            .graphicsLayer {
                this.transformOrigin = TransformOrigin(0f, 0f)
                this.translationX = layoutInfo.rect.left
                this.translationY = layoutInfo.rect.top
                this.scaleX = layoutInfo.scale
                this.scaleY = layoutInfo.scale
                this.clip = true
            }
    ) {
        editLayers.forEach {
            with(it) {
                draw()
            }
        }
    }
    textEditLayers.forEach {
        key(it) {
            it.Content(
                layoutInfo = layoutInfo,
                onFocus = {
                    onFocusLayer(it)
                },
                onToggleDragging = { isDragging ->
                    onToggleDragging(isDragging)
                },
                onEdit = {
                    onEditTextLayer(it)
                }
            ) {
                onDeleteTextLayer(it)
            }
        }
    }
}

@Composable
private fun PhotoPaintCanvas(
    editPaint: EditPaint,
    photoInfo: MutablePickerPhotoInfo,
    layoutInfo: PickerPhotoLayoutInfo,
    editLayers: List<PaintEditLayer>,
    onTouchBegin: () -> Unit,
    onTouchEnd: (PaintEditLayer) -> Unit
) {
    val drawable = photoInfo.drawable ?: return
    val (w, h) = with(LocalDensity.current) {
        arrayOf(
            layoutInfo.rect.width.toDp(),
            layoutInfo.rect.height.toDp()
        )
    }

    val graffitiStrokeWidth = with(LocalDensity.current) {
        LocalPhotoPickerConfig.current.graffitiPaintStrokeWidth.toPx()
    }
    val mosaicStrokeWidth = with(LocalDensity.current) {
        LocalPhotoPickerConfig.current.mosaicPaintStrokeWidth.toPx()
    }
    val currentLayerState = remember(editLayers, editPaint, layoutInfo, drawable) {
        val layer = when (editPaint) {
            is ColorEditPaint -> {
                GraffitiEditLayer(Path(), editPaint.color, graffitiStrokeWidth / layoutInfo.scale)
            }
            is MosaicEditPaint -> {
                val image = photoInfo.mosaicBitmapCache[editPaint.scaleLevel] ?: drawable.toBitmap(
                    drawable.intrinsicWidth / editPaint.scaleLevel,
                    drawable.intrinsicHeight / editPaint.scaleLevel
                ).asImageBitmap().also {
                    photoInfo.mosaicBitmapCache[editPaint.scaleLevel] = it
                }
                MosaicEditLayer(
                    path = Path(),
                    image = image,
                    strokeWidth = mosaicStrokeWidth
                )
            }
        }
        mutableStateOf(layer, neverEqualPolicy())
    }

    val currentLayer = currentLayerState.value

    Canvas(
        modifier = Modifier
            .width(w / layoutInfo.scale)
            .height(h / layoutInfo.scale)
            .graphicsLayer {
                this.transformOrigin = TransformOrigin(0f, 0f)
                this.translationX = layoutInfo.rect.left
                this.translationY = layoutInfo.rect.top
                this.scaleX = layoutInfo.scale
                this.scaleY = layoutInfo.scale
                this.clip = true
            }
    ) {
        with(currentLayer) {
            draw()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(editLayers, editPaint, layoutInfo) {
                coroutineScope {
                    forEachGesture {
                        awaitPointerEventScope {
                            val down = awaitFirstDown(requireUnconsumed = true)
                            down.consumeDownChange()
                            currentLayer.path.moveTo(
                                (down.position.x - layoutInfo.rect.left) / layoutInfo.scale,
                                (down.position.y - layoutInfo.rect.top) / layoutInfo.scale
                            )
                            onTouchBegin()
                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.find { it.id.value == down.id.value }
                                if (change != null) {
                                    change.consumePositionChange()
                                    currentLayer.path.lineTo(
                                        (change.position.x - layoutInfo.rect.left) / layoutInfo.scale,
                                        (change.position.y - layoutInfo.rect.top) / layoutInfo.scale
                                    )
                                    currentLayerState.value = currentLayer
                                }
                            } while (change == null || change.pressed)
                            onTouchEnd(currentLayer)
                        }
                    }
                }
            }
    )
}

@Composable
private fun PhotoPickerEditPaintOptions(
    editPaint: List<EditPaint>,
    size: Dp,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        editPaint.forEachIndexed { index, paintEdit ->
            paintEdit.Compose(size = size, selected = index == selectedIndex) {
                onSelect(index)
            }
        }
    }
}

@Composable
private fun PhotoPickerEditToolBar(
    modifier: Modifier,
    isPaintState: Boolean,
    onPaintClick: (toPaint: Boolean) -> Unit,
    onTextClick: () -> Unit,
    onClipClick: () -> Unit,
    onEnsureClick: () -> Unit
) {
    ConstraintLayout(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        val (paint, text, clip, ensure) = createRefs()
        val horChain = createHorizontalChain(paint, text, clip, chainStyle = ChainStyle.Packed(0f))
        constrain(horChain) {
            start.linkTo(parent.start, 16.dp)
            end.linkTo(ensure.start)
        }
        CommonImageButton(
            modifier = Modifier
                .padding(10.dp)
                .constrainAs(paint) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                },
            res = R.drawable.ic_checkbox_checked,
            checked = isPaintState
        ) {
            onPaintClick(!isPaintState)
        }
        CommonImageButton(
            modifier = Modifier
                .padding(10.dp)
                .constrainAs(text) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                },
            res = R.drawable.ic_checkbox_checked
        ) {
            onTextClick()
        }
        CommonImageButton(
            modifier = Modifier
                .padding(10.dp)
                .constrainAs(clip) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                },
            res = R.drawable.ic_checkbox_checked
        ) {
            onClipClick()
        }
        CommonButton(
            enabled = true,
            text = "确定",
            onClick = onEnsureClick,
            modifier = Modifier.constrainAs(ensure) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                end.linkTo(parent.end, 16.dp)
            }
        )
    }
}

@Composable
private fun PhotoPickerEditTextPaintOptions(
    editPaint: List<ColorEditPaint>,
    size: Dp,
    color: Color,
    isReverse: Boolean,
    modifier: Modifier,
    onReverseClick: (isReverse: Boolean) -> Unit,
    onSelect: (Color) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CommonImageButton(
            res = R.drawable.ic_mark,
            modifier = Modifier.padding(16.dp)
        ) {
            onReverseClick(!isReverse)
        }

        Box(
            modifier = Modifier
                .width(OnePx())
                .height(size + 8.dp)
                .background(LocalPhotoPickerConfig.current.commonSeparatorColor)
        )

        Row(
            modifier = Modifier
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            editPaint.forEach { paintEdit ->
                paintEdit.Compose(size = size, selected = paintEdit.color == color) {
                    onSelect(paintEdit.color)
                }
            }
        }
    }
}
