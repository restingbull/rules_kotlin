# Copyright 2020 The Bazel Authors. All rights reserved.
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
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive", "http_file", "http_jar")
load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")
load("@bazel_tools//tools/build_defs/repo:utils.bzl", "maybe")
load(":vars.bzl", "VARS")

def kt_download_local_dev_dependencies():
    """
    Downloads all necessary http_* artifacts for rules_kotlin dev configuration.

    Must be called before setup_dependencies in the WORKSPACE.
    """

    maybe(
        http_archive,
        name = "io_bazel_rules_kotlin",
        sha256 = VARS.RULES_KOTLIN_SHA,
        urls = [
            "https://github.com/bazelbuild/rules_kotlin/releases/download/legacy-%s/rules_kotlin_release.tgz" % VARS.RULES_KOTLIN_VERSION,
        ],
    )

    http_archive(
        name = VARS.KT_COMPILER_REPO,
        urls = VARS.KOTLIN_CURRENT_COMPILER_RELEASE["urls"],
        sha256 = VARS.KOTLIN_CURRENT_COMPILER_RELEASE["sha256"],
        build_file = "//kotlin/internal/repositories:BUILD.com_github_jetbrains_kotlin",
        strip_prefix = "kotlinc",
    )

    maybe(
        http_archive,
        name = "com_google_protobuf",
        sha256 = VARS.PROTOBUF_SHA,
        strip_prefix = "protobuf-%s" % VARS.PROTOBUF_VERSION,
        urls = [
            "https://mirror.bazel.build/github.com/protocolbuffers/protobuf/archive/v%s.tar.gz" % VARS.PROTOBUF_VERSION,
            "https://github.com/protocolbuffers/protobuf/archive/v%s.tar.gz" % VARS.PROTOBUF_VERSION,
        ],
    )

    maybe(
        http_archive,
        name = "rules_proto",
        sha256 = VARS.RULES_PROTO_SHA,
        strip_prefix = "rules_proto-%s" % VARS.RULES_PROTO_GIT_COMMIT,
        urls = [
            "https://mirror.bazel.build/github.com/bazelbuild/rules_proto/archive/%s.tar.gz" % VARS.RULES_PROTO_GIT_COMMIT,
            "https://github.com/bazelbuild/rules_proto/archive/%s.tar.gz" % VARS.RULES_PROTO_GIT_COMMIT,
        ],
    )

    maybe(
        http_archive,
        name = "bazel_skylib",
        urls = ["https://github.com/bazelbuild/bazel-skylib/archive/%s.tar.gz" % VARS.SKYLIB_VERSION],
        strip_prefix = "bazel-skylib-%s" % VARS.SKYLIB_VERSION,
        sha256 = VARS.SKYLIB_SHA,
    )

    maybe(
        http_jar,
        name = "bazel_deps",
        sha256 = VARS.BAZEL_DEPS_SHA,
        url = "https://github.com/hsyed/bazel-deps/releases/download/v%s/parseproject_deploy.jar" % VARS.BAZEL_DEPS_VERSION,
    )

    maybe(
        http_archive,
        name = "bazel_toolchains",
        sha256 = VARS.BAZEL_TOOLCHAINS_SHA,
        strip_prefix = "bazel-toolchains-%s" % VARS.BAZEL_TOOLCHAINS_VERSION,
        urls = [
            "https://mirror.bazel.build/github.com/bazelbuild/bazel-toolchains/archive/%s.tar.gz" % VARS.BAZEL_TOOLCHAINS_VERSION,
            "https://github.com/bazelbuild/bazel-toolchains/archive/%s.tar.gz" % VARS.BAZEL_TOOLCHAINS_VERSION,
        ],
    )

    maybe(
        http_archive,
        name = "build_bazel_rules_nodejs",
        sha256 = VARS.RULES_NODEJS_SHA,
        url = "https://github.com/bazelbuild/rules_nodejs/releases/download/{0}/rules_nodejs-{0}.tar.gz".format(VARS.RULES_NODEJS_VERSION),
    )

    maybe(
        http_archive,
        name = "rules_jvm_external",
        sha256 = VARS.RULES_JVM_EXTERNAL_SHA,
        strip_prefix = "rules_jvm_external-%s" % VARS.RULES_JVM_EXTERNAL_TAG,
        url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % VARS.RULES_JVM_EXTERNAL_TAG,
    )

    maybe(
        http_archive,
        name = "rules_pkg",
        url = "https://github.com/bazelbuild/rules_pkg/releases/download/0.2.4/rules_pkg-0.2.4.tar.gz",
        sha256 = "4ba8f4ab0ff85f2484287ab06c0d871dcb31cc54d439457d28fd4ae14b18450a",
    )

    maybe(
        git_repository,
        name = "io_bazel_stardoc",
        remote = "https://github.com/bazelbuild/stardoc.git",
        tag = "0.4.0",
    )
