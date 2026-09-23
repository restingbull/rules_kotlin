#!/usr/bin/env bash

set -o errexit -o nounset -o pipefail

# Set by GH actions, see
# https://docs.github.com/en/actions/learn-github-actions/environment-variables#default-environment-variables
TAG=${GITHUB_REF_NAME}
# The prefix is chosen to match what GitHub generates for source archives
PREFIX="rules_kotlin-${TAG:1}"
ARCHIVE="rules_kotlin-$TAG.tar.gz"
bazel --bazelrc=.github/workflows/ci.bazelrc --bazelrc=.bazelrc build --stamp //:rules_kotlin_release
cp bazel-bin/rules_kotlin_release.tgz $ARCHIVE
SHA=$(shasum -a 256 $ARCHIVE | awk '{print $1}')

# Build the stamped per-jar release blobs and stage them for upload.
# The `release` output group of jar_version_generated is a TreeArtifact directory
# of version-stamped <name>-<version>.jar files. --stamp bakes the release version
# into each filename; we copy the directory contents into ./blobs/ so the GH release
# action can upload them by glob (blobs/*.jar) and newly-added jars ship automatically.
JAR_VERSION_TARGET="//src/main/starlark/core/repositories:jar_version_generated"
bazel --bazelrc=.github/workflows/ci.bazelrc --bazelrc=.bazelrc build --stamp --output_groups=release "$JAR_VERSION_TARGET"
rm -rf blobs
mkdir -p blobs
cp -R bazel-bin/src/main/starlark/core/repositories/jar_version_generated/release_blobs/. blobs/

# Write the release notes to release_notes.txt
cat > release_notes.txt << EOF
# Release notes for $TAG
## Using Bzlmod with Bazel 7

1. Enable with \`common --enable_bzlmod\` in \`.bazelrc\`.
2. Add to your \`MODULE.bazel\` file:

\`\`\`starlark
bazel_dep(name = "rules_kotlin", version = "${TAG:1}")
\`\`\`

## Using WORKSPACE

Paste this snippet into your \`WORKSPACE.bazel\` file:

\`\`\`starlark
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
http_archive(
    name = "rules_kotlin",
    sha256 = "${SHA}",
    url = "https://github.com/bazel-contrib/rules_kotlin/releases/download/${TAG}/${ARCHIVE}",
)

load("@rules_kotlin//kotlin:repositories.bzl", "kotlin_repositories")
kotlin_repositories() # if you want the default. Otherwise see custom kotlinc distribution below

load("@rules_kotlin//kotlin:core.bzl", "kt_register_toolchains")
kt_register_toolchains() # to use the default toolchain, otherwise see toolchains below
\`\`\`
EOF
