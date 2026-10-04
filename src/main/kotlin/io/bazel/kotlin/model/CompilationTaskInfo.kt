package io.bazel.kotlin.model

data class CompilationTaskInfo(
  val label: String = "",
  val platform: Platform = Platform.JVM,
  val ruleKind: RuleKind = RuleKind.LIBRARY,
  val moduleName: String = "",
  val passthroughFlagsList: List<String> = emptyList(),
  val toolchainInfo: KotlinToolchainInfo = KotlinToolchainInfo(),
  val friendPathsList: List<String> = emptyList(),
  val primaryOutputPath: String = "",
  val debugList: List<String> = emptyList(),
  val strictKotlinDeps: String = "",
  val reducedClasspathMode: String = "",
  val treatInternalAsPrivateInAbiJar: Boolean = false,
  val removePrivateClassesInAbiJar: Boolean = false,
  val removeDebugInfo: Boolean = false,
  val preserveDeclarationOrder: Boolean = false,
  val removeDataClassCopyIfConstructorIsPrivate: Boolean = false,
)
