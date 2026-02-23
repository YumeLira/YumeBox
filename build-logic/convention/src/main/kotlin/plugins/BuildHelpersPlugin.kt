package plugins

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Marker plugin used to expose helper classes/functions from build-logic
 * to Kotlin DSL build scripts without applying extra conventions.
 */
class BuildHelpersPlugin : Plugin<Project> {
    override fun apply(target: Project) = Unit
}
