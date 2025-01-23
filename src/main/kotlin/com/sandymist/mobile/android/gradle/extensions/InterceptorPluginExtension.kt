package com.sandymist.mobile.android.gradle.extensions

import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.provider.Property

abstract class InterceptorPluginExtension @Inject constructor(project: Project) {

    private val objects = project.objects

    val targetClassName: Property<String> = objects.property(String::class.java)
        .convention("okhttp3.Interceptor") // default, to be overridden

    val tracingInstrumentation: TracingInstrumentationExtension = objects.newInstance(
        TracingInstrumentationExtension::class.java
    )
}
