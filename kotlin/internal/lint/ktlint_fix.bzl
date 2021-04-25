load(":ktlint.bzl", "ktlint")
load(":ktlint_config.bzl", "KtlintConfigInfo")
load("//kotlin/internal/utils:utils.bzl", "utils")

def _ktlint_fix_impl(ctx):
    return [
        ktlint.create_fix_executable(
            output = ctx.actions.declare_file("%s-lint-fix" % ctx.label.name),
            srcs = ctx.files.srcs,
            config = ctx.attr.config,
            actions = ctx.actions,
            toolchains = ctx.toolchains,
        ),
    ]

ktlint_fix = utils.configure_rule(
    implementation = _ktlint_fix_impl,
    attrs = {
        "srcs": attr.label_list(
            allow_files = [".kt", ".kts"],
            doc = "Source files to review and fix",
            mandatory = True,
            allow_empty = False,
        ),
        "config": attr.label(
            doc = "ktlint_config to use",
            providers = [
                [KtlintConfigInfo],
            ],
        ),
    },
    executable = True,
    doc = "Lint Kotlin files and automatically fix them as needed",
    configurations = [
        ktlint.rule_configuration,
    ],
)
