package com.sandymist.mobile.android.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.io.File

abstract class SwapMethodTask : DefaultTask() {

    @InputDirectory
    val inputDir: DirectoryProperty = project.objects.directoryProperty()

    @OutputDirectory
    val outputDir: DirectoryProperty = project.objects.directoryProperty()

    @TaskAction
    fun swapMethod() {
        val inputDirFile = inputDir.get().asFile
        val outputDirFile = outputDir.get().asFile

        // Ensure the output directory exists
        outputDirFile.mkdirs()

        // Process all `.class` files in the input directory
        inputDirFile.walkTopDown().filter { it.extension == "class" }.forEach { classFile ->
            val relativePath = classFile.relativeTo(inputDirFile).path
            val targetFile = File(outputDirFile, relativePath)

            // Ensure the parent directories exist
            targetFile.parentFile.mkdirs()

            // Perform the method swapping logic (for example, with ASM)
            targetFile.outputStream().use { outputStream ->
                classFile.inputStream().use { inputStream ->
                    val classReader = ClassReader(inputStream)
                    val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)

                    // Your custom ClassVisitor for method swapping
                    val classVisitor = SwapMethodClassVisitor(classWriter)
                    classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)

                    outputStream.write(classWriter.toByteArray())
                }
            }
        }
    }
}

class SwapMethodClassVisitor(classVisitor: ClassVisitor) : ClassVisitor(Opcodes.ASM9, classVisitor) {
    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val originalMethodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)

        // Replace the logic of the target method
        return if (name == "methodToBeReplaced" && descriptor == "()V") {
            println("Swapping implementation of method: $name $descriptor")
            SwapMethodVisitor(originalMethodVisitor)
        } else {
            originalMethodVisitor
        }
    }
}

class SwapMethodVisitor(methodVisitor: MethodVisitor) : MethodVisitor(Opcodes.ASM9, methodVisitor) {
    override fun visitCode() {
        super.visitCode()

        // Custom logic to replace the original method body
        // For example, this method just prints a message to the console
        mv.visitFieldInsn(
            Opcodes.GETSTATIC,
            "java/lang/System",
            "out",
            "Ljava/io/PrintStream;"
        )
        mv.visitLdcInsn("Custom method logic executed!")
        mv.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/io/PrintStream",
            "println",
            "(Ljava/lang/String;)V",
            false
        )

        // Add a return instruction for the method
        mv.visitInsn(Opcodes.RETURN)
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        super.visitMaxs(2, 1) // Adjust max stack and local variables as necessary
    }
}

