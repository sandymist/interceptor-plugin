@file:Suppress("UnstableApiUsage")

package com.sandymist.mobile.android.gradle

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationParameters
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import com.android.build.gradle.internal.utils.setDisallowChanges
import com.sandymist.mobile.android.gradle.InterceptorPlugin.Companion.sep
import com.sandymist.mobile.android.gradle.extensions.InterceptorPluginExtension
import com.sandymist.mobile.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import com.sandymist.mobile.android.gradle.services.InterceptorModulesService
import com.sandymist.mobile.android.gradle.util.collectModules
import org.gradle.api.Project
import java.io.File

fun AndroidComponentsExtension<*, *, *>.configure(
    project: Project,
    extension: InterceptorPluginExtension
) {
    // temp folder for sentry-related stuff
    val tmpDir = File("${project.buildDir}${sep}tmp${sep}interceptor")
    tmpDir.mkdirs()
    println("++++ ENTERING CONFIGURE")

    fun isTraceable(buildType: String) = buildType == "debug" || buildType == "dogfood" || buildType == "qa"

    configureVariants { variant ->
        val buildType = variant.buildType ?: ""
        println("++++ BUILDTYPE: $buildType")
        if (isTraceable(buildType) && extension.tracingInstrumentation.enabled.get()) {
            /**
             * We detect sentry-android SDK version using configurations.incoming.afterResolve.
             * This is guaranteed to be executed BEFORE any of the build tasks/transforms are started.
             *
             * After detecting the sdk state, we use Gradle's shared build service to persist
             * the state between builds and also during a single build, because transforms
             * are run in parallel.
             */
            val interceptorModulesService = InterceptorModulesService.register(project)
            project.collectModules(
                "${variant.name}RuntimeClasspath",
                variant.name,
                interceptorModulesService
            )

            variant.configureInstrumentation(
                SpanAddingClassVisitorFactory::class.java,
                InstrumentationScope.ALL,
                FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS,
            ) { params ->
                if (extension.tracingInstrumentation.forceInstrumentDependencies.get()) {
                    params.invalidate.setDisallowChanges(System.currentTimeMillis())
                }
                params.debug.setDisallowChanges(
                    extension.tracingInstrumentation.debug.get()
                )
                params.features.setDisallowChanges(
                    extension.tracingInstrumentation.features.get()
                )
                params.interceptorModulesService.setDisallowChanges(interceptorModulesService)
                params.tmpDir.set(tmpDir)
                println("++++ PARAM TC: " + params.targetClassName)
                println("++++ >>>> TARET CLASS NAME: " + extension.targetClassName.get())
                params.targetClassName.setDisallowChanges(extension.targetClassName.get())
            }
        }
    }
}

private fun <T : InstrumentationParameters> Variant.configureInstrumentation(
    classVisitorFactoryImplClass: Class<out AsmClassVisitorFactory<T>>,
    scope: InstrumentationScope,
    mode: FramesComputationMode,
    instrumentationParamsConfig: (T) -> Unit,
) {
    instrumentation.transformClassesWith(
        classVisitorFactoryImplClass,
        scope,
        instrumentationParamsConfig
    )
    instrumentation.setAsmFramesComputationMode(mode)
}

/**
 * onVariants method in AGP 7.4.0 has a binary incompatibility with the prior versions, hence we
 * have to distinguish here, although the compatibility sources would look exactly the same.
 */
private fun AndroidComponentsExtension<*, *, *>.configureVariants(callback: (Variant) -> Unit) {
    onVariants(callback = callback)
}
