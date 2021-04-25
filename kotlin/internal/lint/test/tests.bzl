load("@bazel_skylib//lib:unittest.bzl", "analysistest", "asserts")
load("//kotlin/internal/lint:ktlint_fix.bzl", "ktlint_fix")
load("//kotlin/internal/lint:ktlint_config.bzl", "KtlintConfigInfo", "ktlint_config")
load("//kotlin/internal/lint:toolchains.bzl", "TOOLCHAIN_TYPE", "ktlint_toolchain")
load("//kotlin/internal/lint:ktlint.bzl", "ktlint")
load("//kotlin/internal/utils:testing.bzl", "testing")

TEST_TOOLCHAIN = Label("//kotlin/internal/lint/test:toolchain")

def _contains_files(container, *files):
    for path in [f.short_path for f in files]:
        if not path in container:
            return dict(
                condition = False,
                msg = "Want %s in: \n%s" % (path, container),
            )

    return dict(condition = True)

def _ktlint_fix_test_impl(ctx):
    env = analysistest.begin(ctx)
    target_under_test = analysistest.target_under_test(env)
    actions = analysistest.target_actions(env)
    write_exec = [a for a in actions if a.mnemonic == "TemplateExpand"].pop()

    if write_exec:
        subs = write_exec.substitutions

        asserts.true(
            env,
            **_contains_files(
                subs["${executable}"],
                ctx.executable.expected_binary,
            )
        )
        asserts.true(
            env,
            **_contains_files(
                subs["${editorconfig_arg}"],
                ktlint.editorconfig_from(ctx.attr.expected_config),
            )
        )
        asserts.true(
            env,
            **_contains_files(subs["${srcs}"], *ctx.files.expected_srcs)
        )

        asserts.true(
            env,
            len(write_exec.content) > 0,
            msg = "Wanted content",
        )
    else:
        asserts.fail(env, "Wanted template action in %s" % actions)

    return analysistest.end(env)

_ktlint_fix_test = analysistest.make(
    _ktlint_fix_test_impl,
    config_settings = {
        "//command_line_option:extra_toolchains": str(TEST_TOOLCHAIN),
    },
    attrs = {
        "expected_binary": attr.label(executable = True, cfg = "host"),
        "expected_config": attr.label(providers = [KtlintConfigInfo]),
        "expected_srcs": attr.label_list(),
    },
)

def ktlint_test_suite(name):
    ktlint_binary = testing.executable("ktlint")
    ktlint_toolchain(
        name = TEST_TOOLCHAIN.name + "_impl",
        tool = ktlint_binary,
    )
    native.toolchain(
        name = TEST_TOOLCHAIN.name,
        toolchain = TEST_TOOLCHAIN.name + "_impl",
        toolchain_type = TOOLCHAIN_TYPE,
        visibility = ["//visibility:public"],
    )

    test_config = testing.target(
        ktlint_config,
        name = name + "_ralph_config",
        editorconfig = testing.source("t_guard"),
    )
    test_srcs = [
        testing.source("wakko.kt"),
        testing.source("yakko.kts"),
        testing.source("dot.kt"),
    ]

    native.test_suite(
        name = name,
        tests = [
            testing.case(
                _ktlint_fix_test,
                name = name + "_ktlint_fix",
                expected_config = test_config,
                expected_srcs = test_srcs,
                expected_binary = ktlint_binary,
                target_under_test = testing.target(
                    ktlint_fix,
                    name = name + "_ktlint_fix_subject",
                    srcs = test_srcs,
                    config = test_config,
                ),
            ),
        ],
    )
