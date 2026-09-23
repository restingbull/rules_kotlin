# Copyright 2018 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
load("@bazel_skylib//:bzl_library.bzl", "bzl_library")
load("//src/main/starlark/release:packager.bzl", "release_archive")
load("//src/main/starlark/release:release_jars.bzl", "release_jars")
load(":version.jar.bzl", "JAR_VERSIONS")

# Regenerates jar_version.bzl at release time from the pinned single-jar deps in
# version.jar.bzl. The generated DefaultInfo file (jar_version.bzl) is packed into the
# release tar via the "pkg" srcs below, shadowing the checked-in placeholder. Keys map the
# downloaded http_file jar to its logical JAR_VERSIONS name.
release_jars(
    name = "jar_version_generated",
    jars = {
        "@com_github_pinterest_ktlint//file": "PINTEREST_KTLINT",
        "@kotlin_build_tools_api//file": "KOTLIN_BUILD_TOOLS_API",
        "@kotlinx_coroutines_core_jvm//file": "KOTLINX_COROUTINES_CORE_JVM",
        "@kotlinx_serialization_core_jvm//file": "KOTLINX_SERIALIZATION_CORE_JVM",
        "@kotlinx_serialization_json//file": "KOTLINX_SERIALIZATION_JSON",
        "@kotlinx_serialization_json_jvm//file": "KOTLINX_SERIALIZATION_JSON_JVM",
    },
    urls = {
        "KOTLINX_COROUTINES_CORE_JVM": JAR_VERSIONS.KOTLINX_COROUTINES_CORE_JVM.url_templates[0],
        "KOTLINX_SERIALIZATION_CORE_JVM": JAR_VERSIONS.KOTLINX_SERIALIZATION_CORE_JVM.url_templates[0],
        "KOTLINX_SERIALIZATION_JSON": JAR_VERSIONS.KOTLINX_SERIALIZATION_JSON.url_templates[0],
        "KOTLINX_SERIALIZATION_JSON_JVM": JAR_VERSIONS.KOTLINX_SERIALIZATION_JSON_JVM.url_templates[0],
        "KOTLIN_BUILD_TOOLS_API": JAR_VERSIONS.KOTLIN_BUILD_TOOLS_API.url_templates[0],
        "PINTEREST_KTLINT": JAR_VERSIONS.PINTEREST_KTLINT.url_templates[0],
    },
    # volatile-status.txt (--stamp) exposes BUILD_TIMESTAMP, not VERSION.
    version_key = "BUILD_TIMESTAMP",
)

release_archive(
    name = "pkg",
    srcs = [
        "BUILD.com_github_google_ksp.bazel",
        "BUILD.com_github_jetbrains_kotlin.bazel",
        "BUILD.kotlin_capabilities.bazel",
        "btapi_impl.bzl",
        "bzlmod_impl.bzl",
        "compiler.bzl",
        "ksp.bzl",
        "version.jar.bzl",
        "versions.bzl",
        # Generated jar_version.bzl shadows the checked-in placeholder; the placeholder is
        # NOT listed here (only glob'd into the bzl_library below for dev-tree load()).
        ":jar_version_generated",
    ],
    src_map = {
        "BUILD.release.bazel": "BUILD.bazel",
        "bzlmod_setup.release.bzl": "bzlmod_setup.bzl",
        "initialize.release.bzl": "initialize.bzl",
    },
    deps = [
        "//src/main/starlark/core/repositories/kotlin:pkg",
    ],
)

bzl_library(
    name = "repositories",
    srcs = glob(["*.bzl"]),
    visibility = ["//:__subpackages__"],
    deps = [
        "//src/main/starlark/core/repositories/kotlin",
        "@bazel_skylib//lib:versions",
    ],
)
