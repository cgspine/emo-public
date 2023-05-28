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

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import cn.qhplus.emo.photo.data.EmoDefaultImagesProvider
import cn.qhplus.emo.photo.data.MediaDataProvider
import cn.qhplus.emo.photo.data.MediaPhotoBucketAllId
import cn.qhplus.emo.photo.data.MediaPhotoProviderFactory
import cn.qhplus.emo.photo.ui.picker.DefaultPhotoPickerConfigProvider
import cn.qhplus.emo.photo.ui.picker.LocalPhotoPickerConfig
import cn.qhplus.emo.photo.ui.picker.PhotoPickerConfigProvider
import cn.qhplus.emo.photo.ui.picker.PhotoPickerEditPage
import cn.qhplus.emo.photo.ui.picker.PhotoPickerGridPage
import cn.qhplus.emo.photo.ui.picker.PhotoPickerPreviewPage
import cn.qhplus.emo.photo.ui.picker.Route
import cn.qhplus.emo.photo.vm.PhotoPickerViewModel
import cn.qhplus.emo.ui.core.ex.setNavTransparent
import cn.qhplus.emo.ui.core.ex.setNormalDisplayCutoutMode
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal const val PHOTO_DEFAULT_PICK_LIMIT_COUNT = 9
internal const val PHOTO_RESULT_URI_LIST = "emo_photo_result_uri_list"
internal const val PHOTO_RESULT_ORIGIN_OPEN = "emo_photo_result_origin_open"
internal const val PHOTO_ENABLE_ORIGIN = "emo_photo_enable_origin"
internal const val PHOTO_CONFIG_PROVIDER = "emo_photo_config_provider"
internal const val PHOTO_PICK_LIMIT_COUNT = "emo_photo_pick_limit_count"
internal const val PHOTO_PICKED_ITEMS = "emo_photo_picked_items"
internal const val PHOTO_PROVIDER_FACTORY = "emo_photo_provider_factory"

class PhotoPickItemInfo(
    val id: Long,
    val name: String,
    val width: Int,
    val height: Int,
    val uri: Uri,
    val rotation: Int
) : Parcelable {

    private fun isRotated() = rotation == 90 || rotation == 270

    val displayWidth: Int by lazy {
        if (isRotated()) height else width
    }

    val displayHeight: Int by lazy {
        if (isRotated()) width else height
    }

    fun ratio(): Float {
        if (height <= 0 || width <= 0) {
            return -1f
        }
        if (isRotated()) {
            return height.toFloat() / width
        }
        return width.toFloat() / height
    }

    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readInt(),
        @Suppress("DEPRECATION")
        parcel.readParcelable(Uri::class.java.classLoader)!!,
        parcel.readInt()
    )

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeString(name)
        dest.writeInt(width)
        dest.writeInt(height)
        dest.writeParcelable(uri, flags)
        dest.writeInt(rotation)
    }

    companion object CREATOR : Parcelable.Creator<PhotoPickItemInfo> {
        override fun createFromParcel(parcel: Parcel): PhotoPickItemInfo {
            return PhotoPickItemInfo(parcel)
        }

        override fun newArray(size: Int): Array<PhotoPickItemInfo?> {
            return arrayOfNulls(size)
        }
    }
}

class PhotoPickResult(val list: List<PhotoPickItemInfo>, val isOriginOpen: Boolean)

fun Intent.getPhotoPickResult(): PhotoPickResult? {
    @Suppress("DEPRECATION")
    val list = getParcelableArrayListExtra<PhotoPickItemInfo>(PHOTO_RESULT_URI_LIST) ?: return null
    if (list.isEmpty()) {
        return null
    }
    val isOriginOpen = getBooleanExtra(PHOTO_RESULT_ORIGIN_OPEN, false)
    return PhotoPickResult(list, isOriginOpen)
}

open class PhotoPickerActivity : ComponentActivity() {

    companion object {

        fun intentOf(
            context: Context,
            factoryCls: Class<out MediaPhotoProviderFactory>,
            cls: Class<out PhotoPickerActivity> = PhotoPickerActivity::class.java,
            pickedItems: ArrayList<Uri> = arrayListOf(),
            pickLimitCount: Int = PHOTO_DEFAULT_PICK_LIMIT_COUNT,
            enableOrigin: Boolean = true,
            configProviderCls: Class<out PhotoPickerConfigProvider> = DefaultPhotoPickerConfigProvider::class.java
        ): Intent {
            val intent = Intent(context, cls)
            intent.putExtra(PHOTO_PICK_LIMIT_COUNT, pickLimitCount)
            intent.putParcelableArrayListExtra(PHOTO_PICKED_ITEMS, pickedItems)
            intent.putExtra(PHOTO_PROVIDER_FACTORY, factoryCls.name)
            intent.putExtra(PHOTO_ENABLE_ORIGIN, enableOrigin)
            intent.putExtra(PHOTO_CONFIG_PROVIDER, configProviderCls.name)
            return intent
        }
    }

    private val dataProviderInstance by lazy {
        dataProvider()
    }

    private val viewModel by viewModels<PhotoPickerViewModel>(factoryProducer = {
        object : AbstractSavedStateViewModelFactory(this@PhotoPickerActivity, intent?.extras) {
            override fun <T : ViewModel> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
                val constructor = modelClass.getDeclaredConstructor(
                    Application::class.java,
                    SavedStateHandle::class.java,
                    MediaDataProvider::class.java,
                    Array<String>::class.java
                )
                return constructor.newInstance(
                    this@PhotoPickerActivity.application,
                    handle,
                    dataProviderInstance,
                    supportedMimeTypes()
                )
            }
        }
    })

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
            PageContentWithConfigProvider(viewModel)
        }

        lifecycleScope.launch {
            viewModel.finishFlow.collectLatest {
                if (it != null) {
                    onHandleSend(it)
                } else {
                    finish()
                }
            }
        }
    }

    @Composable
    protected open fun PageContentWithConfigProvider(viewModel: PhotoPickerViewModel) {
        val configProvider = remember {
            kotlin.runCatching {
                val providerClsName = intent.getStringExtra(PHOTO_CONFIG_PROVIDER) ?: throw RuntimeException("No configProvider provided.")
                Class.forName(providerClsName).newInstance() as PhotoPickerConfigProvider
            }.getOrElse {
                DefaultPhotoPickerConfigProvider()
            }
        }
        configProvider.Provide {
            PageContent(viewModel = viewModel)
        }
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    protected open fun PageContent(viewModel: PhotoPickerViewModel) {
        Surface(
            color = LocalPhotoPickerConfig.current.screenBgColor,
            modifier = Modifier.fillMaxSize()
        ) {
            val navController = rememberAnimatedNavController()
            AnimatedNavHost(
                navController = navController,
                startDestination = Route.GRID
            ) {
                composable(
                    Route.GRID,
                    exitTransition = { fadeOut(tween()) },
                    popEnterTransition = { fadeIn(tween()) }
                ) {
                    PickerGrid(navController, viewModel)
                }

                composable(
                    "${Route.PREVIEW}/{bucketId}/{currentId}",
                    arguments = listOf(navArgument("currentId") { type = NavType.LongType }),
                    enterTransition = { fadeIn(tween()) },
                    exitTransition = { fadeOut(tween()) },
                    popEnterTransition = { fadeIn(tween()) },
                    popExitTransition = { fadeOut(tween()) + scaleOut(targetScale = 0.8f) }
                ) { backStack ->
                    val bucketId = backStack.arguments?.getString("bucketId") ?: MediaPhotoBucketAllId
                    val currentId = backStack.arguments?.getLong("currentId") ?: -1
                    PickerPreview(navController, viewModel, bucketId, currentId)
                }

                composable(
                    "${Route.EDIT}/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    enterTransition = { fadeIn(tween()) },
                    popExitTransition = { fadeOut(tween()) }
                ) { backStack ->
                    val id = backStack.arguments?.getLong("id") ?: -1
                    PickerEdit(navController, viewModel, id)
                }
            }
        }
    }

    @Composable
    protected open fun PickerGrid(
        navController: NavHostController,
        viewModel: PhotoPickerViewModel
    ) {
        PhotoPickerGridPage(navController, viewModel, dataProviderInstance.permissions())
    }

    @Composable
    protected open fun PickerPreview(
        navController: NavHostController,
        viewModel: PhotoPickerViewModel,
        bucketId: String,
        currentId: Long
    ) {
        PhotoPickerPreviewPage(navController, viewModel, bucketId, currentId)
    }

    @Composable
    protected open fun PickerEdit(
        navController: NavHostController,
        viewModel: PhotoPickerViewModel,
        id: Long
    ) {
        PhotoPickerEditPage(navController, viewModel, id)
    }

    protected open fun onHandleSend(pickedList: List<PhotoPickItemInfo>) {
        setResult(
            RESULT_OK,
            Intent().apply {
                putParcelableArrayListExtra(
                    PHOTO_RESULT_URI_LIST,
                    arrayListOf<PhotoPickItemInfo>().apply {
                        addAll(pickedList)
                    }
                )
                putExtra(PHOTO_RESULT_ORIGIN_OPEN, viewModel.isOriginOpenFlow.value)
            }
        )
        finish()
    }

    protected open fun dataProvider(): MediaDataProvider {
        return EmoDefaultImagesProvider()
    }

    protected open fun supportedMimeTypes(): Array<String> {
        return EmoDefaultImagesProvider.DEFAULT_SUPPORT_MIMETYPES
    }
}
