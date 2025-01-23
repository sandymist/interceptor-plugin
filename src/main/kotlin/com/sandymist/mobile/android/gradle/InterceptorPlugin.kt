package com.sandymist.mobile.android.gradle

import com.android.build.api.variant.AndroidComponentsExtension
import com.sandymist.mobile.android.gradle.extensions.InterceptorPluginExtension
import com.sandymist.mobile.android.gradle.util.AgpVersions
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.StopExecutionException
import java.io.File

@Suppress("UnstableApiUsage")
class InterceptorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (AgpVersions.CURRENT < AgpVersions.VERSION_7_0_0) {
            throw StopExecutionException(
                """
                Using com.sandymist.android.gradle:3+ with Android Gradle Plugin < 7 is not supported.
                Either upgrade the AGP version to 7+, or use an earlier version of the Interceptor
                Android Gradle Plugin. For more information check our migration guide
                https://docs.sentry.io/platforms/android/migration/#migrating-from-iosentrysentry-android-gradle-plugin-2x-to-iosentrysentry-android-gradle-plugin-300
                """.trimIndent()
            )
        }

        val extension = project.extensions.create(
            "interceptor",
            InterceptorPluginExtension::class.java,
            project
        )
        project.pluginManager.withPlugin("com.android.application") {
            val androidComponentsExt =
                project.extensions.getByType(AndroidComponentsExtension::class.java)

            // new API configuration
            androidComponentsExt.configure(
                project,
                extension
            )
        }
    }

    companion object {
        internal val sep = File.separator
    }
}
