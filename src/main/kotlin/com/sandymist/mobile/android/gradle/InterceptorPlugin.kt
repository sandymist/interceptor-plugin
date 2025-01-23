package com.sandymist.mobile.android.gradle

import com.android.build.api.variant.AndroidComponentsExtension
import com.sandymist.mobile.android.gradle.extensions.InterceptorPluginExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

@Suppress("UnstableApiUsage")
class InterceptorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "interceptor",
            InterceptorPluginExtension::class.java,
            project
        )
        project.pluginManager.withPlugin("com.android.application") {
            val androidComponentsExt =
                project.extensions.getByType(AndroidComponentsExtension::class.java)
            androidComponentsExt.configure(project, extension)
        }

//        project.afterEvaluate {
//            println("Custom Parameter Target Class: ${extension.targetClassName.get()}")
//        }
    }

    companion object {
        internal val sep = File.separator
    }
}
