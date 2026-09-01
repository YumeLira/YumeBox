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

@file:Suppress("UnusedSymbol")

package com.github.yumeyucca.yumebox.core.model


enum class GeoFileType {
    GeoIP,
    GeoSite,
    Country,
    ASN,
    Model
}

data class GeoXItem(
    val type: GeoFileType,
    val title: String,
    val url: String,
    val fileName: String,
)

val geoXItems =
    listOf(
        GeoXItem(
            GeoFileType.GeoIP,
            "GeoIP.dat",
            "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/geoip.dat",
            "geoip.dat",
        ),
        GeoXItem(
            GeoFileType.GeoSite,
            "GeoSite.dat",
            "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/geosite.dat",
            "geosite.dat",
        ),
        GeoXItem(
            GeoFileType.Country,
            "Country.mmdb",
            "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/country.mmdb",
            "country.mmdb",
        ),
        GeoXItem(
            GeoFileType.ASN,
            "ASN.mmdb",
            "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/GeoLite2-ASN.mmdb",
            "ASN.mmdb",
        ),
        GeoXItem(
            GeoFileType.Model,
            "Model.bin",
            "https://github.com/vernesong/mihomo/releases/download/LightGBM-Model/Model-large.bin",
            "Model.bin"
        )
    )
