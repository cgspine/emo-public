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

package cn.qhplus.emo.photo.coil

import cn.qhplus.emo.photo.data.MediaModel
import cn.qhplus.emo.photo.data.MediaPhotoProviderFactory
import cn.qhplus.emo.photo.data.PhotoProvider

class CoilMediaPhotoProviderFactory : MediaPhotoProviderFactory {

    override fun factory(model: MediaModel): PhotoProvider {
        return CoilPhotoProvider(uri = model.uri, ratio = model.ratio())
    }
}
