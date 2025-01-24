package com.sandymist.mobile.android.gradle

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.internal.crash.afterEvaluate
import com.sandymist.mobile.android.gradle.extensions.InterceptorPluginExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
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

        project.afterEvaluate {
            project.tasks.register("insertLogEventMethodCall", DefaultTask::class.java) { task ->
                task.outputs.upToDateWhen { false }  // Disable caching for this task

                task.group = "bytecode manipulation"
                task.description = "Inserts a call to MyEventLogger.myLogEvent in EventLogger.logEvent."

                // Define the task's actions
                task.doLast {
                    println("Inserting method call into EventLogger.logEvent")

                    val inputDir = File(project.buildDir, "intermediates")
                    val outputDir = File(project.buildDir, "classes-transformed")
                    outputDir.mkdirs()

                    inputDir.walkTopDown()
                        .filter { it.extension == "class" && it.name == "Scranalytics.class" }
                        .forEach { classFile ->
                            val classBytes = classFile.readBytes()
                            val modifiedBytes = transformClass(classBytes)
                            val outputFile = outputDir.resolve(classFile.relativeTo(inputDir))
                            outputFile.parentFile.mkdirs()
                            outputFile.writeBytes(modifiedBytes)
                        }
                }
            }

            project.tasks.named("mergeProjectDexGoogleplayPremiumDebug").configure {
                println("++++ Will depend on task insertLogEventMethodCall")
                it.dependsOn("insertLogEventMethodCall")
            }
        }
    }

    private fun transformClass(classBytes: ByteArray): ByteArray {
        val classReader = ClassReader(classBytes)
        val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES)
        val classVisitor = EventLoggerClassVisitor(classWriter)
        classReader.accept(classVisitor, 0)
        return classWriter.toByteArray()
    }

    companion object {
        internal val sep = File.separator
    }
}
