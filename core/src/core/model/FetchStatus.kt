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
 * Copyright (c)  YumeYucca 2025 - Present
 *
 */

package com.github.yumelira.yumebox.core.model

import android.os.Parcel
import android.os.Parcelable
import com.github.yumelira.yumebox.core.util.Parcelizer
import kotlinx.serialization.Serializable

@Serializable
data class FetchStatus(
    val action: Action,
    val args: List<String>,
    val progress: Int,
    val max: Int,
    val subUpload: Long? = null,
    val subDownload: Long? = null,
    val subTotal: Long? = null,
    val subExpire: Long? = null,
    val subUpdateInterval: Long? = null,
    val subTitle: String? = null,
    val subFilename: String? = null,
) : Parcelable {
    enum class Action {
        FetchConfiguration,
        FetchProviders,
        SubscriptionInfo,
        Verifying,
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        Parcelizer.encodeToParcel(serializer(), dest, this)
    }

    companion object CREATOR : Parcelable.Creator<FetchStatus> {
        override fun createFromParcel(parcel: Parcel): FetchStatus {
            return Parcelizer.decodeFromParcel(serializer(), parcel)
        }

        override fun newArray(size: Int): Array<FetchStatus?> {
            return arrayOfNulls(size)
        }
    }
}
