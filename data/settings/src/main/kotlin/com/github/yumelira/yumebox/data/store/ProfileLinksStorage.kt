/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c)  YumeLira 2025.
 *
 */

package com.github.yumelira.yumebox.data.store

import com.tencent.mmkv.MMKV
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ProfileLink(
    val id: String,
    val name: String,
    val url: String
)

enum class LinkOpenMode {
    IN_APP,
    EXTERNAL_BROWSER
}

class ProfileLinksStorage(externalMmkv: MMKV) : MMKVPreference(externalMmkv = externalMmkv) {

    private val json = Json { ignoreUnknownKeys = true }

    val linkOpenMode by enumFlow(LinkOpenMode.IN_APP)

    val links by jsonListFlow(
        default = emptyList(),
        decode = { str -> decodeFromString<List<ProfileLink>>(str) },
        encode = { value -> encodeToString(value) }
    )
    
    val defaultLinkId by strFlow(default = "")
}
