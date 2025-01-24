package com.sandymist.mobile.android.gradle

import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*

class LogEventMethodVisitor(
    methodVisitor: MethodVisitor
) : MethodVisitor(ASM9, methodVisitor) {

    private val myClassName = "com/scribd/app/scranalytics/MyScranalytics"
    override fun visitCode() {
        super.visitCode()

        // Insert code at the beginning of the method:
        // MyEventLogger().myLogEvent(eventId, parameters, timed, specificTime)

        // Create an instance of MyEventLogger
        mv.visitTypeInsn(NEW, myClassName)
        mv.visitInsn(DUP)
        mv.visitMethodInsn(INVOKESPECIAL, myClassName, "<init>", "()V", false)

        // Load method parameters onto the stack
        mv.visitVarInsn(ALOAD, 1) // eventId
        mv.visitVarInsn(ALOAD, 2) // parameters
        mv.visitVarInsn(ILOAD, 3) // timed
        mv.visitVarInsn(LLOAD, 4) // specificTime

        // Call myLogEvent
        mv.visitMethodInsn(
            INVOKEVIRTUAL,
            myClassName,
            "myLogEvent",
            "(Ljava/lang/String;Ljava/util/Map;ZJ)V",
            false
        )
    }
}

class EventLoggerClassVisitor(
    classVisitor: ClassVisitor
) : ClassVisitor(ASM9, classVisitor) {

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        return if (name == "_logEvent" && descriptor == "(Ljava/lang/String;Ljava/util/Map;ZJ)V") {
            LogEventMethodVisitor(mv)
        } else {
            mv
        }
    }
}
