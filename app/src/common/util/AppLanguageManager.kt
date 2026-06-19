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

package com.github.yumelira.yumebox.common.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.github.yumelira.yumebox.data.model.AppLanguage
import dev.oom_wg.purejoy.mlang.MLang
import java.util.Locale

object AppLanguageManager {

    @Volatile private var activeLanguage: AppLanguage = AppLanguage.System

    @Volatile private var activeLocale: Locale = Locale.getDefault()

    fun apply(language: AppLanguage) {
        activeLanguage = language
        val locale = resolveLocale(language)
        activeLocale = locale

        AppCompatDelegate.setApplicationLocales(
            when (language) {
                AppLanguage.System -> LocaleListCompat.getEmptyLocaleList()
                AppLanguage.Zh -> LocaleListCompat.forLanguageTags("zh-Hans")
                AppLanguage.En -> LocaleListCompat.forLanguageTags("en")
            }
        )

        Locale.setDefault(locale)
        LocaleUtil.setCurrentLocale(locale)
        MLang.updateLocale(locale)
    }

    fun wrap(base: Context): Context {
        val configuration = Configuration(base.resources.configuration)
        applyLocale(configuration, activeLocale)
        return base.createConfigurationContext(configuration)
    }

    fun refreshSystemLanguage() {
        if (activeLanguage == AppLanguage.System) {
            apply(AppLanguage.System)
        }
    }

    private fun resolveLocale(language: AppLanguage): Locale {
        return when (language) {
            AppLanguage.System -> systemLocale()
            AppLanguage.Zh -> Locale.SIMPLIFIED_CHINESE
            AppLanguage.En -> Locale.ENGLISH
        }
    }

    private fun systemLocale(): Locale {
        val resources = Resources.getSystem()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            resources.configuration.locales[0] ?: Locale.getDefault()
        } else {
            @Suppress("DEPRECATION")
            resources.configuration.locale ?: Locale.getDefault()
        }
    }

    private fun applyLocale(configuration: Configuration, locale: Locale) {
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
    }
}
