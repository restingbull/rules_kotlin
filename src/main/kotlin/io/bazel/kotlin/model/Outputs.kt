package io.bazel.kotlin.model

data class Outputs(
  val jar: String = "",
  val jdeps: String = "",
  val srcjar: String = "",
  val abijar: String = "",
  val generatedJavaSrcJar: String = "",
  val generatedJavaStubJar: String = "",
  val generatedClassJar: String = "",
  val generatedKspSrcJar: String = "",
  val generatedKspClassesJar: String = "",
)
