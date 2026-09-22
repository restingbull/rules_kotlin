"""Released bzlmod module extension setup for rules_kotlin."""

load("@bazel_skylib//lib:modules.bzl", "modules")
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_jar")
load(
    "//src/main/starlark/core/repositories:initialize.bzl",
    _kotlin_repositories = "kotlin_repositories",
    _kotlinc_version = "kotlinc_version",
    _ksp_version = "ksp_version",
)
load(
    ":bzlmod_impl.bzl",
    "configure_modules_and_repositories",
    "tag_classes",
)
load(":versions.bzl", _versions = "versions")

def _rules_kotlin_extensions_impl(mctx):
    configure_modules_and_repositories(
        mctx.modules,
        _kotlin_repositories,
        _kotlinc_version,
        _ksp_version,
    )

    # Create one sha256-pinned http_jar repo per released worker/plugin jar. use_all_repos below
    # then exports them. Dev builds (bzlmod_setup.bzl) never reach this path, so they download none.
    for repo_name, jar_version in _versions.RELEASE_WORKER_JARS:
        _versions.use_repository(http_jar, name = repo_name, version = jar_version)

    return modules.use_all_repos(mctx, reproducible = True)

rules_kotlin_extensions = module_extension(
    implementation = _rules_kotlin_extensions_impl,
    tag_classes = tag_classes,
)
