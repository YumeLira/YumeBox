package plugins

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import core.applyBaseAndroidConvention
import core.applyReleaseSigningFrom
import core.readAndroidConventionValues
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class BaseAndroidPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val values = target.readAndroidConventionValues()
        target.pluginManager.withPlugin("com.android.application") { configureApp(target, values) }
        target.pluginManager.withPlugin("com.android.library") { configureLib(target, values) }
    }

    private fun configureApp(
        project: Project,
        values: core.AndroidConventionValues,
    ) {
        project.extensions.configure<ApplicationExtension> {
            applyBaseAndroidConvention(values)
            applyReleaseSigningFrom(project)
        }
    }

    private fun configureLib(
        project: Project,
        values: core.AndroidConventionValues,
    ) {
        project.extensions.configure<LibraryExtension> {
            applyBaseAndroidConvention(values)
        }
    }
}
