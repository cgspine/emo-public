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

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
class ReportMsg(
    @ProtoNumber(1)
    val name: String,
    @ProtoNumber(2)
    val content: ByteArray
)

@Serializable
data class BasicMsgContent(
    @ProtoNumber(1)
    val time: Long = System.currentTimeMillis(),
    @ProtoNumber(2)
    val platform: String = "android"
)

@Serializable
data class WakeMsgContent(
    @ProtoNumber(1)
    val basic: BasicMsgContent = BasicMsgContent(),
    @ProtoNumber(1)
    val user: String
)

@Serializable
data class ClickMsgContent(
    @ProtoNumber(1)
    val basic: BasicMsgContent = BasicMsgContent(),
    @ProtoNumber(1)
    val target: String
)

fun newWakeMsg(user: String): ReportMsg {
    return ReportMsg("wake", ProtoBuf.encodeToByteArray(WakeMsgContent(user = user)))
}

fun newClickMsg(target: String): ReportMsg {
    return ReportMsg("click", ProtoBuf.encodeToByteArray(ClickMsgContent(target = target)))
}
