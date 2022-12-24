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

package cn.qhplus.emo.report

import android.util.Log
import cn.qhplus.emo.EmoApp
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf

object AppListReportTransporter : ListReportTransporter<ReportMsg> {

    override suspend fun transport(client: ReportClient<ReportMsg>, batch: List<ReportMsg>, usedStrategy: ReportStrategy): Boolean {
        batch.asSequence().forEach {
            if (it.name == "wake") {
                Log.i("AppReport", "wake: ${ProtoBuf.decodeFromByteArray<WakeMsgContent>(it.content)}")
            } else if (it.name == "click") {
                Log.i("AppReport", "click: ${ProtoBuf.decodeFromByteArray<ClickMsgContent>(it.content)}")
            }
        }
        return true
    }
}

object ReportProtoBufMsgConverter : ReportMsgConverter<ReportMsg> {

    override fun encode(content: ReportMsg): ByteArray {
        return ProtoBuf.encodeToByteArray(content)
    }

    override fun decode(content: ByteArray): ReportMsg {
        return ProtoBuf.decodeFromByteArray(content)
    }
}

val reportClient by lazy {
    newReportClient(
        context = EmoApp.instance,
        listReportTransporter = writeBackIfFailed(AppListReportTransporter),
        converter = ReportProtoBufMsgConverter,
        fileBatchFileSize = 300 // for test
    )
}

fun reportWake(user: String) {
    reportClient.report(newWakeMsg(user), ReportStrategy.Immediately)
}

fun reportClick(target: String) {
    reportClient.report(newClickMsg(target), ReportStrategy.FileBatch)
}
