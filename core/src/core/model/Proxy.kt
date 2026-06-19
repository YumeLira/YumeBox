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
data class Proxy(
    val name: String,
    val title: String,
    val subtitle: String,
    val type: Type,
    val delay: Int,
    val isGroup: Boolean = type.group,
) : Parcelable {
    @Suppress("unused")
    enum class Type(val group: Boolean) {
        Direct(false),
        Reject(false),
        RejectDrop(false),
        Compatible(false),
        Pass(false),
        Shadowsocks(false),
        ShadowsocksR(false),
        Snell(false),
        Socks5(false),
        Http(false),
        Vmess(false),
        Vless(false),
        Trojan(false),
        Hysteria(false),
        Hysteria2(false),
        Tuic(false),
        WireGuard(false),
        Dns(false),
        Ssh(false),
        Mieru(false),
        AnyTLS(false),
        Sudoku(false),
        Masque(false),
        TrustTunnel(false),
        OpenVPN(false),
        Tailscale(false),
        GostRelay(false),
        Relay(true),
        Selector(true),
        Fallback(true),
        URLTest(true),
        LoadBalance(true),
        Smart(true),
        PassRule(false),
        Unknown(false),
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        Parcelizer.encodeToParcel(serializer(), parcel, this)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Proxy> {
        override fun createFromParcel(parcel: Parcel): Proxy {
            return Parcelizer.decodeFromParcel(serializer(), parcel)
        }

        override fun newArray(size: Int): Array<Proxy?> {
            return arrayOfNulls(size)
        }
    }
}

val Proxy.isProxyGroup: Boolean
    get() = isGroup || type.group

val Proxy.Type.isManuallySelectable: Boolean
    get() = this == Proxy.Type.Selector || this == Proxy.Type.URLTest || this == Proxy.Type.Fallback
