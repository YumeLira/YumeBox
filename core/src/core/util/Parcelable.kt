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



package com.github.yumelira.yumebox.core.util

import android.os.Binder
import android.os.Parcel
import android.os.Parcelable

private class SliceParcelableListBpBinder(val items: List<Parcelable>, val flags: Int) : Binder() {
    override fun onTransact(code: Int, request: Parcel, reply: Parcel?, transactionFlags: Int): Boolean {
        when (code) {
            TRANSACTION_GET_ITEMS -> {
                reply ?: return false

                val offset = request.readInt()
                val chunk = request.readInt()

                val end = (offset + chunk).coerceAtMost(items.size)

                reply.writeInt(end - offset)

                for (i in offset until end) {
                    items[i].writeToParcel(reply, flags)
                }

                return true
            }
        }

        return super.onTransact(code, request, reply, transactionFlags)
    }

    companion object {
        const val TRANSACTION_GET_ITEMS = 10
    }
}

fun <T : Parcelable> List<T>.writeToParcelSlice(parcel: Parcel, flags: Int) {
    val bp = SliceParcelableListBpBinder(this, flags)

    parcel.writeInt(size)
    parcel.writeStrongBinder(bp)
}

fun <T : Parcelable> Parcelable.Creator<T>.createListFromParcelSlice(
    parcel: Parcel,
    flags: Int,
    chunk: Int,
): List<T> {
    val total = parcel.readInt()
    val remote = parcel.readStrongBinder()
    val result = ArrayList<T>(total)

    var offset = 0

    while (offset < total) {
        val request = Parcel.obtain()
        val reply = Parcel.obtain()

        try {
            request.writeInt(offset)
            request.writeInt(chunk)

            if (!remote.transact(
                    SliceParcelableListBpBinder.TRANSACTION_GET_ITEMS,
                    request,
                    reply,
                    flags,
                )
            ) {
                break
            }

            val size = reply.readInt()

            repeat(size) {
                result.add(createFromParcel(reply))
            }

            offset += size

            if (size == 0)
                break
        } finally {
            request.recycle()
            reply.recycle()
        }
    }

    return result
}
