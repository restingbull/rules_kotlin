load(":ktlint_config.bzl", "KtlintConfigInfo")
load(":toolchains.bzl", "TOOLCHAIN_TYPE")

def _editorconfig_from(target):
    return target[KtlintConfigInfo].editorconfig if target and KtlintConfigInfo in target else None

def _toolchain_from(toolchains):
    return toolchains[TOOLCHAIN_TYPE]

def _rule_configuration(toolchains = [], **kwargs):
    return dict(
        toolchains = toolchains + [str(TOOLCHAIN_TYPE)],
        **kwargs
    )

def _create_fix_executable(
        output,
        srcs,
        config,
        toolchains,
        actions):
    """
    Args:
        output: File for the action
        srcs: List[File] to fix
        config: Target[KtlintConfigInfo?]
        toolchains: ToolchainContext
        actions: ctx.actions used to create the correct actions
    Returns:
        DefaultInfo containing the ktlint fix executable.
    """
    editorconfig = _editorconfig_from(config)
    editorconfig_arg = "--editorconfig={file}".format(file = editorconfig.path) if editorconfig else ""

    ktlint_toolchain = _toolchain_from(toolchains)

    actions.expand_template(
        template = ktlint_toolchain.run_fix_template,
        output = output,
        substitutions = {
            "${executable}": ktlint_toolchain.tool_info.files_to_run.executable.path,
            "${editorconfig_arg}": editorconfig_arg,
            "${srcs}": " ".join([src.path for src in srcs]),
        },
        is_executable = True,
    )

    return DefaultInfo(
        executable = output,
        runfiles = ktlint_toolchain.tool_info.default_runfiles,
    )

ktlint = struct(
    editorconfig_from = _editorconfig_from,
    toolchain_from = _toolchain_from,
    rule_configuration = _rule_configuration,
    create_fix_executable = _create_fix_executable,
)
