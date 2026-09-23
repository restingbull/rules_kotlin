# Copyright 2026 The Bazel Authors. All rights reserved.
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
"""The release_jars rule: stamps per-jar sha256/urls into jar_version.bzl.

Wraps the io.bazel.kotlin.builder.cmd.ReleaseJars worker in a ctx.actions.run,
threading ctx.version_file (the --stamp volatile status) so the emitted version
tracks the stamped release. Produces:

  * DefaultInfo(files = [<name>/jar_version.bzl]) - the generated Starlark table.
  * OutputGroupInfo(release = [<name>/release_blobs]) - a TreeArtifact directory
    of version-stamped jars (filenames are not predeclared).
"""

def _release_jars_impl(ctx):
    out_bzl = ctx.actions.declare_file(ctx.label.name + "/jar_version.bzl")
    out_dir = ctx.actions.declare_directory(ctx.label.name + "/release_blobs")

    args = ctx.actions.args()
    args.add("--version_file", ctx.version_file)
    args.add("--version_key", ctx.attr.version_key)
    args.add("--out_bzl", out_bzl)
    args.add("--out_dir", out_dir.path)

    jar_files = []
    for jar_target, name in ctx.attr.jars.items():
        jar = jar_target.files.to_list()[0]
        jar_files.append(jar)
        args.add("--jar", name + "=" + jar.path)
        args.add("--url", name + "=" + ctx.attr.urls.get(name, ""))

    ctx.actions.run(
        mnemonic = "ReleaseJars",
        executable = ctx.executable._worker,
        arguments = [args],
        inputs = depset(jar_files + [ctx.version_file]),
        outputs = [out_bzl, out_dir],
    )

    return [
        DefaultInfo(files = depset([out_bzl])),
        OutputGroupInfo(release = depset([out_dir])),
    ]

release_jars = rule(
    implementation = _release_jars_impl,
    doc = "Stamps per-jar sha256/urls into a jar_version.bzl and a release TreeArtifact.",
    attrs = {
        "jars": attr.label_keyed_string_dict(
            allow_files = [".jar"],
            doc = "jar file -> logical name (from version.jar.bzl-driven filegroups).",
        ),
        "urls": attr.string_dict(
            doc = "logical name -> url template (parallels jars).",
        ),
        "version_key": attr.string(
            default = "VERSION",
            doc = "Status-file key holding the release version.",
        ),
        "_worker": attr.label(
            default = "//src/main/kotlin/io/bazel/kotlin/builder/cmd:release_jars",
            executable = True,
            cfg = "exec",
        ),
    },
)
