load(
    ":vars.release.bzl",
    RELEASE = "VARS",
)

_ignore = {k: True for k in dir(struct())}

config = dict(
    BAZEL_DEPS_SHA = "05498224710808be9687f5b9a906d11dd29ad592020246d4cd1a26eeaed0735e",
    BAZEL_DEPS_VERSION = "0.1.0",
    BAZEL_TOOLCHAINS_SHA = "5962fe677a43226c409316fcb321d668fc4b7fa97cb1f9ef45e7dc2676097b26",
    BAZEL_TOOLCHAINS_VERSION = "be10bee3010494721f08a0fccd7f57411a1e773e",
    JAVA_RUNTIME_TOOLCHAIN_TYPE = "@bazel_tools//tools/jdk:runtime_toolchain_type",
    JAVA_TOOLCHAIN_TYPE = "@bazel_tools//tools/jdk:toolchain_type",
    KT_COMPILER_REPO = "dev_com_github_jetbrains_kotlin",
    PROTOBUF_SHA = "cf754718b0aa945b00550ed7962ddc167167bd922b842199eeb6505e6f344852",
    PROTOBUF_VERSION = "3.11.3",
    RULES_JVM_EXTERNAL_SHA = "f04b1466a00a2845106801e0c5cec96841f49ea4e7d1df88dc8e4bf31523df74",
    RULES_JVM_EXTERNAL_TAG = "2.7",
    RULES_KOTLIN_SHA = "da0e6e1543fcc79e93d4d93c3333378f3bd5d29e82c1bc2518de0dbe048e6598",
    RULES_KOTLIN_VERSION = "1.4.0-rc3",
    RULES_NODEJS_SHA = "3356c6b767403392bab018ce91625f6d15ff8f11c6d772dc84bc9cada01c669a",
    RULES_NODEJS_VERSION = "0.36.1",
    RULES_PROTO_GIT_COMMIT = "f6b8d89b90a7956f6782a4a3609b2f0eee3ce965",
    RULES_PROTO_SHA = "4d421d51f9ecfe9bf96ab23b55c6f2b809cbaf0eea24952683e397decfbd0dd0",
    SKYLIB_SHA = "2ea8a5ed2b448baf4a6855d3ce049c4c452a6470b1efd1504fdb7c1c134d220a",
    SKYLIB_VERSION = "0.8.0",
    TOOLCHAIN_TYPE = "@io_bazel_rules_kotlin//kotlin/internal:kt_toolchain_type",
    KOTLIN = struct(
        VERSION = "1.3.50",
        COROUTINE = "1.3.5",
        SERIALIZATION = "0.14.0",
        TEST = "1.3.72",
        REFLECT = "1.3.72",
    ),
)

config.update({k: getattr(RELEASE, k, None) for k in dir(RELEASE) if not k in _ignore})

VARS = struct(**config)
