package io.bazel.kotlin.model

data class Plugin(
  val id: String = "",
  val classpathList: List<String> = emptyList(),
  val optionsList: List<PluginOption> = emptyList(),
  val phasesList: List<PluginPhase> = emptyList(),
)
