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

package com.github.yumelira.yumebox.core.util

import android.content.Context
import java.io.File

const val RUNTIME_HOME_DIR_NAME = "mihomo"
const val LEGACY_RUNTIME_HOME_DIR_NAME = "clash"
const val PROFILE_PROVIDERS_DIR_NAME = "providers"
const val RULE_PROVIDER_SCOPE = "rules"
const val PROXY_PROVIDER_SCOPE = "proxies"
const val ACG_ASSETS_DIR_NAME = "acg"
const val ACG_WALLPAPER_FILE_NAME = "wallpaper.dat"

val Context.runtimeHomeDir: File
    get() = filesDir.resolve(RUNTIME_HOME_DIR_NAME)

val Context.acgAssetsDir: File
    get() = filesDir.resolve(ACG_ASSETS_DIR_NAME)

/**
 * Stable app-private slot for the locally copied ACG wallpaper. The directory is created on access so
 * callers can stream-copy into the returned file directly.
 */
fun Context.acgWallpaperFile(): File =
    acgAssetsDir.apply { mkdirs() }.resolve(ACG_WALLPAPER_FILE_NAME)

val Context.legacyRuntimeHomeDir: File
    get() = filesDir.resolve(LEGACY_RUNTIME_HOME_DIR_NAME)

fun profileProvidersRoot(profileDir: File): File {
    return profileDir.resolve(PROFILE_PROVIDERS_DIR_NAME)
}

fun profileProviderScopeDir(profileDir: File, scope: String): File {
    return profileProvidersRoot(profileDir).resolve(scope)
}
