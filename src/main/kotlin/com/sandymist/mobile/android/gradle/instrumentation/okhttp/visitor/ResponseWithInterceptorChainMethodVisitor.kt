package com.sandymist.mobile.android.gradle.instrumentation.okhttp.visitor

import com.sandymist.mobile.android.gradle.instrumentation.SpanAddingClassVisitorFactory
import com.sandymist.mobile.android.gradle.instrumentation.util.Types
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.GeneratorAdapter
import org.objectweb.asm.commons.Method

class ResponseWithInterceptorChainMethodVisitor(
    api: Int,
    private val originalVisitor: MethodVisitor,
    access: Int,
    name: String?,
    descriptor: String?,
    private val parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
) : GeneratorAdapter(api, originalVisitor, access, name, descriptor) {

    private var shouldInstrument = false

    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        if (opcode == Opcodes.INVOKEVIRTUAL &&
            owner == "okhttp3/OkHttpClient" &&
            name == "interceptors"
        ) {
            shouldInstrument = true
        }
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }

    override fun visitInsn(opcode: Int) {
        super.visitInsn(opcode)
        if (opcode == Opcodes.POP && shouldInstrument) {
            visitAddInterceptor(parameters.targetClassName.get())
            shouldInstrument = false
        }
    }

    /*
     Roughly constructing this, but in Java:

     if (interceptors.find { it is MyOkHttpInterceptor } != null) {
       interceptors += MyOkHttpInterceptor()
     }
     */
    private fun MethodVisitor.visitAddInterceptor(targetClassName: String) {
        println("Target class: " + targetClassName.toJvmDescriptor())
        val targetClassType = Type.getType(targetClassName.toJvmDescriptor())

        originalVisitor.visitVarInsn(Opcodes.ALOAD, 1) // interceptors list

        checkCast(Types.ITERABLE)
        invokeInterface(Types.ITERABLE, Method.getMethod("java.util.Iterator iterator ()"))
        val iteratorIndex = newLocal(Types.ITERATOR)
        storeLocal(iteratorIndex)

        val whileLabel = Label()
        val endWhileLabel = Label()
        visitLabel(whileLabel)
        loadLocal(iteratorIndex)
        invokeInterface(Types.ITERATOR, Method.getMethod("boolean hasNext ()"))
        ifZCmp(EQ, endWhileLabel)
        loadLocal(iteratorIndex)
        invokeInterface(Types.ITERATOR, Method.getMethod("Object next ()"))

        val interceptorIndex = newLocal(Types.OBJECT)
        storeLocal(interceptorIndex)
        loadLocal(interceptorIndex)
        checkCast(Types.OKHTTP_INTERCEPTOR)
        instanceOf(targetClassType)
        ifZCmp(EQ, whileLabel)
        loadLocal(interceptorIndex)
        val ifLabel = Label()
        goTo(ifLabel)

        visitLabel(endWhileLabel)
        originalVisitor.visitInsn(Opcodes.ACONST_NULL)
        visitLabel(ifLabel)
        val originalMethodLabel = Label()
        ifNonNull(originalMethodLabel)

        originalVisitor.visitVarInsn(Opcodes.ALOAD, 1)
        checkCast(Types.COLLECTION)
        newInstance(targetClassType)
        dup()
        val interceptorOkHttpCtor = Method.getMethod("void <init> ()")
        invokeConstructor(targetClassType, interceptorOkHttpCtor)
        val addInterceptor = Method.getMethod("boolean add (Object)")
        invokeInterface(Types.COLLECTION, addInterceptor)
        pop()
        visitLabel(originalMethodLabel)
    }
}

fun String.toJvmDescriptor(): String {
    return "L" + this.replace('.', '/') + ";"
}
