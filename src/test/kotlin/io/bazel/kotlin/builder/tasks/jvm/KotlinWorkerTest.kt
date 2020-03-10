/*
 * Copyright 2020 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.bazel.kotlin.builder.tasks.jvm

import com.google.common.truth.Truth.assertThat
import io.bazel.kotlin.builder.KotlinJvmTestBuilder
import io.bazel.kotlin.builder.tasks.BazelWorker.Companion.OK
import io.bazel.kotlin.builder.tasks.InvocationWorker
import io.bazel.kotlin.builder.tasks.KotlinBuilder.Companion.JavaBuilderFlags
import io.bazel.kotlin.builder.tasks.KotlinBuilder.Companion.KotlinBuilderFlags
import io.bazel.kotlin.builder.utils.BazelRunFiles
import io.bazel.kotlin.builder.utils.Flag
import io.bazel.kotlin.model.CompilationTaskInfo
import io.bazel.kotlin.model.Platform
import io.bazel.kotlin.model.RuleKind
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.streams.toList

@RunWith(JUnit4::class)
class KotlinWorkerTest {

  private val ijar = BazelRunFiles.fromLabel(" @bazel_tools//tools/jdk:ijar")

  private val wrkDir = Files.createTempDirectory("KotlinBuilderEnvironmentTest")

  class SourceBuilder(private val out: ByteArrayOutputStream = ByteArrayOutputStream()) {
    val ln = "\n".toByteArray(StandardCharsets.UTF_8)
    fun l(l: String) {
      out.write(l.toByteArray(StandardCharsets.UTF_8))
      out.write(ln)
    }

    fun bytes(): ByteArray {
      return out.toByteArray()
    }
  }

  private fun runIjar(jar: Path, out: Path): Pair<Int, String> {
    return ProcessBuilder().command(ijar.toString(), "--target_label", "//jar:interface",
            "--injecting_rule_kind", "kt_jvm_library",
            jar.toString(),
            out.toString())
        .redirectErrorStream(true)
        .start().onExit().get().let {
          it.exitValue() to String(it.inputStream.readAllBytes(), StandardCharsets.UTF_8)
        }
  }

  private fun src(path: String, writeLines: (SourceBuilder.() -> Unit)): Path {
    val srcPath = Files.createDirectories(wrkDir.resolve("src")).resolve(path)
    val b = SourceBuilder()
    b.writeLines()
    Files.write(srcPath, b.bytes())

    require(Files.readAllLines(srcPath).isNotEmpty()) {
      "failed to write $srcPath"
    }
    return srcPath
  }

  class ArgsBuilder(val args: MutableMap<Flag, String> = mutableMapOf()) {
    fun flag(flag: Flag, value: String) {
      args[flag] = value
    }

    fun flag(flag: Flag, p: Path) {
      flag(flag, p.toString())
    }

    fun cp(flag: Flag, value: String) {
      args[flag] = args[flag]?.let { it + File.pathSeparator + value } ?: value
    }

    fun remove(flag: Flag) {
      args.remove(flag)
    }

    fun source(src: Path) {
      args[JavaBuilderFlags.SOURCES] =
          args[JavaBuilderFlags.SOURCES]?.let { "$it $src" } ?: src.toString()
    }

    fun list(): List<String> {
      return args.flatMap { listOf(it.key.flag, it.value) }
    }
  }

  private fun args(
    info: CompilationTaskInfo,
    init: ArgsBuilder.() -> Unit
  ) = with(ArgsBuilder()) {
    flag(JavaBuilderFlags.TARGET_LABEL, info.label)
    flag(KotlinBuilderFlags.MODULE_NAME, info.moduleName)
    flag(KotlinBuilderFlags.API_VERSION, info.toolchainInfo.common.apiVersion)
    flag(KotlinBuilderFlags.LANGUAGE_VERSION, info.toolchainInfo.common.languageVersion)
    flag(JavaBuilderFlags.RULE_KIND, "kt_${info.platform.name}_${info.ruleKind.name}")
    flag(JavaBuilderFlags.CLASSDIR, "kt_classes")
    flag(KotlinBuilderFlags.GENERATED_CLASSDIR, "generated_classes")
    flag(JavaBuilderFlags.TEMPDIR, "tmp")
    flag(JavaBuilderFlags.SOURCEGEN_DIR, "generated_sources")
    cp(JavaBuilderFlags.CLASSPATH, KotlinJvmTestBuilder.KOTLIN_STDLIB.singleCompileJar())
    flag(KotlinBuilderFlags.PASSTHROUGH_FLAGS, info.passthroughFlags)
    flag(KotlinBuilderFlags.DEBUG, info.debugList.joinToString(","))
    flag(KotlinBuilderFlags.JVM_TARGET, info.toolchainInfo.jvm.jvmTarget)
    init()
    list()
  }

  @Test
  fun `abi generation`() {

    val builder = KotlinJvmTestBuilder.component().kotlinBuilder()

    val one = src("One.kt") {
      l("package harry.nilsson")
      l("")
      l("class One : Zero() {")
      l("  override fun isTheLoneliestNumber():String {")
      l("     return \"that you'll ever do\"")
      l("  }")
      l("}")
    }

    val zero = src("Zero.kt") {
      l("package harry.nilsson")
      l("")
      l("abstract class Zero {")
      l("  abstract fun isTheLoneliestNumber():String")
      l("}")
    }

    // output buffer for compiler logs.
    val outputStream = ByteArrayOutputStream()
    val stdOut = System.out

    // tee the stdout -- useful for debugging, but also used as
    // part of the compilation task output.
    System.setOut(PrintStream(object : OutputStream() {
      @Throws(IOException::class) override fun write(b: Int) {
        outputStream.write(b)
        stdOut.write(b)
      }
    }))

    val worker = InvocationWorker(delegate = builder, buffer = outputStream)

    val abiJar = out("abi.jar")

    assertThat(worker.invoke(args(compilationTaskInfo) {
      flag(KotlinBuilderFlags.ABI_JAR, abiJar)
      source(one)
      source(zero)
    })).isEqualTo(OK to "")

    assertJarClasses(abiJar).containsExactly("harry/nilsson/One.class")
  }



  fun assertJarClasses(jar:Path) = assertThat(
      ZipFile(jar.toFile())
          .stream()
          .map(ZipEntry::getName)
          .filter { it.endsWith(".class") }
          .toList()
  )

  @Test
  fun `output directories are different for invocations`() {

    val builder = KotlinJvmTestBuilder.component().kotlinBuilder()

    val one = src("One.kt") {
      l("package harry.nilsson")
      l("")
      l("class One {")
      l("  fun isTheLoneliestNumber():String {")
      l("     return \"that you'll ever do\"")
      l("  }")
      l("}")
    }

    val two = src("Two.kt") {
      l("package harry.nilsson")
      l("class Two {")
      l("  fun canBeAsBadAsOne():String {")
      l("     return \"it is the loneliest number since the number one\"")
      l("  }")
      l("}")
    }

    // output buffer for compiler logs.
    val outputStream = ByteArrayOutputStream()
    val stdOut = System.out

    // tee the stdout -- useful for debugging, but also used as
    // part of the compilation task output.
    System.setOut(PrintStream(object : OutputStream() {
      @Throws(IOException::class) override fun write(b: Int) {
        outputStream.write(b)
        stdOut.write(b)
      }
    }))

    val worker = InvocationWorker(delegate = builder, buffer = outputStream)

    val jarOne = out("one.jar")
    assertThat(worker.invoke(args(compilationTaskInfo) {
      source(one)
      flag(JavaBuilderFlags.OUTPUT, jarOne.toString())
      flag(KotlinBuilderFlags.OUTPUT_SRCJAR, "$jarOne.srcjar")
      flag(KotlinBuilderFlags.OUTPUT_JDEPS, "out.jdeps")
    })).isEqualTo(OK to "")

    System.err.println(ZipFile(jarOne.toFile())
        .stream()
        .map(ZipEntry::getName)
        .toList())

    assertThat(
        ZipFile(jarOne.toFile())
            .stream()
            .map(ZipEntry::getName)
            .filter { it.endsWith(".class") }
            .toList()
    ).containsExactly("harry/nilsson/One.class")

    val jarTwo = out("two.jar")
    assertThat(worker.invoke(args(compilationTaskInfo) {
      source(two)
      flag(JavaBuilderFlags.OUTPUT, jarTwo.toString())
      flag(KotlinBuilderFlags.OUTPUT_SRCJAR, "$jarTwo.srcjar")
      flag(KotlinBuilderFlags.OUTPUT_JDEPS, "out.jdeps")
    })).isEqualTo(OK to "")

    assertThat(
        ZipFile(jarTwo.toFile())
            .stream()
            .map(ZipEntry::getName)
            .filter { it.endsWith(".class") }
            .toList()
    ).containsExactly("harry/nilsson/Two.class")
  }

  private fun out(name: String): Path {
    return Files.createDirectories(wrkDir.resolve("out")).resolve(name)
  }

  private val compilationTaskInfo: CompilationTaskInfo
    get() {
      return with(CompilationTaskInfo.newBuilder()) {
        label = "//singing/nilsson:one"
        moduleName = "harry.nilsson"
        platform = Platform.JVM
        ruleKind = RuleKind.LIBRARY
        toolchainInfo = with(toolchainInfoBuilder) {
          common =
              commonBuilder.setApiVersion("1.3").setCoroutines("enabled").setLanguageVersion("1.3")
                  .build()
          jvm = jvmBuilder.setJvmTarget("1.8").build()
          build()
        }
        build()
      }
    }
}
