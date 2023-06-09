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

package cn.qhplus.emo.ui.page

import android.app.Activity.RESULT_OK
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import cn.qhplus.emo.EmoKvInstance
import cn.qhplus.emo.MainActivity
import cn.qhplus.emo.config.SchemeConst
import cn.qhplus.emo.config.runQuietly
import cn.qhplus.emo.config.schemeBuilder
import cn.qhplus.emo.fs.fileName
import cn.qhplus.emo.photo.activity.PhotoClipperActivity
import cn.qhplus.emo.photo.activity.PhotoClipperResult
import cn.qhplus.emo.photo.activity.PhotoPickResult
import cn.qhplus.emo.photo.activity.PhotoPickerActivity
import cn.qhplus.emo.photo.activity.PhotoViewerActivity
import cn.qhplus.emo.photo.activity.getPhotoClipperResult
import cn.qhplus.emo.photo.activity.getPhotoPickResult
import cn.qhplus.emo.photo.coil.CoilMediaPhotoProviderFactory
import cn.qhplus.emo.photo.coil.CoilPhotoProvider
import cn.qhplus.emo.photo.data.PhotoShot
import cn.qhplus.emo.photo.pdf.BundlePdfDataSourceFactory
import cn.qhplus.emo.photo.pdf.PdfActivity
import cn.qhplus.emo.photo.pdf.PdfDataSource
import cn.qhplus.emo.photo.pdf.PdfPhotoProvider
import cn.qhplus.emo.photo.pdf.PdfThumbPhoto
import cn.qhplus.emo.photo.ui.PhotoThumbnailWithViewer
import cn.qhplus.emo.photo.ui.edit.EditLayer
import cn.qhplus.emo.photo.ui.edit.EditLayerDeserializeFactory
import cn.qhplus.emo.scheme.ComposeScheme
import cn.qhplus.emo.ui.CommonItem
import coil.compose.AsyncImage
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber

@ComposeScheme(
    action = SchemeConst.SCHEME_ACTION_PHOTO,
    alternativeHosts = [MainActivity::class]
)
@Composable
fun PhotoPage() {
    OnlyBackListPage(
        title = "Photo"
    ) {
        item {
            CommonItem("Photo Viewer") {
                schemeBuilder(SchemeConst.SCHEME_ACTION_PHOTO_VIEWER).runQuietly()
            }
        }

        item {
            CommonItem("Photo Picker") {
                schemeBuilder(SchemeConst.SCHEME_ACTION_PHOTO_PICKER).runQuietly()
            }
        }

        item {
            CommonItem("Photo Clipper") {
                schemeBuilder(SchemeConst.SCHEME_ACTION_PHOTO_CLIPPER).runQuietly()
            }
        }

        item {
            val context = LocalContext.current
            val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetMultipleContents(), onResult = { uris ->
                if(uris.isNotEmpty()){
                    val photoProviders = uris.map {
                        PhotoShot(
                            PdfPhotoProvider(it),
                            null,
                            null,
                            null
                        )
                    }
                    val intent = PhotoViewerActivity.intentOf(context, PhotoViewerActivity::class.java, photoProviders, 0)
                    context.startActivity(intent)
                }
            })
            CommonItem("PDF Multi Viewer") {
                launcher.launch("application/pdf")
            }
        }

        item {
            val context = LocalContext.current
            val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent(), onResult = {uri ->
                if(uri != null){
                    val bundle = Bundle().apply {
                        putParcelable("uri", uri)
                    }
                    val intent = PdfActivity.intentOf(context, PdfActivity::class.java, bundle, TestBundlePdfDataSourceFactory::class.java)
                    context.startActivity(intent)
                }
            })
            CommonItem("PDF Reader With Editing") {
                launcher.launch("application/pdf")
            }
        }

        item {
            val state = remember {
                mutableStateOf<PdfThumbPhoto?>(null)
            }
            val context = LocalContext.current
            val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent(), onResult = {uri ->
                if(uri != null){
                    state.value = PdfThumbPhoto(TestPdfDataSource(context.applicationContext, uri))
                }
            })
            Column {
                CommonItem("PDF Reader Thumb") {
                    launcher.launch("application/pdf")
                }
                Box(modifier = Modifier.width(100.dp).height(141.dp)) {
                    state.value?.Compose(
                        contentScale = ContentScale.Fit,
                        isContainerDimenExactly = true,
                        onSuccess = null,
                        onError = null
                    )
                }

            }

        }
    }
}

@ComposeScheme(
    action = SchemeConst.SCHEME_ACTION_PHOTO_VIEWER,
    alternativeHosts = [MainActivity::class]
)
@Composable
fun PhotoViewerPage() {
    OnlyBackListPage(
        title = "Photo Viewer"
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                PhotoThumbnailWithViewer(
                    images = persistentListOf(
                        CoilPhotoProvider(
                            "https://weread-picture-1258476243.file.myqcloud.com/9979/31y68oGufDGL3zQ6TT.jpg".toUri(),
                            ratio = 1f
                        )
                    )
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                PhotoThumbnailWithViewer(
                    images = persistentListOf(
                        CoilPhotoProvider(
                            "https://weread-picture-1258476243.file.myqcloud.com/9136/1yn0KLFwy6Vb0nE6Sg.png".toUri(),
                            ratio = 1.379f
                        )
                    )
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                PhotoThumbnailWithViewer(
                    images = persistentListOf(
                        CoilPhotoProvider(
                            "file:///android_asset/test.png".toUri(),
                            ratio = 0.0125f
                        )
                    )
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                PhotoThumbnailWithViewer(
                    images = persistentListOf(
                        CoilPhotoProvider(
                            "https://weread-picture-1258476243.file.myqcloud.com/8779/6WY7guGLeGfp0KK6Sb.jpeg".toUri(),
                            ratio = 0.749f
                        )
                    )
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                PhotoThumbnailWithViewer(
                    images = persistentListOf(
                        CoilPhotoProvider(
                            "https://weread-picture-1258476243.file.myqcloud.com/8779/6WY7guGLeGfp0KK6Sb.jpeg".toUri(),
                            ratio = 0.749f
                        ),
                        CoilPhotoProvider(
                            "https://weread-picture-1258476243.file.myqcloud.com/9136/1yn0KLFwy6Vb0nE6Sg.png".toUri(),
                            ratio = 1.379f
                        )
                    )
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                PhotoThumbnailWithViewer(
                    images = persistentListOf(
                        CoilPhotoProvider(
                            "https://weread-picture-1258476243.file.myqcloud.com/8779/6WY7guGLeGfp0KK6Sb.jpeg".toUri(),
                            ratio = 0.749f
                        ),
                        CoilPhotoProvider(
                            "https://weread-picture-1258476243.file.myqcloud.com/9136/1yn0KLFwy6Vb0nE6Sg.png".toUri(),
                            ratio = 1.379f
                        ),
                        CoilPhotoProvider(
                            "https://weread-picture-1258476243.file.myqcloud.com/9979/31y68oGufDGL3zQ6TT.jpg".toUri(),
                            ratio = 1f
                        )
                    )
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                PhotoThumbnailWithViewer(
                    images = persistentListOf(
                        CoilPhotoProvider(
                            "https://weread-picture-1258476243.file.myqcloud.com/8779/6WY7guGLeGfp0KK6Sb.jpeg".toUri(),
                            ratio = 0.749f
                        ),
                        CoilPhotoProvider(
                            "https://weread-picture-1258476243.file.myqcloud.com/9136/1yn0KLFwy6Vb0nE6Sg.png".toUri(),
                            ratio = 1.379f
                        ),
                        CoilPhotoProvider(
                            "https://weread-picture-1258476243.file.myqcloud.com/9979/31y68oGufDGL3zQ6TT.jpg".toUri(),
                            ratio = 1f
                        ),
                        CoilPhotoProvider(
                            "https://weread-picture-1258476243.file.myqcloud.com/8779/6WY7guGLeGfp0KK6Sb.jpeg".toUri(),
                            ratio = 0.749f
                        ),
                        CoilPhotoProvider(
                            "https://weread-picture-1258476243.file.myqcloud.com/9136/1yn0KLFwy6Vb0nE6Sg.png".toUri(),
                            ratio = 1.379f
                        )
                    )
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                PhotoThumbnailWithViewer(
                    images = persistentListOf(
                        CoilPhotoProvider(
                            "https://weread-picture-1258476243.file.myqcloud.com/8779/6WY7guGLeGfp0KK6Sb.jpeg".toUri(),
                            ratio = 0.749f
                        ),
                        CoilPhotoProvider(
                            "https://weread-picture-1258476243.file.myqcloud.com/9136/1yn0KLFwy6Vb0nE6Sg.png".toUri(),
                            ratio = 1.379f
                        ),
                        CoilPhotoProvider(
                            "https://weread-picture-1258476243.file.myqcloud.com/9979/31y68oGufDGL3zQ6TT.jpg".toUri(),
                            ratio = 1f
                        ),
                        CoilPhotoProvider(
                            "https://weread-picture-1258476243.file.myqcloud.com/8779/6WY7guGLeGfp0KK6Sb.jpeg".toUri(),
                            ratio = 0.749f
                        ),
                        CoilPhotoProvider(
                            "https://weread-picture-1258476243.file.myqcloud.com/9136/1yn0KLFwy6Vb0nE6Sg.png".toUri(),
                            ratio = 1.379f
                        ),
                        CoilPhotoProvider(
                            "https://weread-picture-1258476243.file.myqcloud.com/8779/6WY7guGLeGfp0KK6Sb.jpeg".toUri(),
                            ratio = 0.749f
                        ),
                        CoilPhotoProvider(
                            "https://weread-picture-1258476243.file.myqcloud.com/9136/1yn0KLFwy6Vb0nE6Sg.png".toUri(),
                            ratio = 1.379f
                        ),
                        CoilPhotoProvider(
                            "https://weread-picture-1258476243.file.myqcloud.com/8779/6WY7guGLeGfp0KK6Sb.jpeg".toUri(),
                            ratio = 0.749f
                        ),
                        CoilPhotoProvider(
                            "https://weread-picture-1258476243.file.myqcloud.com/9979/31y68oGufDGL3zQ6TT.jpg".toUri(),
                            ratio = 1f
                        )
                    )
                )
            }
        }
    }
}


@ComposeScheme(
    action = SchemeConst.SCHEME_ACTION_PHOTO_PICKER,
    alternativeHosts = [MainActivity::class]
)
@Composable
fun PhotoPickerPage() {
    val pickResult = remember {
        mutableStateOf<PhotoPickResult?>(null)
    }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            it.data?.getPhotoPickResult()?.let { ret ->
                pickResult.value = ret
            }
        }
    }

    OnlyBackListPage(
        title = "Photo Viewer"
    ) {
        item(key = "pick-photo") {
            val context = LocalContext.current
            CommonItem("Pick Photo") {
                pickLauncher.launch(
                    PhotoPickerActivity.intentOf(
                        context,
                        CoilMediaPhotoProviderFactory::class.java,
                        pickedItems = arrayListOf<Uri>().apply { pickResult.value?.list?.mapTo(this) { it.uri } }
                    )
                )
            }
        }

        val result = pickResult.value
        if (result != null && result.list.isNotEmpty()) {
            item(key = result.list.map { it.id }.joinToString(",")) {
                val images = remember(pickResult) {
                    result.list.map {
                        CoilPhotoProvider(
                            it.uri,
                            ratio = it.ratio()
                        )
                    }.toPersistentList()
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Text(text = "原图：${result.isOriginOpen}")

                    PhotoThumbnailWithViewer(
                        images = images
                    )
                }
            }
        }
    }
}

@ComposeScheme(
    action = SchemeConst.SCHEME_ACTION_PHOTO_CLIPPER,
    alternativeHosts = [MainActivity::class]
)
@Composable
fun PhotoClipperPage() {
    val clipperResult = remember {
        mutableStateOf<PhotoClipperResult?>(null)
    }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            it.data?.getPhotoClipperResult()?.let { ret ->
                clipperResult.value = ret
            }
        }
    }

    OnlyBackListPage(
        title = "Photo Clipper"
    ) {
        item(key = "clipper-photo") {
            val context = LocalContext.current
            CommonItem("Clip Photo") {
                pickLauncher.launch(
                    PhotoClipperActivity.intentOf(
                        context,
                        CoilPhotoProvider(
                            "https://weread-picture-1258476243.file.myqcloud.com/9979/31y68oGufDGL3zQ6TT.jpg".toUri(),
                            ratio = 0f
                        )
                    )
                )
            }
        }

        val result = clipperResult.value
        if (result != null) {
            item(key = result.uri.toString()) {
                AsyncImage(model = result.uri, contentDescription = "")
            }
        }
    }
}

class TestPdfDataSource(val context: Context, val uri: Uri): PdfDataSource {

    override val title: State<String>
        get() = mutableStateOf(uri.fileName(context) ?: "")


    override fun readInitIndex(context: Context): Int {
        return EmoKvInstance.getInt("pdf_index_${uri}", 0)
    }

    override fun readInitOffset(context: Context): Int {
        return EmoKvInstance.getInt("pdf_offset_${uri}", 0)
    }

    override suspend fun getFileDescriptor(context: Context): ParcelFileDescriptor? {
        return context.contentResolver.openFileDescriptor(uri, "r")
    }

    override suspend fun saveIndexAndOffset(index: Int, offset: Int) {
        EmoKvInstance.put("pdf_index_${uri}", index)
        EmoKvInstance.put("pdf_offset_${uri}", offset)
    }

    override fun supportEdit(page: Int): Boolean {
        return true
    }

    override suspend fun saveEditLayers(page: Int, layers: PersistentList<EditLayer>) {
        EmoKvInstance.put("pdf_editing_${uri}_${page}".toByteArray(), layers.map { it.serialize() }.let {
            ProtoBuf.encodeToByteArray(EditLayersStoreInfo(it))
        })
    }

    override suspend fun loadEditLayers(page: Int): PersistentList<EditLayer> {
        return EmoKvInstance.get("pdf_editing_${uri}_${page}".toByteArray())?.let{
            ProtoBuf.decodeFromByteArray<EditLayersStoreInfo>(it).list.mapNotNull { value ->
                EditLayerDeserializeFactory.deserialize(value)
            }
        }?.toPersistentList() ?: persistentListOf()
    }
}

@Serializable
class EditLayersStoreInfo(
    @ProtoNumber(1)
    val list: List<ByteArray>
)

class TestBundlePdfDataSourceFactory: BundlePdfDataSourceFactory {
    override fun factory(context: Context, bundle: Bundle): PdfDataSource {
        val uri = bundle.getParcelable<Uri>("uri") ?: throw RuntimeException("uri is not provided")
        return TestPdfDataSource(context, uri)
    }

}