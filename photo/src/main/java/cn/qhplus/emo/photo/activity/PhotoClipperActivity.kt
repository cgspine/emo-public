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
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import cn.qhplus.emo.photo.R
import cn.qhplus.emo.photo.data.PhotoClipperDelivery
import cn.qhplus.emo.photo.data.PhotoProvider
import cn.qhplus.emo.photo.data.PhotoShotRecover
import cn.qhplus.emo.photo.data.lossPhotoProvider
import cn.qhplus.emo.photo.ui.clipper.PhotoClipper
import cn.qhplus.emo.photo.util.PhotoHelper
import cn.qhplus.emo.photo.util.saveToLocal
import cn.qhplus.emo.ui.core.Loading
import cn.qhplus.emo.ui.core.ex.setNavTransparent
import cn.qhplus.emo.ui.core.ex.setNormalDisplayCutoutMode
import cn.qhplus.emo.ui.core.modifier.throttleClick
import cn.qhplus.emo.ui.core.modifier.windowInsetsCommonNavPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PHOTO_CLIPPER_DELIVERY_KEY = "emo_photo_clipper_delivery"
private const val PHOTO_CLIPPER_META_KEY = "emo_photo_clipper_meta"
private const val PHOTO_CLIPPER_RECOVER_KEY = "emo_photo_clipper_recover"
private const val PHOTO_CLIPPER_RESULT_URI = "emo_photo_clipper_result_uri"
private const val PHOTO_CLIPPER_RESULT_WIDTH = "emo_photo_clipper_result_width"
private const val PHOTO_CLIPPER_RESULT_HEIGHT = "emo_photo_clipper_result_height"

class PhotoClipperResult(
    val width: Int,
    val height: Int,
    val uri: Uri
)
fun Intent.getPhotoClipperResult(): PhotoClipperResult? {
    val uri = getParcelableExtra<Uri>(PHOTO_CLIPPER_RESULT_URI) ?: return null
    val width = getIntExtra(PHOTO_CLIPPER_RESULT_WIDTH, -1)
    val height = getIntExtra(PHOTO_CLIPPER_RESULT_HEIGHT, -1)
    if (width <= 0 || height <= 0) {
        return null
    }
    return PhotoClipperResult(width, height, uri)
}

open class PhotoClipperActivity : ComponentActivity() {

    companion object {

        fun intentOf(
            context: Context,
            photoProvider: PhotoProvider,
            cls: Class<out PhotoClipperActivity> = PhotoClipperActivity::class.java
        ): Intent {
            val intent = Intent(context, cls)
            intent.putExtra(PHOTO_CLIPPER_DELIVERY_KEY, PhotoClipperDelivery.put(photoProvider))
            val meta = photoProvider.meta()
            val recoverCls = photoProvider.recoverCls()
            if (meta != null && recoverCls != null) {
                intent.putExtra(PHOTO_CLIPPER_META_KEY, meta)
                intent.putExtra(PHOTO_CLIPPER_RECOVER_KEY, recoverCls.name)
            }
            return intent
        }
    }

    private val clipStatus = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).run {
            isAppearanceLightNavigationBars = false
        }

        window.setNavTransparent()
        window.setNormalDisplayCutoutMode()

        val deliverKey = intent.getLongExtra(PHOTO_CLIPPER_DELIVERY_KEY, -1)
        val photoProvider = PhotoClipperDelivery.getAndRemove(deliverKey) ?: buildPhotoProvider()

        setContent {
            PageContent(photoProvider)
        }

        overridePendingTransition(R.anim.scale_enter, 0)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.scale_exit)
    }

    private fun buildPhotoProvider(): PhotoProvider {
        return try {
            val meta = intent.getBundleExtra(PHOTO_CLIPPER_META_KEY)
            val clsName = intent.getStringExtra(PHOTO_CLIPPER_RECOVER_KEY)
            if (meta == null || clsName.isNullOrBlank()) {
                lossPhotoProvider
            } else {
                val cls = Class.forName(clsName)
                val recover = cls.newInstance() as PhotoShotRecover
                recover.recover(meta)?.photoProvider ?: lossPhotoProvider
            }
        } catch (e: Throwable) {
            lossPhotoProvider
        }
    }

    @Composable
    protected open fun PageContent(photoProvider: PhotoProvider) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            PhotoClipper(
                photoProvider = photoProvider
            ) { doClip ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .windowInsetsCommonNavPadding()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .throttleClick {
                                finish()
                            }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "取消",
                            fontSize = 20.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .throttleClick {
                                val bitmap = doClip()
                                if (bitmap == null) {
                                    onClipFailed()
                                } else {
                                    clipStatus.value = true
                                    lifecycleScope.launch {
                                        onClipFinished(bitmap)
                                        clipStatus.value = false
                                    }
                                }
                            }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "确定",
                            fontSize = 20.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            ClipHanding()
        }
    }

    @Composable
    protected open fun ClipHanding() {
        val isHanding = clipStatus.value
        if (isHanding) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Loading(
                    modifier = Modifier.align(Alignment.Center),
                    size = 64.dp,
                    lineColor = Color.White
                )
            }
        }
    }

    protected open suspend fun onClipFinished(bm: Bitmap) {
        val uri = withContext(Dispatchers.IO) {
            bm.saveToLocal(PhotoHelper.getAppShareDir(this@PhotoClipperActivity.applicationContext))
        }
        setResult(
            RESULT_OK,
            Intent().apply {
                putExtra(PHOTO_CLIPPER_RESULT_WIDTH, bm.width)
                putExtra(PHOTO_CLIPPER_RESULT_HEIGHT, bm.height)
                putExtra(PHOTO_CLIPPER_RESULT_URI, uri)
            }
        )
        finish()
    }

    protected open fun onClipFailed() {
        finish()
    }
}
