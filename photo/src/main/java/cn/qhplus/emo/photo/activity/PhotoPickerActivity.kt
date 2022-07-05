package cn.qhplus.emo.photo.activity

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import cn.qhplus.emo.photo.data.EmoDefaultImagesProvider
import cn.qhplus.emo.photo.data.MediaDataProvider
import cn.qhplus.emo.photo.data.MediaPhotoBucketAllId
import cn.qhplus.emo.photo.data.MediaPhotoProviderFactory
import cn.qhplus.emo.photo.ui.picker.DefaultPhotoPickerConfigProvider
import cn.qhplus.emo.photo.ui.picker.LocalPhotoPickerConfig
import cn.qhplus.emo.photo.ui.picker.PICKER_ROUTE_Edit
import cn.qhplus.emo.photo.ui.picker.PICKER_ROUTE_GRID
import cn.qhplus.emo.photo.ui.picker.PICKER_ROUTE_PREVIEW
import cn.qhplus.emo.photo.ui.picker.PhotoPickerEditPage
import cn.qhplus.emo.photo.ui.picker.PhotoPickerGridPage
import cn.qhplus.emo.photo.ui.picker.PhotoPickerPreviewPage
import cn.qhplus.emo.photo.vm.PhotoPickerViewModel
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

const val PHOTO_DEFAULT_PICK_LIMIT_COUNT = 9
internal const val PHOTO_RESULT_URI_LIST = "emo_photo_result_uri_list"
internal const val PHOTO_RESULT_ORIGIN_OPEN = "emo_photo_result_origin_open"
internal const val PHOTO_ENABLE_ORIGIN = "emo_photo_enable_origin"
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

    fun ratio(): Float {
        if(height <= 0 || width <= 0){
            return -1f
        }
        if(rotation == 90 || rotation == 270){
            return height.toFloat() / width
        }
        return width.toFloat() / height
    }

    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readInt(),
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
            enableOrigin: Boolean = true
        ): Intent {
            val intent = Intent(context, cls)
            intent.putExtra(PHOTO_PICK_LIMIT_COUNT, pickLimitCount)
            intent.putParcelableArrayListExtra(PHOTO_PICKED_ITEMS, pickedItems)
            intent.putExtra(PHOTO_PROVIDER_FACTORY, factoryCls.name)
            intent.putExtra(PHOTO_ENABLE_ORIGIN, enableOrigin)
            return intent
        }
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
                    dataProvider(),
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
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                window.navigationBarDividerColor = android.graphics.Color.TRANSPARENT
                window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        setContent {
            PageContentWithConfigProvider(viewModel)
            PageContent(viewModel)
        }

        lifecycleScope.launch {
            viewModel.finishFlow.collectLatest {
                if(it != null){
                    onHandleSend(it)
                }else{
                    finish()
                }
            }
        }
    }

    @Composable
    protected open fun PageContentWithConfigProvider(viewModel: PhotoPickerViewModel){
        DefaultPhotoPickerConfigProvider {
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
                startDestination = PICKER_ROUTE_GRID
            ) {
                composable(
                    PICKER_ROUTE_GRID,
                    exitTransition = { fadeOut(tween()) },
                    popEnterTransition = { fadeIn(tween()) }
                ) {
                    PickerGrid(navController, viewModel)
                }

                composable(
                    "${PICKER_ROUTE_PREVIEW}/{bucketId}/{currentId}",
                    arguments = listOf(navArgument("currentId"){ type = NavType.LongType }),
                    enterTransition = { fadeIn(tween()) },
                    exitTransition = { fadeOut(tween()) + scaleOut(targetScale = 0.8f) },
                    popEnterTransition = { fadeIn(tween()) + scaleIn(initialScale = 0.8f) },
                    popExitTransition = { fadeOut(tween())}
                ) { backStack ->
                    val bucketId = backStack.arguments?.getString("bucketId") ?: MediaPhotoBucketAllId
                    val currentId = backStack.arguments?.getLong("currentId") ?: -1
                    PickerPreview(navController, viewModel, bucketId, currentId)
                }

                composable(
                    "${PICKER_ROUTE_Edit}/{id}",
                    arguments = listOf(navArgument("id"){ type = NavType.LongType }),
                    enterTransition = { fadeIn(tween()) + scaleIn(initialScale = 0.8f) },
                    exitTransition = { fadeOut(tween()) + scaleOut(targetScale = 0.8f) },
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
    ){
        PhotoPickerGridPage(navController, viewModel)
    }

    @Composable
    protected open fun PickerPreview(
        navController: NavHostController,
        viewModel: PhotoPickerViewModel,
        bucketId: String,
        currentId: Long
    ){
        PhotoPickerPreviewPage(navController, viewModel, bucketId, currentId)
    }

    @Composable
    protected open fun PickerEdit(
        navController: NavHostController,
        viewModel: PhotoPickerViewModel,
        id: Long
    ){
        PhotoPickerEditPage(navController, viewModel, id)
    }

    protected open fun onHandleSend(pickedList: List<PhotoPickItemInfo>) {
        setResult(RESULT_OK, Intent().apply {
            putParcelableArrayListExtra(PHOTO_RESULT_URI_LIST, arrayListOf<PhotoPickItemInfo>().apply {
                addAll(pickedList)
            })
            putExtra(PHOTO_RESULT_ORIGIN_OPEN, viewModel.isOriginOpenFlow.value)
        })
        finish()
    }

    protected open fun dataProvider(): MediaDataProvider {
        return EmoDefaultImagesProvider()
    }

    protected open fun supportedMimeTypes(): Array<String> {
        return EmoDefaultImagesProvider.DEFAULT_SUPPORT_MIMETYPES
    }
}