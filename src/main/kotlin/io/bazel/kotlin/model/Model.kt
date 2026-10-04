package io.bazel.kotlin.model

enum class Platform {
  JVM,
  ANDROID,
  ;

  companion object {
    fun fromName(name: String?): Platform = values().firstOrNull { it.name == name } ?: JVM
  }
}

enum class RuleKind {
  LIBRARY,
  BINARY,
  TEST,
  IMPORT,
  ;

  companion object {
    fun fromName(name: String?): RuleKind = values().firstOrNull { it.name == name } ?: LIBRARY
  }
}

enum class PluginPhase {
  PLUGIN_PHASE_UNSPECIFIED,
  PLUGIN_PHASE_COMPILE,
  PLUGIN_PHASE_STUBS,
  ;

  companion object {
    fun fromName(name: String?): PluginPhase =
      values().firstOrNull { it.name == name } ?: PLUGIN_PHASE_UNSPECIFIED
  }
}

data class Common(
  val languageVersion: String = "",
  val apiVersion: String = "",
  val coroutines: String = "",
)

data class Jvm(val jvmTarget: String = "")

data class BtapiRuntime(
  val apiImplClasspathList: List<String> = emptyList(),
  val jvmAbiGenClasspathList: List<String> = emptyList(),
  val kaptClasspathList: List<String> = emptyList(),
  val jdepsGenClasspathList: List<String> = emptyList(),
  val classpathFingerprint: String = "",
)

data class KotlinToolchainInfo(
  val common: Common = Common(),
  val jvm: Jvm = Jvm(),
  val btapi: BtapiRuntime? = null,
)

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

data class PluginOption(val key: String = "", val value: String = "")

data class Plugin(
  val id: String = "",
  val classpathList: List<String> = emptyList(),
  val optionsList: List<PluginOption> = emptyList(),
  val phasesList: List<PluginPhase> = emptyList(),
)

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

data class JvmCompilationTask(
  val info: CompilationTaskInfo = CompilationTaskInfo(),
  val directories: Directories = Directories(),
  val outputs: Outputs = Outputs(),
  val inputs: Inputs = Inputs(),
  val compileKotlin: Boolean = false,
  val instrumentCoverage: Boolean = false,
)
