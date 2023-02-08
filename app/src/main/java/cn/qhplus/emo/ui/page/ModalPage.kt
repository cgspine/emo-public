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

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import cn.qhplus.emo.MainActivity
import cn.qhplus.emo.config.SchemeConst
import cn.qhplus.emo.modal.EmoBottomSheetList
import cn.qhplus.emo.modal.EmoDialogActions
import cn.qhplus.emo.modal.EmoDialogList
import cn.qhplus.emo.modal.EmoDialogMarkList
import cn.qhplus.emo.modal.EmoDialogMsg
import cn.qhplus.emo.modal.EmoDialogMutiCheckList
import cn.qhplus.emo.modal.EmoModalAction
import cn.qhplus.emo.modal.TipStatus
import cn.qhplus.emo.modal.emoBottomSheet
import cn.qhplus.emo.modal.emoDialog
import cn.qhplus.emo.modal.emoTip
import cn.qhplus.emo.modal.emoToast
import cn.qhplus.emo.scheme.ComposeScheme
import cn.qhplus.emo.theme.EmoTheme
import cn.qhplus.emo.ui.CommonItem
import cn.qhplus.emo.ui.core.Item
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@ComposeScheme(
    action = SchemeConst.SCHEME_ACTION_MODAL,
    alternativeHosts = [MainActivity::class]
)
@Composable
fun ModalPage() {
    val view = LocalView.current
    OnlyBackListPage(
        title = "Modal"
    ) {
        item {
            CommonItem("消息类型对话框") {
                view.emoDialog { modal ->
                    EmoTheme {
                        EmoDialogMsg(
                            modal,
                            "这是标题",
                            "这是一丢丢有趣但是没啥用的内容",
                            listOf(
                                EmoModalAction("取 消", MaterialTheme.colorScheme.primary) {
                                    it.dismiss()
                                },
                                EmoModalAction("确 定", MaterialTheme.colorScheme.primary) {
                                    view.emoToast("确定啦!!!")
                                    it.dismiss()
                                }
                            )
                        )
                    }
                }.show()
            }
        }

        item {
            CommonItem("列表类型对话框") {
                view.emoDialog { modal ->
                    EmoDialogList(modal, maxHeight = 500.dp) {
                        items(200) { index ->
                            Item(title = "第${index + 1}项") {
                                view.emoToast("你点了第${index + 1}项")
                            }
                        }
                    }
                }.show()
            }
        }

        item {
            CommonItem("单选类型浮层") {
                view.emoDialog { modal ->
                    val list = remember {
                        val items = arrayListOf<String>()
                        for (i in 0 until 500) {
                            items.add("Item $i")
                        }
                        items
                    }
                    var markIndex by remember {
                        mutableStateOf(20)
                    }
                    EmoDialogMarkList(
                        modal,
                        maxHeight = 500.dp,
                        list = list,
                        markIndex = markIndex
                    ) { _, index ->
                        markIndex = index
                        view.emoToast("你点了第${index + 1}项")
                    }
                }.show()
            }
        }

        item {
            CommonItem("多选类型浮层") {
                view.emoDialog { modal ->
                    EmoTheme {
                        val list = remember {
                            val items = arrayListOf<String>()
                            for (i in 0 until 500) {
                                items.add("Item $i")
                            }
                            items
                        }
                        val checked = remember {
                            mutableStateListOf(0, 5, 10, 20)
                        }
                        val disable = remember {
                            mutableStateListOf(5, 10)
                        }
                        Column {
                            EmoDialogMutiCheckList(
                                modal,
                                maxHeight = 500.dp,
                                list = list,
                                checked = checked.toSet(),
                                disabled = disable.toSet()
                            ) { _, index ->
                                if (checked.contains(index)) {
                                    checked.remove(index)
                                } else {
                                    checked.add(index)
                                }
                            }
                            EmoDialogActions(
                                modal = modal,
                                actions = listOf(
                                    EmoModalAction("取 消", MaterialTheme.colorScheme.primary) {
                                        it.dismiss()
                                    },
                                    EmoModalAction("确 定", MaterialTheme.colorScheme.primary) {
                                        view.emoToast("你选择了: ${checked.joinToString(",")}")
                                        it.dismiss()
                                    }
                                )
                            )
                        }
                    }
                }.show()
            }
        }

        item {
            CommonItem("Toast") {
                view.emoToast("这只是个 Toast!")
            }
        }

        item {
            CommonItem("BottomSheet(list)") {
                view.emoBottomSheet {
                    EmoBottomSheetList(it) {
                        items(200) { index ->
                            Item(title = "第${index + 1}项") {
                                view.emoToast("你点了第${index + 1}项")
                            }
                        }
                    }
                }.show()
            }
        }

        item {
            val scope = rememberCoroutineScope()
            CommonItem("Tip - Done") {
                val flow = MutableStateFlow<TipStatus>(TipStatus.Loading())
                val tip = view.emoTip(status = flow).show()
                scope.launch {
                    delay(1000)
                    flow.value = TipStatus.Done()
                    delay(100)
                    tip.dismiss()
                }
            }
        }
        item {
            val scope = rememberCoroutineScope()
            CommonItem("Tip - Error") {
                val flow = MutableStateFlow<TipStatus>(TipStatus.Loading())
                val tip = view.emoTip(status = flow).show()
                scope.launch {
                    delay(1000)
                    flow.value = TipStatus.Error()
                    delay(100)
                    tip.dismiss()
                }
            }
        }
    }
}
