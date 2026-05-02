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
 * Copyright (c)  YumeLira 2025 - Present
 *
 */



package com.github.yumelira.yumebox.core.model

import android.os.Parcel
import android.os.Parcelable
import com.github.yumelira.yumebox.core.util.createListFromParcelSlice
import com.github.yumelira.yumebox.core.util.writeToParcelSlice
import kotlinx.serialization.Serializable

@Serializable
data class ProxyGroup(
    val name: String = "",
    val type: Proxy.Type,
    val proxies: List<Proxy>,
    val now: String,
    val icon: String? = null,
    val hidden: Boolean = false,
) : Parcelable {
    class SliceProxyList(proxies: List<Proxy>) : List<Proxy> by proxies, Parcelable {
        constructor(parcel: Parcel) : this(Proxy.createListFromParcelSlice(parcel, 0, 50))

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            writeToParcelSlice(dest, flags)
        }

        companion object CREATOR : Parcelable.Creator<SliceProxyList> {
            override fun createFromParcel(parcel: Parcel): SliceProxyList {
                return SliceProxyList(parcel)
            }

            override fun newArray(size: Int): Array<SliceProxyList?> {
                return arrayOfNulls(size)
            }
        }
    }

    constructor(parcel: Parcel) : this(
        type = Proxy.Type.entries[parcel.readInt()],
        proxies = SliceProxyList(parcel),
        now = parcel.readString().orEmpty(),
        icon = parcel.readString(),
        name = if (parcel.dataAvail() > 0) parcel.readString().orEmpty() else "",
        hidden = if (parcel.dataAvail() > 0) parcel.readByte().toInt() != 0 else false,
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(type.ordinal)
        SliceProxyList(proxies).writeToParcel(parcel, 0)
        parcel.writeString(now)
        parcel.writeString(icon)
        parcel.writeString(name)
        parcel.writeByte(if (hidden) 1.toByte() else 0.toByte())
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ProxyGroup> {
        override fun createFromParcel(parcel: Parcel): ProxyGroup {
            return ProxyGroup(parcel)
        }

        override fun newArray(size: Int): Array<ProxyGroup?> {
            return arrayOfNulls(size)
        }
    }
}
