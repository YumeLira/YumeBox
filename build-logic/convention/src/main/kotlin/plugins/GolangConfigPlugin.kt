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
 * Copyright (c) YumeYuka & YumeLira 2025.
 *
 */

package plugins

import core.ConfigProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

class GolangConfigPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val provider = ConfigProvider(target)
        val ext = target.extensions.create<GolangExtension>("golang")
        configureDefaults(ext, target, provider)
    }

    private fun configureDefaults(
        extension: GolangExtension,
        project: Project,
        provider: ConfigProvider,
    ) = extension.apply {
        sourceDir.convention(project.layout.projectDirectory.dir("src/golang/native"))
        outputDir.convention(project.layout.buildDirectory.dir("golang"))
        architectures.convention(GolangExtension.DEFAULT_ARCHITECTURES)
        buildTags.convention(
            provider.getCsv(
                "golang.buildTags",
                GolangExtension.DEFAULT_BUILD_TAGS.joinToString(","),
            ).toMutableList(),
        )
        buildFlags.convention(
            provider.getCsv(
                "golang.buildFlags",
                GolangExtension.DEFAULT_BUILD_FLAGS.joinToString(","),
            ).toMutableList(),
        )
    }
}
