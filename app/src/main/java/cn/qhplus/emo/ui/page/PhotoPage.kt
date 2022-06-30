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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import cn.qhplus.emo.photo.coil.CoilPhotoProvider
import cn.qhplus.emo.photo.ui.PhotoThumbnailWithViewer
import cn.qhplus.emo.ui.CommonItem
import cn.qhplus.emo.ui.RouteConst

@Composable
fun PhotoPage(navController: NavHostController) {
    OnlyBackListPage(
        navController = navController,
        title = "Photo"
    ) {
        item {
            CommonItem("Photo Viewer") {
                navController.navigate(RouteConst.ROUTE_PHOTO_VIEWER)
            }
        }

        item {
            CommonItem("Photo Picker") {
            }
        }
    }
}

@Composable
fun PhotoViewerPage(navController: NavHostController) {
    OnlyBackListPage(
        navController = navController,
        title = "Photo Viewer"
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                PhotoThumbnailWithViewer(
                    images = listOf(
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
                    images = listOf(
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
                    images = listOf(
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
                    images = listOf(
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
                    images = listOf(
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
                    images = listOf(
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
                    images = listOf(
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
                    images = listOf(
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
                        ),
                    )
                )
            }
        }
    }
}
