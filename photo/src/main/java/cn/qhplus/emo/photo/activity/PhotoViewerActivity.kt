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

package cn.qhplus.emo.photo.activity

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import cn.qhplus.emo.core.EmoLog
import cn.qhplus.emo.photo.R
import cn.qhplus.emo.photo.data.PhotoShot
import cn.qhplus.emo.photo.data.PhotoShotDelivery
import cn.qhplus.emo.photo.data.PhotoShotRecover
import cn.qhplus.emo.photo.data.PhotoViewerData
import cn.qhplus.emo.photo.data.lossPhotoShot
import cn.qhplus.emo.photo.ui.viewer.PhotoPageCtrl
import cn.qhplus.emo.photo.ui.viewer.PhotoViewerArg
import cn.qhplus.emo.photo.ui.viewer.PhotoViewerScaffold
import com.google.accompanist.pager.ExperimentalPagerApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val PHOTO_CURRENT_INDEX = "emo_photo_current_index"
private const val PHOTO_SHOT_DELIVERY_KEY = "emo_photo_shot_delivery"
private const val PHOTO_COUNT = "emo_photo_count"
private const val PHOTO_META_KEY_PREFIX = "emo_photo_meta_"
private const val PHOTO_RECOVER_CLASS_KEY_PREFIX = "emo_photo_recover_cls_"

open class PhotoViewerActivity : ComponentActivity() {

    companion object {

        fun intentOf(
            context: Context,
            cls: Class<out PhotoViewerActivity>,
            list: List<PhotoShot>,
            index: Int
        ): Intent {
            val data = PhotoViewerData(list, index)
            val intent = Intent(context, cls)
            intent.putExtra(PHOTO_SHOT_DELIVERY_KEY, PhotoShotDelivery.put(data))
            intent.putExtra(PHOTO_CURRENT_INDEX, index)
            intent.putExtra(PHOTO_COUNT, list.size)
            if (list.size < 250) {
                list.forEachIndexed { i, shot ->
                    val meta = shot.photoProvider.meta()
                    val recoverCls = shot.photoProvider.recoverCls()
                    if (meta != null && recoverCls != null) {
                        intent.putExtra("${PHOTO_META_KEY_PREFIX}$i", meta)
                        intent.putExtra(
                            "${PHOTO_RECOVER_CLASS_KEY_PREFIX}$i",
                            recoverCls.name
                        )
                    }
                }
            } else {
                EmoLog.w(
                    "PhotoViewerActivity",
                    "once delivered too many photos, so only use memory data for delivery, " +
                        "there may be some recover issue."
                )
            }
            return intent
        }
    }

    private val viewModel by viewModels<PhotoViewerViewModel>()
    private val transitionTargetFlow = MutableStateFlow(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).run {
            hide(WindowInsetsCompat.Type.statusBars())
            isAppearanceLightNavigationBars = false
        }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                window.navigationBarDividerColor = android.graphics.Color.TRANSPARENT
                window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                transitionTargetFlow.value = false
            }
        })

        setContent {
            PageContent()
        }

        if(shouldTransitionPhoto()){
            overridePendingTransition(0, 0)
        }else{
            overridePendingTransition(R.anim.scale_enter, 0)
        }
    }

    override fun finish() {
        super.finish()
        if(!shouldTransitionPhoto()){
            overridePendingTransition(0, R.anim.scale_exit)
        }
    }

    @Composable
    protected open fun PageContent() {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            val data = viewModel.data
            if (data == null || data.list.isEmpty()) {
                Text(text = "没有图片数据")
            } else {
                PhotoViewer(transitionTargetFlow, data.list, data.index)
            }
        }
    }

    @OptIn(ExperimentalPagerApi::class)
    @Composable
    protected open fun PhotoViewer(
        transitionTargetFlow: StateFlow<Boolean>,
        list: List<PhotoShot>,
        index: Int
    ) {
        val arg = remember(list, index) {
            PhotoViewerArg(
                list,
                index,
                PhotoPageCtrl(
                    transitionTargetFlow = transitionTargetFlow,
                    onTapExit = { page, afterTransition ->
                        onTapExit(page, afterTransition)
                    },
                    onLongClick = { page, drawable ->
                        onLongClick(page, drawable)
                    },
                    shouldTransition = shouldTransitionPhoto(),
                    allowPullExit = allowPullExit()
                )
            )
        }
        PhotoViewerScaffold(arg)
    }

    protected open fun shouldTransitionPhoto(): Boolean {
        return true
    }

    protected open fun allowPullExit(): Boolean {
        return true
    }

    protected open fun onLongClick(page: Int, drawable: Drawable) {
    }

    protected open fun onTapExit(page: Int, afterTransition: Boolean) {
        if (afterTransition) {
            finish()
            overridePendingTransition(0, 0)
        } else {
            finish()
            overridePendingTransition(0, R.anim.scale_exit)
        }
    }
}

class PhotoViewerViewModel(state: SavedStateHandle) : ViewModel() {

    private val enterIndex = state.get<Int>(PHOTO_CURRENT_INDEX) ?: 0
    val data: PhotoViewerData?

    private val transitionDeliverKey = state.get<Long>(PHOTO_SHOT_DELIVERY_KEY) ?: -1

    init {
        val transitionDeliverData = PhotoShotDelivery.getAndRemove(transitionDeliverKey)
        data = if (transitionDeliverData != null) {
            transitionDeliverData
        } else {
            val count = state.get<Int>(PHOTO_COUNT) ?: 0
            if (count > 0) {
                val list = arrayListOf<PhotoShot>()
                for (i in 0 until count) {
                    try {
                        val meta = state.get<Bundle>("${PHOTO_META_KEY_PREFIX}$i")
                        val clsName =
                            state.get<String>("${PHOTO_RECOVER_CLASS_KEY_PREFIX}$i")
                        if (meta == null || clsName.isNullOrBlank()) {
                            list.add(lossPhotoShot)
                        } else {
                            val cls = Class.forName(clsName)
                            val recover = cls.newInstance() as PhotoShotRecover
                            list.add(recover.recover(meta) ?: lossPhotoShot)
                        }
                    } catch (e: Throwable) {
                        list.add(lossPhotoShot)
                    }
                }
                PhotoViewerData(list, enterIndex)
            } else {
                null
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        PhotoShotDelivery.remove(transitionDeliverKey)
    }
}

class MutableDrawableCache(var drawable: Drawable? = null)
