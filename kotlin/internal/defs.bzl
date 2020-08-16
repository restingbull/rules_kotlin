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
load("//kotlin/internal/repositories:vars.bzl", "VARS")
load(
    "@io_bazel_rules_kotlin//kotlin/internal:defs.bzl",
    ktcpi = "KtCompilerPluginInfo",
    ktjmi = "KtJvmInfo",
    ktjsi = "KtJsInfo",
    tt = "TOOLCHAIN_TYPE",
)

KT_COMPILER_REPO = VARS.KT_COMPILER_REPO
TOOLCHAIN_TYPE = tt
JAVA_TOOLCHAIN_TYPE = VARS.JAVA_TOOLCHAIN_TYPE
KtJsInfo = ktjsi
KtJvmInfo = ktjmi
KtCompilerPluginInfo = ktcpi
JAVA_RUNTIME_TOOLCHAIN_TYPE = VARS.JAVA_RUNTIME_TOOLCHAIN_TYPE
