#!/usr/bin/env bash
# Dry-run oracle for the stamped release pipeline (no real tag pushed).
#
# Proves the two release artifacts wire up the stamped per-jar blob flow:
#   * release_prep.sh performs a --stamp build of the `release` output group of
#     the jar_version_generated TreeArtifact and stages its contents into ./blobs.
#   * release.yml uploads those blobs by GLOB (blobs/*.jar), not an explicit list,
#     alongside the existing release tgz, keeping fail_on_unmatched_files.
#
# End-to-end it stages the blobs and asserts every staged file is NON-EMPTY and
# carries the STAMPED version (proving --stamp took effect). The stamped version
# is read from the generated jar_version.bzl VERSION rather than
# bazel-out/volatile-status.txt: BUILD_TIMESTAMP in volatile-status churns on
# every invocation while the cached ReleaseJars action keeps its original stamp,
# so the generated file is the only self-consistent source of the value that was
# actually baked into the blob filenames.

set -o errexit -o nounset -o pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$REPO_ROOT"

PREP=".github/workflows/release_prep.sh"
YML=".github/workflows/release.yml"
JAR_TARGET="//src/main/starlark/core/repositories:jar_version_generated"
BLOBS_SRC="bazel-bin/src/main/starlark/core/repositories/jar_version_generated/release_blobs"
GENERATED_BZL="bazel-bin/src/main/starlark/core/repositories/jar_version_generated/jar_version.bzl"

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

# --- Static wiring: release_prep.sh ---------------------------------------
grep -q -- '--stamp' "$PREP" || fail "release_prep.sh missing --stamp build"
grep -q -- '--output_groups=release' "$PREP" ||
  fail "release_prep.sh does not build the release output group"
grep -q 'jar_version_generated' "$PREP" ||
  fail "release_prep.sh does not build the jar_version_generated target"
grep -q 'blobs' "$PREP" || fail "release_prep.sh does not stage blobs"

# --- Static wiring: release.yml (upload by GLOB) --------------------------
grep -q 'blobs/\*\.jar' "$YML" || fail "release.yml does not upload blobs/*.jar by glob"
grep -q 'fail_on_unmatched_files' "$YML" || fail "release.yml dropped fail_on_unmatched_files"
grep -q 'rules_kotlin-\*\.tar\.gz' "$YML" || fail "release.yml dropped the release tgz glob"

# --- Dynamic oracle: stamped build + staging carries the version ----------
rm -rf blobs
bazel build --stamp --output_groups=release "$JAR_TARGET"
mkdir -p blobs
cp -R "$BLOBS_SRC/." blobs/

test -f "$GENERATED_BZL" || fail "generated jar_version.bzl absent"
VERSION="$(awk -F'"' '$1 ~ /^VERSION =/ {print $2}' "$GENERATED_BZL")"
test -n "$VERSION" || fail "no stamped VERSION in generated jar_version.bzl"

ls blobs/*.jar >/dev/null 2>&1 || fail "no jars staged in ./blobs"
if find blobs -type f -size 0 | grep -q .; then
  fail "zero-byte blob staged"
fi
for f in blobs/*.jar; do
  case "$f" in
    *-"$VERSION".jar) ;;
    *) fail "$f missing stamped -$VERSION" ;;
  esac
done

echo "OK: all blobs non-empty and stamped -$VERSION"
