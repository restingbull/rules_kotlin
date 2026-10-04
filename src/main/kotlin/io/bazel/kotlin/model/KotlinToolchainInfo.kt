package io.bazel.kotlin.model

data class KotlinToolchainInfo(
  val common: Common = Common(),
  val jvm: Jvm = Jvm(),
  val btapi: BtapiRuntime? = null,
)
