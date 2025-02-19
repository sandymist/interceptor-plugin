package com.sandymist.mobile.android.gradle.instrumentation.okhttp

import com.android.build.api.instrumentation.ClassContext
import com.sandymist.mobile.android.gradle.instrumentation.*
import com.sandymist.mobile.android.gradle.instrumentation.okhttp.visitor.ResponseWithInterceptorChainMethodVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class OkHttp : ClassInstrumentable {
    override val fqName: String get() = "RealCall"

    override fun getVisitor(
        instrumentableContext: ClassContext,
        apiVersion: Int,
        originalVisitor: ClassVisitor,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
    ): ClassVisitor = CommonClassVisitor(
        apiVersion = apiVersion,
        classVisitor = originalVisitor,
        className = fqName.substringAfterLast('.'),
        methodInstrumentables = listOf(ResponseWithInterceptorChain()),
        parameters = parameters
    )

    // OkHttp has renamed the class in v4, hence we are looking for both old and new package names
    // https://github.com/square/okhttp/commit/3d3b0f64005f7d2dd7cde80a9eaf665f8df86fb6#diff-e46bb5c1117393fbfb8cd1496fc4a2dcfcd6fcf70d065c50be83ce9215b2ec7b
    override fun isInstrumentable(data: ClassContext): Boolean =
        data.currentClassData.className == "okhttp3.internal.connection.RealCall" ||
            data.currentClassData.className == "okhttp3.RealCall"
}

class ResponseWithInterceptorChain : MethodInstrumentable {
    override val fqName: String get() = "getResponseWithInterceptorChain"

    override fun getVisitor(
        instrumentableContext: MethodContext,
        apiVersion: Int,
        originalVisitor: MethodVisitor,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
    ): MethodVisitor = ResponseWithInterceptorChainMethodVisitor(
        api = apiVersion,
        originalVisitor = originalVisitor,
        access = instrumentableContext.access,
        name = instrumentableContext.name,
        descriptor = instrumentableContext.descriptor,
        parameters = parameters,
    )

    override fun isInstrumentable(data: MethodContext): Boolean {
        return data.name?.startsWith(fqName) == true
    }
}
