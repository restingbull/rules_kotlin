package io.bazel.kotlin.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PluginsPayloadTest {
  @Test
  fun parsesEmptyObjectToEmptyList() {
    assertEquals(emptyList<Plugin>(), PluginsPayloadJson.parse("{}"))
  }

  @Test
  fun defaultsMissingFields() {
    assertEquals(listOf(Plugin()), PluginsPayloadJson.parse("""{"plugins":[{}]}"""))
  }

  @Test
  fun parsesFullPluginMappingModelTypes() {
    val json = """
      {"plugins":[
        {"id":"p1",
         "classpath":["a.jar","b.jar"],
         "options":[{"key":"k1","value":"v1"},{"key":"k2","value":"v2"}],
         "phases":["PLUGIN_PHASE_COMPILE","PLUGIN_PHASE_STUBS"]}
      ]}
    """.trimIndent()
    val expected = listOf(
      Plugin(
        id = "p1",
        classpathList = listOf("a.jar", "b.jar"),
        optionsList = listOf(PluginOption("k1", "v1"), PluginOption("k2", "v2")),
        phasesList = listOf(PluginPhase.PLUGIN_PHASE_COMPILE, PluginPhase.PLUGIN_PHASE_STUBS),
      ),
    )
    assertEquals(expected, PluginsPayloadJson.parse(json))
  }

  @Test
  fun dropsUnknownPhaseNamesByConstantNameButKeepsUnspecified() {
    val json = """
      {"plugins":[{"id":"p","phases":[
        "PLUGIN_PHASE_BOGUS","PLUGIN_PHASE_COMPILE","UNRECOGNIZED",
        "PLUGIN_PHASE_UNSPECIFIED","PLUGIN_PHASE_STUBS"]}]}
    """.trimIndent()
    assertEquals(
      listOf(
        PluginPhase.PLUGIN_PHASE_COMPILE,
        PluginPhase.PLUGIN_PHASE_UNSPECIFIED,
        PluginPhase.PLUGIN_PHASE_STUBS,
      ),
      PluginsPayloadJson.parse(json).single().phasesList,
    )
  }

  @Test
  fun dropsAllUnknownPhasesToEmpty() {
    val json = """{"plugins":[{"id":"p","phases":["UNKNOWN"]}]}"""
    assertEquals(emptyList<PluginPhase>(), PluginsPayloadJson.parse(json).single().phasesList)
  }

  @Test
  fun ignoresUnknownKeysAtEveryLevel() {
    val json = """
      {"unknownTop":1,
       "plugins":[
         {"id":"p","extraPlugin":true,
          "options":[{"key":"k","value":"v","extraOpt":0}],
          "classpath":["x.jar"]}
       ]}
    """.trimIndent()
    val expected = listOf(
      Plugin(
        id = "p",
        classpathList = listOf("x.jar"),
        optionsList = listOf(PluginOption("k", "v")),
      ),
    )
    assertEquals(expected, PluginsPayloadJson.parse(json))
  }

  @Test(expected = IllegalArgumentException::class)
  fun malformedJsonThrowsIllegalArgument() {
    PluginsPayloadJson.parse("{invalid")
  }
}
