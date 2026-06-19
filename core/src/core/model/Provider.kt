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
data class Provider(
    val name: String,
    val type: Type,
    val vehicleType: VehicleType,
    val updatedAt: Long,
    val path: String = "",
) : Parcelable, Comparable<Provider> {
    enum class Type {
        Proxy,
        Rule,
    }

    enum class VehicleType {
        HTTP,
        File,
        Inline,
        Compatible,
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        Parcelizer.encodeToParcel(serializer(), parcel, this)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun compareTo(other: Provider): Int {
        return compareValuesBy(this, other, Provider::type, Provider::name)
    }

    companion object CREATOR : Parcelable.Creator<Provider> {
        override fun createFromParcel(parcel: Parcel): Provider {
            return Parcelizer.decodeFromParcel(serializer(), parcel)
        }

        override fun newArray(size: Int): Array<Provider?> {
            return arrayOfNulls(size)
        }
    }
}
