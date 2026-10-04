package io.bazel.kotlin.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Serialization DTOs mirroring the fixed plugins-payload JSON emitted by Starlark payload.bzl.
 * These reference only JSON-shaped primitives so the kotlinx.serialization compiler plugin can
 * generate serializers without touching the hand-written model types.
 */
@Serializable
data class PluginsPayloadDto(val plugins: List<PluginDto> = emptyList())

@Serializable
data class PluginDto(
  val id: String = "",
  val classpath: List<String> = emptyList(),
  val options: List<PluginOptionDto> = emptyList(),
  val phases: List<String> = emptyList(),
)

@Serializable
data class PluginOptionDto(val key: String = "", val value: String = "")

/** Parses the plugins-payload JSON into the hand-written [Plugin] model. */
object PluginsPayloadJson {
  private val format = Json { ignoreUnknownKeys = true }

  /**
   * Parses [json] into a list of model [Plugin]s.
   *
   * Malformed input is left to surface the kotlinx `SerializationException`, which is itself an
   * [IllegalArgumentException]; callers rely on that type and it is not caught or rewrapped here.
   */
  fun parse(json: String): List<Plugin> =
    format.decodeFromString(PluginsPayloadDto.serializer(), json).plugins.map { it.toModel() }

  private fun PluginDto.toModel(): Plugin =
    Plugin(
      id = id,
      classpathList = classpath,
      optionsList = options.map { PluginOption(key = it.key, value = it.value) },
      // Map each phase string to a PluginPhase by EXACT enum-constant name and drop anything that
      // is not a declared constant. PluginPhase.fromName must NOT be used: its UNSPECIFIED fallback
      // would make a bogus name indistinguishable from a legitimate PLUGIN_PHASE_UNSPECIFIED value.
      phasesList = phases.mapNotNull { name -> PluginPhase.entries.firstOrNull { it.name == name } },
    )
}
