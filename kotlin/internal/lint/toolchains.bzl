# Define toolchains for ktlint

TOOLCHAIN_TYPE = Label("//kotlin/internal/lint:toolchain_type")
DEFAULT_TOOLCHAIN = Label("//kotlin/internal/lint:default")
RUN_FIX_TEMPLATE = Label("//kotlin/internal/lint:ktlint_fix.template.sh")

def _ktlint_toolchain_impl(ctx):
    return [
        platform_common.ToolchainInfo(
            tool_info = ctx.attr.tool[DefaultInfo],
            run_fix_template = ctx.file.run_fix_template,
        ),
    ]

ktlint_toolchain = rule(
    implementation = _ktlint_toolchain_impl,
    attrs = {
        "tool": attr.label(
            default = "@com_github_pinterest_ktlint//file",
            executable = True,
            cfg = "target",
        ),
        "run_fix_template": attr.label(
            default = RUN_FIX_TEMPLATE,
            allow_single_file = True,
        ),
    },
)

def configure_toolchains():
    if TOOLCHAIN_TYPE.package != native.package_name():
        fail("Must be called in %s not %s" % (TOOLCHAIN_TYPE.package, native.package_name()))
    native.toolchain_type(
        name = TOOLCHAIN_TYPE.name,
        visibility = ["//visibility:public"],
    )

    native.exports_files([RUN_FIX_TEMPLATE.name])

    ktlint_toolchain(
        name = DEFAULT_TOOLCHAIN.name + "_impl",
        tool = "@com_github_pinterest_ktlint//file",
    )
    native.toolchain(
        name = DEFAULT_TOOLCHAIN.name,
        toolchain = DEFAULT_TOOLCHAIN.name + "_impl",
        toolchain_type = TOOLCHAIN_TYPE,
        visibility = ["//visibility:public"],
    )
