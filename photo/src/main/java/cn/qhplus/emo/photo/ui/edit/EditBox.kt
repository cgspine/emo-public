package cn.qhplus.emo.photo.ui.edit

import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.qhplus.emo.photo.R
import cn.qhplus.emo.photo.data.PhotoProvider
import cn.qhplus.emo.photo.ui.GestureContent
import cn.qhplus.emo.photo.ui.GestureContentState
import cn.qhplus.emo.ui.core.helper.OnePx
import cn.qhplus.emo.ui.core.modifier.throttleNoIndicationClick
import cn.qhplus.emo.ui.core.modifier.windowInsetsCommonNavPadding
import cn.qhplus.emo.ui.core.modifier.windowInsetsCommonTopPadding
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.coroutineScope

@Stable
sealed class EditScene
@Stable
object EditSceneNormal : EditScene()
@Stable
class EditScenePaint(val paint: PaintOption) : EditScene()
@Stable
class EditSceneText(val editLayer: TextEditLayer) : EditScene()

fun EditScene.isPaintScene(): Boolean = this is EditScenePaint
fun EditScene.isTextScene(): Boolean = this is EditSceneText
@Stable
class EditState(
    val config: PhotoEditConfig
) {
    val toolBarVisibility = mutableStateOf(true)

    internal var drawable: Drawable? = null

    val selectedPaintOption = mutableStateOf(config.paintOptions[config.paintOptions.size / 2])
    val selectedTextOption = mutableStateOf(config.textEditOptions[config.textEditOptions.size / 2])
    val textConfigReversed = mutableStateOf(false)

    val scene = mutableStateOf<EditScene>(EditSceneNormal)

    val paintEditLayers = mutableStateListOf<PathEditLayer>()

    val textEditLayers = mutableStateListOf<TextEditLayer>()
}


@Composable
fun EditBox(
    modifier: Modifier = Modifier,
    photoProvider: PhotoProvider,
    state: EditState,
    onBack: () -> Unit,
    onEnsure:(Drawable, PersistentList<EditLayer>) -> Unit
) {
    val systemUiController = rememberSystemUiController()
    SideEffect {
        if (systemUiController.isSystemBarsVisible) {
            systemUiController.isSystemBarsVisible = false
        }
    }

    val backHandler = remember(onBack) {
        {
            if(state.scene.value.isTextScene()){
                state.scene.value = EditSceneNormal
            } else {
                onBack()
            }
        }
    }
    BackHandler(true, backHandler)

    val gestureContentState = remember(photoProvider) {
        GestureContentState(
            photoProvider.ratio(),
            photoProvider.isLongImage()
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().then(modifier)) {
        GestureContent(
            modifier = Modifier.fillMaxSize(),
            state = gestureContentState,
            onTap = {
                state.textEditLayers.forEach {
                    it.isFocus = false
                }
            },
            canTransformStart = {
                state.textEditLayers.none { it.isFocus }
            }
        ) { onRatioEnsure ->
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val photo = remember(photoProvider) {
                    photoProvider.photo()
                }
                val photoRatio = remember {
                    mutableStateOf(photoProvider.ratio())
                }
                photo?.Compose(
                    contentScale = ContentScale.Fit,
                    isContainerDimenExactly = true,
                    onSuccess = {
                        state.drawable = it.drawable
                        if (it.drawable.intrinsicWidth > 0 && it.drawable.intrinsicHeight > 0) {
                            val ratio = it.drawable.intrinsicWidth.toFloat() / it.drawable.intrinsicHeight
                            onRatioEnsure(ratio)
                        }
                    },
                    onError = null
                )
                EditArea(state = state, gestureContentState = gestureContentState, ratioGetter = { photoRatio.value })
            }
        }
        EditCtrl(
            state = state,
            gestureContentState = gestureContentState,
            onBack = backHandler,
            onEnsure = {
                state.drawable?.let { drawable ->
                    onEnsure(
                        drawable,
                        (state.paintEditLayers + state.textEditLayers).map { it.toImmutable() }.toPersistentList()
                    )
                }
            },
        )
    }
}

@Composable
private fun BoxWithConstraintsScope.EditArea(
    state: EditState,
    gestureContentState: GestureContentState,
    ratioGetter: () -> Float
) {
    val ratio = ratioGetter()
    if (ratio <= 0f) {
        return
    }
    val viewportRatio = maxWidth / maxHeight
    BoxWithConstraints(modifier = Modifier
        .let {
            if (ratio >= viewportRatio) {
                it
                    .width(maxWidth)
                    .height(maxWidth / ratio)
            }else {
                it
                    .width(maxHeight * ratio)
                    .height(maxHeight)
            }
        }
        .align(Alignment.Center)
        .graphicsLayer {
            clip = true
        }
    ) {
        EditLayerList(state)
        EditGraffitiBoard(state, gestureContentState)
    }
}

@Composable
fun EditGraffitiBoard(
    state: EditState,
    gestureContentState: GestureContentState
) {
    val scene = state.scene.value
    if(scene is EditScenePaint){
        val size = gestureContentState.layoutInfo?.px?.let { Size(it.contentWidth, it.contentHeight) } ?: return
        GraffitiBoard(
            scene.paint,
            size,
            gestureContentState.targetScale,
            onTouchBegin = {
                state.paintEditLayers.add(it)
                state.toolBarVisibility.value = false
            },
            onTouchEnd = {
                state.toolBarVisibility.value = true
            })
    }
}

@Composable
fun GraffitiBoard(
    paint: PaintOption,
    size: Size,
    scale: Float,
    onTouchBegin: (PathEditLayer) -> Unit,
    onTouchEnd: (PathEditLayer) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(paint, scale, size) {
                coroutineScope {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = true)

                        if (down.pressed != down.previousPressed) {
                            down.consume()
                        }
                        val layer = paint.newPaintLayer(size, scale)
                        layer.append(down.position)
                        onTouchBegin(layer)
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.find { it.id.value == down.id.value }
                            if (change != null) {
                                if (change.positionChange() != Offset.Zero) {
                                    change.consume()
                                }
                                layer.append(change.position)
                            }
                        } while (change == null || change.pressed)
                        onTouchEnd(layer)
                    }
                }
            }
    )
}

@Composable
fun BoxWithConstraintsScope.EditCtrl(
    state: EditState,
    gestureContentState: GestureContentState,
    onBack: () -> Unit,
    onEnsure: () -> Unit
) {
    val isTextEditing by remember {
        derivedStateOf {
            state.scene.value.isTextScene()
        }
    }
    AnimatedVisibility(
        visible = state.toolBarVisibility.value,
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter),
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
        visible = !isTextEditing && state.toolBarVisibility.value,
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        EditCtrlToolBar(
            modifier = Modifier.windowInsetsCommonNavPadding(),
            state = state,
            gestureContentState = gestureContentState,
            onEnsureClick = onEnsure
        )
    }



    AnimatedVisibility(
        visible = isTextEditing,
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        EditTextCtrl(state = state, onBack)
    }

    // TopArea
    AnimatedVisibility(
        visible = state.toolBarVisibility.value,
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter),
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
        visible = state.toolBarVisibility.value,
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopStart),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        EditImageButton(
            modifier = Modifier
                .windowInsetsPadding(
                    WindowInsets.statusBars
                        .union(WindowInsets.displayCutout)
                        .only(WindowInsetsSides.Start + WindowInsetsSides.Top)
                )
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
            res = cn.qhplus.emo.ui.core.R.drawable.ic_topbar_back,
            config = state.config
        ) {
            onBack()
        }
    }
}


@Composable
private fun PaintSelector(
    state: EditState
){
    val isPaintScene by remember {
        derivedStateOf {
            state.scene.value.isPaintScene()
        }
    }
    EditImageButton(
        modifier = Modifier
            .padding(16.dp),
        res = R.drawable.ic_edit_paint,
        config = state.config,
        checked = isPaintScene
    ) {
        if(state.scene.value.isPaintScene()){
            state.scene.value = EditSceneNormal
        } else {
            state.scene.value = EditScenePaint(state.selectedPaintOption.value)
        }
    }
}

@Composable
private fun EditCtrlToolBar(
    modifier: Modifier,
    state: EditState,
    gestureContentState: GestureContentState,
    onEnsureClick: () -> Unit
) {
    Column(modifier = modifier
        .fillMaxWidth()) {
        EditPaintOptions(state, state.config.optionSelectorSize)
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(start = 4.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PaintSelector(state)
            EditImageButton(
                modifier = Modifier
                    .padding(10.dp),
                config = state.config,
                res = R.drawable.ic_edit_text
            ) {
                val layoutInfo = gestureContentState.layoutInfo ?: return@EditImageButton
                val layer = state.selectedTextOption.value.newTextLayer(state.textConfigReversed.value,
                    gestureContentState.targetScale,
                    gestureContentState.targetTranslateX,
                    gestureContentState.targetTranslateY,
                    layoutInfo,
                    {
                        state.scene.value = EditSceneText(it)
                    },
                    {
                        state.textEditLayers.remove(it)
                    }
                )
                state.textEditLayers.add(layer)
                state.scene.value = EditSceneText(layer)
            }
            Spacer(modifier = Modifier.weight(1f))
            EditSureButton(
                enabled = true,
                config = state.config,
                text = "确定",
                onClick = onEnsureClick,
            )
        }
    }
}

@Composable
private fun ColumnScope.EditPaintOptions(
    state: EditState,
    size: Dp
) {
    val isPaintScene by remember {
        derivedStateOf {
            state.scene.value.isPaintScene()
        }
    }
    if(isPaintScene){
        EditImageButton(
            modifier = Modifier
                .padding(20.dp)
                .align(Alignment.End),
            config = state.config,
            res = R.drawable.ic_edit_go_back
        ) {
            state.paintEditLayers.removeLastOrNull()
        }
        val selected = state.selectedPaintOption.value
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            state.config.paintOptions.forEach { paintOption ->
                key(paintOption) {
                    paintOption.Selector(size = size, selected = paintOption == selected) {
                        state.selectedPaintOption.value = it
                        state.scene.value = EditScenePaint(it)
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.EditLayerList(
    state: EditState,
) {
    EditPaintLayerList(state = state)
    EditTextLayerList(state = state)
}

@Composable
private fun BoxWithConstraintsScope.EditPaintLayerList(
    state: EditState,
) {
    state.paintEditLayers.forEach {layer ->
        key(layer) {
            with(layer){
                Content()
            }
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.EditTextLayerList(
    state: EditState,
) {
    state.textEditLayers.forEach {layer ->
        key(layer) {
            with(layer){
                Content()
            }
        }
    }
}

@Composable
fun BoxWithConstraintsScope.EditLayerList(list: PersistentList<EditLayer>) {
    list.forEach {layer ->
        key(layer) {
            with(layer){
                Content()
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditTextCtrl(
    state: EditState,
    onBack: () -> Unit
) {
    val scene = state.scene.value
    if(scene is EditSceneText){
        Column(modifier = Modifier
            .fillMaxSize()
            .background(state.config.textEditMaskColor)
            .throttleNoIndicationClick {
                onBack()
            }
            .windowInsetsPadding(
                WindowInsets.navigationBarsIgnoringVisibility
                    .union(WindowInsets.ime)
                    .union(WindowInsets.statusBarsIgnoringVisibility)
                    .union(WindowInsets.displayCutout)
            )) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val focusRequester = remember { FocusRequester() }

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
                BasicTextField(
                    value = scene.editLayer.text,
                    onValueChange = {
                        scene.editLayer.text = it
                    },
                    modifier = Modifier
                        .padding(16.dp)
                        .let {
                            if (scene.editLayer.reversed && scene.editLayer.text.text.isNotBlank()) {
                                it.background(color = scene.editLayer.color, shape = RoundedCornerShape(6.dp))
                            } else {
                                it
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .width(IntrinsicSize.Min)
                        .focusRequester(focusRequester),
                    textStyle = TextEditStyle.copy(
                        color = if(scene.editLayer.reversed) {
                            if(scene.editLayer.color == Color.White) Color.Black else Color.White
                        } else scene.editLayer.color
                    ),
                    cursorBrush = SolidColor(state.config.primaryColor),
                    keyboardOptions = remember {
                        KeyboardOptions(
                            KeyboardCapitalization.None,
                            imeAction = ImeAction.Done
                        )
                    },
                    keyboardActions = remember(onBack) {
                        KeyboardActions(onDone = {
                            onBack()
                        })
                    },
                )
            }
            EditTextOptions(state, state.config.optionSelectorSize)
        }
    }
}


@Composable
private fun EditTextOptions(
    state: EditState,
    size: Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EditImageButton(
            config = state.config,
            res = if(state.textConfigReversed.value) R.drawable.ic_edit_text_reversed else R.drawable.ic_edit_text,
            modifier = Modifier.padding(16.dp)
        ) {
            val reversed = !state.textConfigReversed.value
            state.textConfigReversed.value = reversed
            (state.scene.value as? EditSceneText)?.editLayer?.reversed = reversed
        }

        Box(
            modifier = Modifier
                .width(OnePx())
                .height(size + 8.dp)
                .background(Color.White.copy(alpha = 0.3f))
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            state.config.textEditOptions.forEach { option ->
                option.Selector(size = size, selected = state.selectedTextOption.value == option) {
                    state.selectedTextOption.value = it
                    (state.scene.value as? EditSceneText)?.editLayer?.let { layer ->
                        layer.color = it.color
                    }
                }
            }
        }
    }
}