# Source of jar identity + current sha for the single-jar-shipping dependencies.
"""Pinned single-jar dependency versions extracted from versions.bzl.

These entries ship as a single downloaded jar (via http_file). They are the source of
jar identity (url templates + version) and the current sha256. The release pipeline
(//src/main/starlark/release:release_jars) regenerates the checked-in jar_version.bzl
placeholder from these at release time.
"""

load(":versions.bzl", "version")

# Keep in sync with _KOTLIN_CURRENT_RELEASE in versions.bzl: the Build Tools API interfaces
# jar ships from the same Kotlin compiler release train.
_KOTLIN_CURRENT_RELEASE = "2.4.20"

PINTEREST_KTLINT = version(
    version = "1.8.0",
    url_templates = [
        "https://github.com/pinterest/ktlint/releases/download/{version}/ktlint",
    ],
    sha256 = "a3fd620207d5c40da6ca789b95e7f823c54e854b7fade7f613e91096a3706d75",
)

# Starting with Kotlin 2.4.0 the Build Tools API interfaces are no longer bundled in
# kotlin-compiler.jar, so they must be provided as a separate jar.
KOTLIN_BUILD_TOOLS_API = version(
    version = _KOTLIN_CURRENT_RELEASE,
    url_templates = [
        "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-build-tools-api/{version}/kotlin-build-tools-api-{version}.jar",
    ],
    sha256 = "47a622dce7231b1916334b69a00bc1094adf6577e6492f9f06b3d9c2450fe459",
)

KOTLINX_SERIALIZATION_CORE_JVM = version(
    version = "1.8.1",
    url_templates = [
        "https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-core-jvm/{version}/kotlinx-serialization-core-jvm-{version}.jar",
    ],
    sha256 = "3565b6d4d789bf70683c45566944287fc1d8dc75c23d98bd87d01059cc76f2b3",
)

KOTLINX_SERIALIZATION_JSON = version(
    version = "1.8.1",
    url_templates = [
        "https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-json/{version}/kotlinx-serialization-json-{version}.jar",
    ],
    sha256 = "58adf3358a0f99dd8d66a550fbe19064d395e0d5f7f1e46515cd3470a56fbbb0",
)

KOTLINX_SERIALIZATION_JSON_JVM = version(
    version = "1.8.1",
    url_templates = [
        "https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-json-jvm/{version}/kotlinx-serialization-json-jvm-{version}.jar",
    ],
    sha256 = "8769e5647557e3700919c32d508f5c5dad53c5d8234cd10846354fbcff14aa24",
)

KOTLINX_COROUTINES_CORE_JVM = version(
    version = "1.10.2",
    url_templates = [
        "https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core-jvm/{version}/kotlinx-coroutines-core-jvm-{version}.jar",
    ],
    sha256 = "5ca175b38df331fd64155b35cd8cae1251fa9ee369709b36d42e0a288ccce3fd",
)

# Aggregate for iteration by the release pipeline and for consumption in initialize.release.bzl.
JAR_VERSIONS = struct(
    PINTEREST_KTLINT = PINTEREST_KTLINT,
    KOTLIN_BUILD_TOOLS_API = KOTLIN_BUILD_TOOLS_API,
    KOTLINX_SERIALIZATION_CORE_JVM = KOTLINX_SERIALIZATION_CORE_JVM,
    KOTLINX_SERIALIZATION_JSON = KOTLINX_SERIALIZATION_JSON,
    KOTLINX_SERIALIZATION_JSON_JVM = KOTLINX_SERIALIZATION_JSON_JVM,
    KOTLINX_COROUTINES_CORE_JVM = KOTLINX_COROUTINES_CORE_JVM,
)
