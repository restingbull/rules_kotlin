# Testing utilities
load("@bazel_skylib//lib:unittest.bzl", "analysistest", "asserts")

def _target(
        define_rule,
        name,
        **kwargs):
    define_rule(
        name = name,
        **kwargs
    )
    return name

def _executable_stub_impl(ctx):
    file = ctx.actions.declare_file(ctx.label.name)
    ctx.actions.write(
        output = file,
        content = "<dummy exec>",
        is_executable = True,
    )
    return DefaultInfo(
        executable = file,
    )

_executable_stub = rule(
    implementation = _executable_stub_impl,
    executable = True,
)

def _executable(name):
    return _target(
        _executable_stub,
        name = name,
    )

def _source_stub_impl(ctx):
    file = ctx.actions.declare_file(ctx.label.name)
    ctx.actions.write(
        output = file,
        content = "<dummy file>",
    )
    return DefaultInfo(
        files = depset([file]),
    )

_source_stub = rule(
    implementation = _source_stub_impl,
)

def _source(name):
    return _target(
        _source_stub,
        name = name,
    )

testing = struct(
    source = _source,
    executable = _executable,
    target = _target,
    case = _target,
)
