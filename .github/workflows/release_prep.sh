#!/usr/bin/env bash

set -o errexit -o nounset -o pipefail

# Set by GH actions, see
# https://docs.github.com/en/actions/learn-github-actions/environment-variables#default-environment-variables
TAG=${GITHUB_REF_NAME}
# The prefix is chosen to match what GitHub generates for source archives
PREFIX="rules_kotlin-${TAG:1}"
ARCHIVE="rules_kotlin-$TAG.tar.gz"
VERSION="${TAG:1}" # e.g. v1.9.5 -> 1.9.5

# The 8 worker/plugin jars published as standalone GitHub release assets. Each maps its build label to
# "<stable-asset-filename>:<versions.bzl sha256 placeholder token>". These are built, copied to their
# stable filenames, sha256-hashed, and stamped into versions.bzl BEFORE the release tarball is built.
declare -A JARS=(
  ["//src/main/kotlin/io/bazel/kotlin/builder/cmd:build_deploy.jar"]="kotlin_worker.jar:PLACEHOLDER_SHA256_kotlin_worker"
  ["//src/main/kotlin/io/bazel/kotlin/builder/cmd:merge_jdeps_deploy.jar"]="jdeps_merger_worker.jar:PLACEHOLDER_SHA256_jdeps_merger_worker"
  ["//src/main/kotlin/io/bazel/kotlin/builder/cmd:ksp2_deploy.jar"]="ksp2_worker.jar:PLACEHOLDER_SHA256_ksp2_worker"
  ["//src/main/kotlin/io/bazel/kotlin/ksp2:ksp2.jar"]="ksp2_invoker.jar:PLACEHOLDER_SHA256_ksp2_invoker"
  ["//src/main/kotlin:skip-code-gen.jar"]="skip-code-gen.jar:PLACEHOLDER_SHA256_skip_code_gen"
  ["//src/main/kotlin:jdeps-gen.jar"]="jdeps-gen.jar:PLACEHOLDER_SHA256_jdeps_gen"
  ["//src/main/kotlin:skip-code-gen-embeddable.jar"]="skip-code-gen-embeddable.jar:PLACEHOLDER_SHA256_skip_code_gen_embeddable"
  ["//src/main/kotlin:jdeps-gen-embeddable.jar"]="jdeps-gen-embeddable.jar:PLACEHOLDER_SHA256_jdeps_gen_embeddable"
)

# 1. build all 8 jar targets in one invocation
bazel --bazelrc=.github/workflows/ci.bazelrc --bazelrc=.bazelrc build "${!JARS[@]}"

# 2. for each: locate the built jar under bazel-bin, cp to its stable asset filename, sha256, stamp the sha
VERSIONS_BZL="src/main/starlark/core/repositories/versions.bzl"
for label in "${!JARS[@]}"; do
  asset="${JARS[$label]%%:*}"
  token="${JARS[$label]##*:}"
  out=$(bazel --bazelrc=.github/workflows/ci.bazelrc --bazelrc=.bazelrc cquery --output=files "$label")
  cp "$out" "$asset"
  sha=$(shasum -a 256 "$asset" | awk '{print $1}')
  # Anchor the match on the closing quote so a shorter token (e.g. ..._skip_code_gen) cannot match
  # inside a longer one (..._skip_code_gen_embeddable); associative-array iteration order is unspecified.
  sed -i.bak "s|${token}\"|${sha}\"|g" "$VERSIONS_BZL"
done

# 3. stamp the release version placeholder (jar {version}) + keep the MODULE version in lockstep.
# Scope the MODULE stamp to the module() block so bazel_dep versions are untouched.
sed -i.bak "s|0.0.0-UNSTAMPED|$VERSION|g" "$VERSIONS_BZL"
sed -i.bak "/^module(/,/^)/ s|version = \"[^\"]*\"|version = \"$VERSION\"|" MODULE.release.bazel
rm -f "$VERSIONS_BZL.bak" MODULE.release.bazel.bak

# Fail loudly if any placeholder survived stamping: sed exits 0 on zero matches, so a token/name drift
# would otherwise silently ship literal placeholders (bogus sha256 / unstamped version) to consumers.
if grep -qE 'PLACEHOLDER_SHA256_|0\.0\.0-UNSTAMPED' "$VERSIONS_BZL"; then
  echo "ERROR: unstamped placeholder(s) remain in $VERSIONS_BZL after release stamping:" >&2
  grep -nE 'PLACEHOLDER_SHA256_|0\.0\.0-UNSTAMPED' "$VERSIONS_BZL" >&2
  exit 1
fi

bazel --bazelrc=.github/workflows/ci.bazelrc --bazelrc=.bazelrc build //:rules_kotlin_release
cp bazel-bin/rules_kotlin_release.tgz $ARCHIVE
SHA=$(shasum -a 256 $ARCHIVE | awk '{print $1}')

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
