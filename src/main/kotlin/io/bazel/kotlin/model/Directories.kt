package io.bazel.kotlin.model

data class Directories(
  val classes: String = "",
  val generatedClasses: String = "",
  val generatedSources: String = "",
  val temp: String = "",
  val generatedStubClasses: String = "",
  val abiClasses: String = "",
  val generatedJavaSources: String = "",
  val javaClasses: String = "",
  val coverageMetadataClasses: String = "",
)
