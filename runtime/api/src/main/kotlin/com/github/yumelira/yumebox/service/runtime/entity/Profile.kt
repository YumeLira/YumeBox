@file:UseSerializers(UUIDSerializer::class)

package com.github.yumelira.yumebox.service.runtime.entity

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import com.github.yumelira.yumebox.core.util.Parcelizer
import com.github.yumelira.yumebox.service.runtime.util.UUIDSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.util.*

/**
 * Profile data class for YumeBox
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Profile(
    val uuid: UUID,
    val name: String,
    val type: Type,
    val source: String,
    val active: Boolean,
    val interval: Long,
    val upload: Long,
    val download: Long,
    val total: Long,
    val expire: Long,
    val updatedAt: Long,
    val imported: Boolean,
    val pending: Boolean,
) : Parcelable {
    enum class Type {
        File, Url, External
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        Parcelizer.encodeToParcel(serializer(), parcel, this)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Profile> {
        override fun createFromParcel(parcel: Parcel): Profile {
            return Parcelizer.decodeFromParcel(serializer(), parcel)
        }

        override fun newArray(size: Int): Array<Profile?> {
            return arrayOfNulls(size)
        }
    }
}