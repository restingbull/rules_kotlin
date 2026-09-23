"""Analysis tests for the release_jars rule (stamped jar_version.bzl + release TreeArtifact)."""

load("@rules_testing//lib:analysis_test.bzl", "analysis_test")
load("@rules_testing//lib:truth.bzl", "matching")
load(":case.bzl", "suite")

def _release_jars_assertions(env, target):
    # DefaultInfo carries the generated jar_version.bzl.
    default_basenames = [f.basename for f in target[DefaultInfo].files.to_list()]
    env.expect.that_collection(default_basenames).contains("jar_version.bzl")

    # The "release" output group is a single TreeArtifact directory.
    release_files = target[OutputGroupInfo].release.to_list()
    env.expect.that_int(len(release_files)).equals(1)
    env.expect.that_bool(release_files[0].is_directory).equals(True)

    # The ReleaseJars action threads the stamp: version file is an input and
    # the argv carries --version_file (design criterion 3).
    action = env.expect.that_target(target).action_named("ReleaseJars")
    action.argv().contains("--version_file")
    action.inputs().contains_predicate(matching.file_basename_equals("volatile-status.txt"))

def _test_release_jars_wires_stamp_and_outputs(test):
    analysis_test(
        name = test.name,
        impl = _release_jars_assertions,
        target = "//src/test/starlark:release_jars_fixture",
    )

def release_jars_test_suite(name):
    suite(
        name,
        stamp_and_outputs = _test_release_jars_wires_stamp_and_outputs,
    )
