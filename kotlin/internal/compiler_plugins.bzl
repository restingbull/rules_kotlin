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

load(
    "//kotlin/internal:defs.bzl",
    _KtCompilerPluginInfo = "KtCompilerPluginInfo",
)

def plugins_to_classpaths(providers_list):
    flattened_files = []
    for providers in providers_list:
        if _KtCompilerPluginInfo in providers:
            provider = providers[_KtCompilerPluginInfo]
            for e in provider.classpath:
                flattened_files.append(e)
    return flattened_files

def plugins_to_options(providers_list):
    kt_compiler_plugin_providers = [
        providers[_KtCompilerPluginInfo]
        for providers in providers_list
        if _KtCompilerPluginInfo in providers
    ]
    flattened_options = []
    for provider in kt_compiler_plugin_providers:
        for option in provider.options:
            flattened_options.append("%s:%s" % (option.id, option.value))
    return flattened_options
