package io.bazel.kotlin.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ModelTest {
  @Test
  fun defaultsPreserveProtoAbsenceSemantics() {
    val task = JvmCompilationTask()
    assertEquals(CompilationTaskInfo(), task.info)
    assertEquals(Directories(), task.directories)
    assertEquals(Outputs(), task.outputs)
    assertEquals(Inputs(), task.inputs)
    assertFalse(task.compileKotlin)
    assertFalse(task.instrumentCoverage)
    assertEquals(Platform.JVM, task.info.platform)
    assertEquals(RuleKind.LIBRARY, task.info.ruleKind)
    assertEquals(KotlinToolchainInfo(), task.info.toolchainInfo)
    assertEquals(Common(), task.info.toolchainInfo.common)
    assertEquals(Jvm(), task.info.toolchainInfo.jvm)
    assertNull(task.info.toolchainInfo.btapi)

    // Inspect every scalar and repeated field, including optional output gates.
    for (model in listOf(
      task, task.info, task.directories, task.outputs, task.inputs,
      KotlinToolchainInfo(), Common(), Jvm(), BtapiRuntime(), Plugin(), PluginOption(),
    )) {
      for (field in model.javaClass.declaredFields.filterNot { it.isSynthetic }) {
        field.isAccessible = true
        when (field.type) {
          String::class.java -> assertEquals(field.name, "", field.get(model))
          Boolean::class.javaPrimitiveType -> assertEquals(field.name, false, field.get(model))
          List::class.java -> {
            assertEquals(field.name, emptyList<Any>(), field.get(model))
            org.junit.Assert.assertTrue(field.name, field.name.endsWith("List"))
          }
        }
      }
    }
  }

  @Test
  fun copyReplacesSourcesWithoutLosingTaskState() {
    val original = JvmCompilationTask(
      info = CompilationTaskInfo(label = "//example:lib", moduleName = "example"),
      directories = Directories(temp = "tmp"),
      outputs = Outputs(jar = "out.jar"),
      inputs = Inputs(classpathList = listOf("dep.jar"), sourceJarsList = listOf("src.jar")),
      compileKotlin = true,
      instrumentCoverage = true,
    )
    val expanded = original.copy(inputs = original.inputs.copy(kotlinSourcesList = listOf("Expanded.kt")))
    assertEquals(listOf("Expanded.kt"), expanded.inputs.kotlinSourcesList)
    assertEquals(original, expanded.copy(inputs = original.inputs))
    assertEquals(original.inputs, expanded.inputs.copy(kotlinSourcesList = emptyList()))
    assertEquals(emptyList<String>(), original.inputs.kotlinSourcesList)
  }

  @Test
  fun btapiPresenceDistinguishesRuntimeEvenWithEmptyClasspath() {
    val legacy = KotlinToolchainInfo()
    val btapi = legacy.copy(btapi = BtapiRuntime())
    assertNull(legacy.btapi)
    assertEquals(BtapiRuntime(), btapi.btapi)
    assertEquals(legacy, btapi.copy(btapi = null))
  }

  @Test
  fun enumNamesAndKnownParsingMatchProtoConstants() {
    assertEquals(listOf("JVM", "ANDROID"), Platform.values().map { it.name })
    assertEquals(listOf("LIBRARY", "BINARY", "TEST", "IMPORT"), RuleKind.values().map { it.name })
    assertEquals(
      listOf("PLUGIN_PHASE_UNSPECIFIED", "PLUGIN_PHASE_COMPILE", "PLUGIN_PHASE_STUBS"),
      PluginPhase.values().map { it.name },
    )
    Platform.values().forEach { assertEquals(it, Platform.fromName(it.name)) }
    RuleKind.values().forEach { assertEquals(it, RuleKind.fromName(it.name)) }
    PluginPhase.values().forEach { assertEquals(it, PluginPhase.fromName(it.name)) }
  }

  @Test
  fun unknownEnumNamesUseSpecifiedDefaultsWithoutThrowing() {
    // KotlinBuilder currently throws; these are the new model's explicit parsing defaults.
    for (name in listOf(null, "", "UNRECOGNIZED", "FUTURE_VALUE", "jvm")) {
      assertEquals(Platform.JVM, Platform.fromName(name))
      assertEquals(RuleKind.LIBRARY, RuleKind.fromName(name))
      assertEquals(PluginPhase.PLUGIN_PHASE_UNSPECIFIED, PluginPhase.fromName(name))
    }
  }
}
