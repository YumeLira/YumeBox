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

@file:UseSerializers(DateSerializer::class)

package com.github.yumelira.yumebox.core.model

import android.os.Parcel
import android.os.Parcelable
import com.github.yumelira.yumebox.core.util.DateSerializer
import com.github.yumelira.yumebox.core.util.Parcelizer
import java.util.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class LogMessage(val level: Level, val message: String, val time: Date) : Parcelable {
    @Serializable
    enum class Level {
        @SerialName("debug") Debug,
        @SerialName("info") Info,
        @SerialName("warning") Warning,
        @SerialName("error") Error,
        @SerialName("silent") Silent,
        @SerialName("unknown") Unknown,
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        Parcelizer.encodeToParcel(serializer(), parcel, this)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @JvmField
        val CREATOR =
            object : Parcelable.Creator<LogMessage> {
                override fun createFromParcel(parcel: Parcel): LogMessage {
                    return Parcelizer.decodeFromParcel(serializer(), parcel)
                }

                override fun newArray(size: Int): Array<LogMessage?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
