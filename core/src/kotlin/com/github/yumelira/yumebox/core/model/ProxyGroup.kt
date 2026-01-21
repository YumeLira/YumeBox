package com.github.yumelira.yumebox.core.model

import android.os.Parcel
import android.os.Parcelable
import com.github.yumelira.yumebox.core.util.createListFromParcelSlice
import com.github.yumelira.yumebox.core.util.writeToParcelSlice
import kotlinx.serialization.Serializable

@Serializable
data class ProxyGroup(
    val type: Proxy.Type,
    val proxies: List<Proxy>,
    val now: String,
    val icon: String? = null,
) : Parcelable {
    class SliceProxyList(data: List<Proxy>) : List<Proxy> by data, Parcelable {
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
        Proxy.Type.entries[parcel.readInt()],
        SliceProxyList(parcel),
        parcel.readString()!!,
        parcel.readString(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(type.ordinal)
        SliceProxyList(proxies).writeToParcel(parcel, 0)
        parcel.writeString(now)
        parcel.writeString(icon)
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
