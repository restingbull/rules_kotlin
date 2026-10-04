package io.bazel.kotlin.model

data class JvmCompilationTask(
  val info: CompilationTaskInfo = CompilationTaskInfo(),
  val directories: Directories = Directories(),
  val outputs: Outputs = Outputs(),
  val inputs: Inputs = Inputs(),
  val compileKotlin: Boolean = false,
  val instrumentCoverage: Boolean = false,
)
