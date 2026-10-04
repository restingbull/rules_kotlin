package io.bazel.kotlin.model

data class BtapiRuntime(
  val apiImplClasspathList: List<String> = emptyList(),
  val jvmAbiGenClasspathList: List<String> = emptyList(),
  val kaptClasspathList: List<String> = emptyList(),
  val jdepsGenClasspathList: List<String> = emptyList(),
  val classpathFingerprint: String = "",
)
