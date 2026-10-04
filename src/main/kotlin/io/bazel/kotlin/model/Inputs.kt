package io.bazel.kotlin.model

data class Inputs(
  val classpathList: List<String> = emptyList(),
  val directDependenciesList: List<String> = emptyList(),
  val kotlinSourcesList: List<String> = emptyList(),
  val javaSourcesList: List<String> = emptyList(),
  val sourceJarsList: List<String> = emptyList(),
  val processorsList: List<String> = emptyList(),
  val processorpathsList: List<String> = emptyList(),
  val stubsPluginOptionsList: List<String> = emptyList(),
  val stubsPluginsList: List<String> = emptyList(),
  val stubsPluginClasspathList: List<String> = emptyList(),
  val compilerPluginOptionsList: List<String> = emptyList(),
  val compilerPluginsList: List<String> = emptyList(),
  val compilerPluginClasspathList: List<String> = emptyList(),
  val javacFlagsList: List<String> = emptyList(),
  val depsArtifactsList: List<String> = emptyList(),
  val pluginsList: List<Plugin> = emptyList(),
)
