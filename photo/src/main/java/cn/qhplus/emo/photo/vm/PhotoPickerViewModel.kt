package cn.qhplus.emo.photo.vm

import android.app.Application
import android.net.Uri
import androidx.annotation.Keep
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.qhplus.emo.core.EmoLog
import cn.qhplus.emo.core.LogTag
import cn.qhplus.emo.photo.activity.PHOTO_DEFAULT_PICK_LIMIT_COUNT
import cn.qhplus.emo.photo.activity.PHOTO_ENABLE_ORIGIN
import cn.qhplus.emo.photo.activity.PHOTO_PICKED_ITEMS
import cn.qhplus.emo.photo.activity.PHOTO_PICK_LIMIT_COUNT
import cn.qhplus.emo.photo.activity.PHOTO_PROVIDER_FACTORY
import cn.qhplus.emo.photo.activity.PhotoPickItemInfo
import cn.qhplus.emo.photo.data.MediaDataProvider
import cn.qhplus.emo.photo.data.MediaPhotoBucketAllId
import cn.qhplus.emo.photo.data.MediaPhotoBucketVO
import cn.qhplus.emo.photo.data.MediaPhotoProviderFactory
import cn.qhplus.emo.photo.data.MediaPhotoVO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhotoPickerData(
    val loading: Boolean,
    val data: List<MediaPhotoBucketVO>?,
    val error: Throwable? = null
)

class PhotoPickerViewModel @Keep constructor(
    val application: Application,
    val state: SavedStateHandle,
    val dataProvider: MediaDataProvider,
    val supportedMimeTypes: Array<String>
) : ViewModel(), LogTag {

    val pickLimitCount = state.get<Int>(PHOTO_PICK_LIMIT_COUNT) ?: PHOTO_DEFAULT_PICK_LIMIT_COUNT

    val enableOrigin = state.get<Boolean>(PHOTO_ENABLE_ORIGIN) ?: true

    private val photoProviderFactory: MediaPhotoProviderFactory

    private val _photoPickerDataFlow = MutableStateFlow(PhotoPickerData(true, null))
    val photoPickerDataFlow = _photoPickerDataFlow.asStateFlow()

    private val _pickedMap = mutableMapOf<Long, MediaPhotoVO>()
    private val _pickedListFlow = MutableStateFlow<List<Long>>(emptyList())
    val pickedListFlow = _pickedListFlow.asStateFlow()

    private val _pickedCountFlow = MutableStateFlow(0)
    val pickedCountFlow = _pickedCountFlow.asStateFlow()

    private val _isOriginOpenFlow = MutableStateFlow(false)
    val isOriginOpenFlow = _isOriginOpenFlow.asStateFlow()

    private val _finishFlow = MutableSharedFlow<List<PhotoPickItemInfo>?>()
    val finishFlow = _finishFlow.asSharedFlow()

    init {
        val photoProviderFactoryClsName =
            state.get<String>(PHOTO_PROVIDER_FACTORY) ?: throw RuntimeException("no MediaPhotoProviderFactory is provided.")
        photoProviderFactory = Class.forName(photoProviderFactoryClsName).newInstance() as MediaPhotoProviderFactory
    }

    fun loadData(){
        viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    dataProvider.provide(application, supportedMimeTypes).map { bucket ->
                        MediaPhotoBucketVO(bucket.id, bucket.name, bucket.list.map {
                            MediaPhotoVO(it, photoProviderFactory.factory(it))
                        })
                    }
                }
                val pickedItems = state.get<ArrayList<Uri>>(PHOTO_PICKED_ITEMS)
                if(pickedItems != null){
                    state[PHOTO_PICKED_ITEMS] = null
                    val map = mutableMapOf<Uri, Long>()
                    _pickedMap.clear()
                    data.find { it.id == MediaPhotoBucketAllId}?.list?.let {  list ->
                        for(element in list){
                            if(pickedItems.find { it == element.model.uri } != null) {
                                _pickedMap[element.model.id] = element
                                map[element.model.uri] = element.model.id
                            }
                            if(map.size == pickedItems.size){
                                break
                            }
                        }

                    }
                    // keep the order.
                    val list = pickedItems.mapNotNull {
                        map[it]
                    }
                    _pickedListFlow.value = list
                    _pickedCountFlow.value = list.size
                }

                _photoPickerDataFlow.value = PhotoPickerData(false, data)
            } catch (e: Throwable) {
                _photoPickerDataFlow.value = PhotoPickerData(false, null, e)
            }
        }
    }

    fun handleFinish(data: List<PhotoPickItemInfo>?){
        viewModelScope.launch {
            _finishFlow.emit(data)
        }
    }



    fun toggleOrigin(toOpen: Boolean) {
        _isOriginOpenFlow.value = toOpen
    }

    fun togglePick(item: MediaPhotoVO) {
        if (_photoPickerDataFlow.value.loading) {
            EmoLog.w(TAG, "pick when data is not finish loaded, please check why this method called here?")
            return
        }
        val list = arrayListOf<Long>()
        list.addAll(_pickedListFlow.value)
        if (list.contains(item.model.id)) {
            _pickedMap.remove(item.model.id)
            list.remove(item.model.id)
            _pickedListFlow.value = list
            _pickedCountFlow.value = list.size
        } else {
            if (list.size >= pickLimitCount) {
                EmoLog.w(TAG, "can not pick more photo, please check why this method called here?")
                return
            }
            _pickedMap[item.model.id] = item
            list.add(item.model.id)
            _pickedListFlow.value = list
            _pickedCountFlow.value = list.size
        }
    }

    fun getPickedVOList(): List<MediaPhotoVO>{
        return _pickedListFlow.value.mapNotNull { id ->
            _pickedMap[id]
        }
    }

    fun getPickedResultList(): List<PhotoPickItemInfo> {
        return _pickedListFlow.value.mapNotNull { id ->
            _pickedMap[id]?.model?.let {
                PhotoPickItemInfo(it.id, it.name, it.width, it.height, it.uri, it.rotation)
            }
        }
    }
}