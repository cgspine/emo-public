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

package cn.qhplus.emo.photo.pdf

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.qhplus.emo.photo.data.BitmapPhotoProvider
import cn.qhplus.emo.photo.data.PhotoLoadStatus
import cn.qhplus.emo.photo.ui.GestureContent
import cn.qhplus.emo.photo.ui.GestureContentState
import cn.qhplus.emo.photo.ui.edit.EditBox
import cn.qhplus.emo.photo.ui.edit.EditState
import cn.qhplus.emo.photo.ui.edit.PathEditLayer
import cn.qhplus.emo.photo.ui.edit.TextEditLayer
import cn.qhplus.emo.ui.core.LazyListScrollBar
import cn.qhplus.emo.ui.core.Loading
import cn.qhplus.emo.ui.core.TopBar
import cn.qhplus.emo.ui.core.TopBarBackIconItem
import cn.qhplus.emo.ui.core.TopBarTextItem
import cn.qhplus.emo.ui.core.TopBarTitleLayout
import cn.qhplus.emo.ui.core.emoTopBarHeight
import cn.qhplus.emo.ui.core.ex.setNavTransparent
import cn.qhplus.emo.ui.core.ex.setNormalDisplayCutoutMode
import cn.qhplus.emo.ui.core.helper.OnePx
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface BundlePdfDataSourceFactory {
    fun factory(context: Context, bundle: Bundle): PdfDataSource
}

open class PdfActivity : ComponentActivity() {
    companion object {
        private const val PDF_KEY_META = "pdf_key_meta"
        private const val PDF_KEY_FACTORY_CLS = "pdf_key_factory_cls"
        private const val PDF_KEY_CONFIG_CLS = "pdf_key_config_cls"

        fun intentOf(
            context: Context,
            cls: Class<out PdfActivity> = PdfActivity::class.java,
            pdfMeta: Bundle,
            factoryCls: Class<out BundlePdfDataSourceFactory>,
            configProviderCls: Class<out PdfConfigProvider> = DefaultPdfConfigProvider::class.java
        ): Intent {
            val intent = Intent(context, cls)
            intent.putExtra(PDF_KEY_META, pdfMeta)
            intent.putExtra(PDF_KEY_FACTORY_CLS, factoryCls.name)
            intent.putExtra(PDF_KEY_CONFIG_CLS, configProviderCls.name)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).run {
            isAppearanceLightNavigationBars = false
        }
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.setNavTransparent()
        window.setNormalDisplayCutoutMode()
        setContent {
            PageContentWithConfigProvider()
        }
    }

    @Composable
    protected open fun PageContentWithConfigProvider() {
        val configProvider = remember {
            kotlin.runCatching {
                val providerClsName = intent.getStringExtra(PDF_KEY_CONFIG_CLS) ?: throw RuntimeException("No configProvider provided.")
                Class.forName(providerClsName).newInstance() as PdfConfigProvider
            }.getOrElse {
                DefaultPdfConfigProvider()
            }
        }
        configProvider.Provide {
            PageContent()
        }
    }

    protected open fun createDataSource(): PdfDataSource {
        val bundle = intent.getBundleExtra(PDF_KEY_META) ?: throw RuntimeException("pdf meta is not provided.")
        val factoryCls = intent.getStringExtra(PDF_KEY_FACTORY_CLS) ?: throw RuntimeException("factory class is not provided.")
        val instance = Class.forName(factoryCls).newInstance() as BundlePdfDataSourceFactory
        return instance.factory(this, bundle)
    }

    protected open fun onSaveEditLayerFailed(e: Throwable) {
        Toast.makeText(this@PdfActivity, "保存失败，请重试", Toast.LENGTH_SHORT).show()
    }

    @Composable
    protected open fun PageContent() {
        val editingPage = remember {
            mutableStateOf<PdfPage?>(null)
        }

        val isFullPageState = remember {
            mutableStateOf(false)
        }

        var loadStatus by remember {
            mutableStateOf(PhotoLoadStatus.Loading)
        }

        val dataSource = remember {
            createDataSource()
        }

        val context = LocalContext.current.applicationContext
        val listState = rememberSaveable(saver = LazyListState.Saver) {
            LazyListState(
                dataSource.readInitIndex(context),
                dataSource.readInitOffset(context)
            )
        }

        val gestureContentState = remember {
            GestureContentState(0f, true)
        }

        FullStateHandler(
            listState = listState,
            isFullState = { isFullPageState.value },
            isEditing = { editingPage.value != null },
            updateFullState = { isFullPageState.value = it }
        )

        BackHandler(editingPage.value != null) {
            editingPage.value = null
        }

        val pdfDrawable = remember {
            mutableStateOf<PdfDrawable?>(null)
        }

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            GestureContent(
                modifier = Modifier.fillMaxSize(),
                onLongPress = { offset ->
                    listState.layoutInfo.visibleItemsInfo.find {
                        it.offset < offset.y && it.offset + it.size > offset.y
                    }?.index?.let {
                        if (dataSource.supportEdit(it)) {
                            pdfDrawable.value?.list?.getOrNull(it)?.let { page ->
                                editingPage.value = page
                            }
                        }
                    }
                },
                state = gestureContentState,
                onTap = {
                    isFullPageState.value = !isFullPageState.value
                }
            ) { _ ->
                PdfBox(
                    dataSource = dataSource,
                    listState = listState,
                    firstPagePaddingForTopBar = true,
                    onSuccess = {
                        loadStatus = PhotoLoadStatus.Success
                        pdfDrawable.value = it
                    },
                    onError = {
                        loadStatus = PhotoLoadStatus.Failed
                    }
                )
            }

            if (loadStatus == PhotoLoadStatus.Loading) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Loading(
                        lineColor = LocalPdfConfig.current.tipColor
                    )
                    DownloadProgress(dataSource)
                }
            } else if (loadStatus == PhotoLoadStatus.Failed) {
                Text(
                    text = "加载失败",
                    modifier = Modifier.align(Alignment.Center),
                    color = LocalPdfConfig.current.tipColor
                )
            }
            PdfScrollBar(listState)
            TopBarLayoutAnimate(dataSource, listState, { isFullPageState.value }) {
                finish()
            }
        }
        EditingPage(
            dataSource = dataSource,
            pageGetter = { editingPage.value },
            onBack = { editingPage.value = null }
        )
    }

    @Composable
    protected open fun DownloadProgress(dataSource: PdfDataSource) {
        val progress by dataSource.downloadProgress.collectAsStateWithLifecycle()
        if (progress in 0..99) {
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = "下载中 $progress%",
                color = LocalPdfConfig.current.tipColor
            )
        }
    }

    @Composable
    fun EditingPage(dataSource: PdfDataSource, pageGetter: () -> PdfPage?, onBack: () -> Unit) {
        val page = pageGetter()
        if (page != null) {
            val provider = page.bitmap.value?.get()?.let {
                BitmapPhotoProvider(it, Color.White)
            }
            if (provider != null) {
                val config = LocalPdfConfig.current.editConfig
                val editState = remember(config) {
                    EditState(config).apply {
                        page.editLayers.value.asSequence().map {
                            it.toMutable(this)
                        }.forEach {
                            if (it is PathEditLayer) {
                                paintEditLayers.add(it)
                            } else if (it is TextEditLayer) {
                                textEditLayers.add(it)
                            }
                        }
                    }
                }
                val scope = rememberCoroutineScope()
                EditBox(
                    modifier = Modifier.background(Color.Black),
                    photoProvider = provider,
                    state = editState,
                    onBack = onBack,
                    onEnsure = { _, editLayers ->
                        page.editLayers.value = editLayers
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    dataSource.saveEditLayers(page.page, editLayers)
                                }
                                onBack()
                            } catch (e: Throwable) {
                                onSaveEditLayerFailed(e)
                            }
                        }
                    }
                )
            }
        }
    }

    @Composable
    fun FullStateHandler(
        listState: LazyListState,
        isFullState: () -> Boolean,
        isEditing: () -> Boolean,
        updateFullState: (Boolean) -> Unit
    ) {
        val systemUiController = rememberSystemUiController()
        val statusBarDarkContent = LocalPdfConfig.current.statusBarDarkContent
        LaunchedEffect(statusBarDarkContent) {
            systemUiController.statusBarDarkContentEnabled = statusBarDarkContent
        }
        val isFullPage = isFullState()
        LaunchedEffect(isFullPage) {
            systemUiController.isSystemBarsVisible = !isFullPage
        }
        val isScrollInProgress = remember {
            derivedStateOf {
                !isEditing() && listState.isScrollInProgress
            }
        }
        if (isScrollInProgress.value) {
            if (listState.canScrollBackward != isFullPage) {
                updateFullState(listState.canScrollBackward)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BoxWithConstraintsScope.PdfScrollBar(listState: LazyListState) {
    val systemBars = WindowInsets.systemBarsIgnoringVisibility
    val topInset = with(LocalDensity.current) {
        systemBars.getTop(LocalDensity.current).toDp()
    }
    val bottomInset = with(LocalDensity.current) {
        systemBars.getBottom(LocalDensity.current).toDp()
    }
    LazyListScrollBar(
        listState = listState,
        insetTop = emoTopBarHeight + topInset + 8.dp,
        insetBottom = bottomInset + 8.dp,
        insetRight = 4.dp,
        thumbWidth = 20.dp,
        thumbHeight = 72.dp,
        thumbBgColor = LocalPdfConfig.current.scrollBarBgColor,
        thumbLineColor = LocalPdfConfig.current.scrollBarLineColor
    )
}

@Composable
fun TopBarLayoutAnimate(
    dataSource: PdfDataSource,
    listState: LazyListState,
    isFullPage: () -> Boolean,
    onBack: () -> Unit
) {
    AnimatedVisibility(
        visible = !isFullPage(),
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it })
    ) {
        TopBarLayout(dataSource, listState, onBack)
    }
}

@Composable
fun TopBarLayout(
    dataSource: PdfDataSource,
    listState: LazyListState,
    onBack: () -> Unit
) {
    val contentColor = LocalPdfConfig.current.barContentColor
    val catalogText = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalCount = layoutInfo.totalItemsCount
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (totalCount == 0 || visibleItemsInfo.isEmpty()) {
                ""
            } else {
                val index = visibleItemsInfo.fastFirstOrNull {
                    it.offset >= layoutInfo.viewportStartOffset && it.offset + it.size <= layoutInfo.viewportEndOffset
                }?.index ?: visibleItemsInfo.fastFirstOrNull {
                    val visibleSize = if (it.offset >= layoutInfo.viewportStartOffset) {
                        layoutInfo.viewportEndOffset - it.offset
                    } else {
                        it.offset + it.size - layoutInfo.viewportStartOffset
                    }
                    visibleSize >= (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 3
                }?.index ?: visibleItemsInfo.first().index
                "${index + 1} / $totalCount"
            }
        }
    }
    val topBarLeftItems = remember(contentColor) {
        persistentListOf(
            TopBarBackIconItem(tint = contentColor) {
                onBack()
            }
        )
    }
    val topBarRightItems = remember(contentColor) {
        persistentListOf(
            TopBarTextItem(text = { catalogText.value }, color = contentColor) {
            }
        )
    }
    val separatorColor = LocalPdfConfig.current.barDividerColor
    val topBarTitle = dataSource.title.collectAsStateWithLifecycle()
    TopBar(
        title = { topBarTitle.value },
        separatorHeight = OnePx(),
        separatorColor = { separatorColor },
        paddingEnd = 16.dp,
        backgroundColor = LocalPdfConfig.current.barBgColor,
        leftItems = topBarLeftItems,
        rightItems = topBarRightItems,
        titleLayout = remember {
            object : TopBarTitleLayout {
                @Composable
                override fun Compose(titleGetter: () -> CharSequence, subTitleGetter: () -> CharSequence, alignTitleCenter: Boolean) {
                    val title = titleGetter()
                    Text(
                        title.toString(),
                        color = LocalPdfConfig.current.barContentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    )
}
