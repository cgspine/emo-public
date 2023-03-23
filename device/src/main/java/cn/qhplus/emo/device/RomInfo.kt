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

package cn.qhplus.emo.device

import android.os.Build

enum class RomType {
    Xiaomi,
    Meizu,
    Huawei,
    Honor,
    Oppo,
    Vivo,
    Samsung,
    NotCare
}

val romModel by lazy { Build.MODEL }
val romBrand by lazy { Build.BRAND }
val romManufacturer by lazy { Build.MANUFACTURER }

private val romTypeCacheRunner by lazy {
    CacheRunner<Unit, RomType> {
        val manufacturer = romManufacturer.lowercase()
        val brand = romBrand.lowercase()
        if (isRom(brand, manufacturer, "xiaomi")) {
            RomType.Xiaomi
        } else if (isRom(brand, manufacturer, "samsung")) {
            RomType.Samsung
        } else if (isRom(brand, manufacturer, "vivo")) {
            RomType.Vivo
        } else if (isRom(brand, manufacturer, "oppo")) {
            RomType.Oppo
        } else if (isRom(brand, manufacturer, "huawei")) {
            RomType.Huawei
        } else if (isRom(brand, manufacturer, "honor")) {
            RomType.Honor
        } else if (isRom(brand, manufacturer, "meizu")) {
            RomType.Meizu
        } else {
            RomType.NotCare
        }
    }
}

private fun isRom(brand: String, manufacturer: String, name: String): Boolean {
    return brand.contains(name) || manufacturer.contains(name)
}

fun getRomType() = romTypeCacheRunner.get(Unit) ?: RomType.NotCare
fun isXiaomi() = getRomType() == RomType.Xiaomi
fun isHuawei() = getRomType() == RomType.Huawei
fun isHonor() = getRomType() == RomType.Honor
fun isOppo() = getRomType() == RomType.Oppo
fun isVivo() = getRomType() == RomType.Vivo
fun isMeizu() = getRomType() == RomType.Meizu
fun isSamsung() = getRomType() == RomType.Samsung
