versions = struct(
    RULES_KOTLIN = struct(
        urls = [
            "https://github.com/bazelbuild/rules_kotlin/archive/v1.5.0-beta-3.zip",
        ],
        sha256 = "d8650bb939d87a080448ffbbbd1594f5ae054738df5d9471941c18784aa139b9",
        prefix = "rules_kotlin-1.5.0-beta-3",
    ),
)

def archive_repository_implementation(repository_ctx):
    attr = repository_ctx.attr

    # In a perfect world, the local archive path would be set via a build config setting
    # Alas, a repository label is never replaced with a target -- meaning repository_rules are
    # evaluated first (reasonable) but disallow using settings as they need to be targets.
    # One day, one hopes that there will be a flag to control repository behavior.
    # Instead, environment variables are a tried and true workaround.
    environ = repository_ctx.os.environ

    if attr.env_archive in environ:
        repository_ctx.extract(
            archive = environ[attr.env_archive],
            stripPrefix = environ.get(attr.env_archive_prefix, ""),
        )
    elif attr.env_stable in environ:
        repository_ctx.download_and_extract(
            attr._remote_urls,
            sha256 = attr._remote_sha256,
            stripPrefix = attr._remote_prefix,
        )
    else:
        # not OS safe. Presuming linux-likes until complaints.
        sep = "/"
        base = None
        if attr._local_repository_path.startswith(sep):
            base = repository_ctx.path(attr._local_repository_path)
        else:
            base = repository_ctx.path(environ["PWD"] + sep + attr._local_repository_path)

        release_archive = attr._local_release_archive_target

        target = "//%s:%s" % (release_archive.package, release_archive.name)

        repository_ctx.report_progress("Building %s in %s... (may take a while.)" % (target, base))
        result = repository_ctx.execute(
            [
                "bazel",
                "build",
                target,
            ],
            working_directory = str(base),
            timeout = 1200,  # build take a minute.
        )
        if result.return_code:
            fail("%s: %s\n%s" % (str(base), result.stderr, result.stdout))
        release_artifact = base.get_child("bazel-bin")
        if release_archive.package:
            release_artifact = release_artifact.get_child(release_archive.package)
        release_artifact = release_artifact.get_child(release_archive.name + ".tgz")

        repository_ctx.extract(
            archive = release_artifact,
        )

archive_repository = repository_rule(
    implementation = archive_repository_implementation,
    attrs = {
        "_remote_urls": attr.string_list(
            doc = "A list of urls for the archive",
            default = versions.RULES_KOTLIN.urls,
        ),
        "_remote_sha256": attr.string(
            doc = "sha256 of the archive",
            default = versions.RULES_KOTLIN.sha256,
        ),
        "_remote_prefix": attr.string(
            doc = "prefix to remove from the remote archive",
            default = versions.RULES_KOTLIN.prefix,
        ),
        "local_repository_path": attr.string(
            doc = "local repository directory",
            default = "../..",
        ),
        "local_release_archive_target": attr.label(
            doc = "release_archive rule.",
            default = Label("//:rules_kotlin_release"),
        ),
        "env_archive": attr.string(
            doc = "release archive path environment variable name",
            default = "RULES_KOTLIN_ARCHIVE",
        ),
        "env_archive_prefix": attr.string(
            doc = "release archive path environment variable name",
            default = "RULES_KOTLIN_ARCHIVE_PREFIX",
        ),
        "env_stable": attr.string(
            doc = "Use stable release",
            default = "RULES_KOTLIN_STABLE",
        ),
    },
)
