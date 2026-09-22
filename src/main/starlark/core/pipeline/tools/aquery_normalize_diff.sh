#!/usr/bin/env bash
# aquery_normalize_diff.sh <baseline.textproto> <candidate.textproto>
#
# Pure text normalizer + differ for `bazel aquery --output=textproto` dumps.
# Exits 0 iff the two inputs are equal after normalization; otherwise prints a
# unified diff of the normalized forms and exits 1.
#
# Normalization removes checkout-specific noise so that a behaviour-preserving
# edit produces a byte-identical aquery WITHIN one checkout:
#   * erase the absolute exec-root prefix (.../execroot/<name>/ -> "")
#   * erase config-hash path segments (k8-fastbuild-ST-<hash> -> k8-fastbuild)
#   * sort the action list by mnemonic + primary output so action-emission
#     order is irrelevant.
# It KEEPS mnemonics, argv, input/output relative paths and env.
#
# This script performs NO bazel invocation; it is pure text over its two file
# arguments and is therefore safe to run under `bazel test`.
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "usage: $0 <baseline.textproto> <candidate.textproto>" >&2
  exit 2
fi

TAB="$(printf '\t')"

# normalize <file> -> canonical text on stdout
normalize() {
  local f="$1"
  local tagged
  tagged="$(
    sed -E \
      -e 's#/[^" ]*/execroot/[^"/ ]+/##g' \
      -e 's#-ST-[0-9a-z]+##g' \
      "$f" \
    | awk '
        function flush() {
          key = bmnem "\001" bout
          enc = blk
          gsub(/\n/, "\002", enc)
          printf "A\t%s\t%s\n", key, enc
          collecting = 0; blk = ""; bmnem = ""; bout = ""
        }
        BEGIN { collecting = 0; depth = 0; blk = ""; bmnem = ""; bout = ""; ln = 0 }
        {
          ln++
          line = $0
          o = line; c = line
          no = gsub(/\{/, "", o)
          nc = gsub(/\}/, "", c)
          if (collecting == 0) {
            if (line ~ /^actions \{/) {
              collecting = 1; depth = no - nc; blk = line; bmnem = ""; bout = ""
              if (depth <= 0) flush()
            } else {
              printf "B\t%09d\t%s\n", ln, line
            }
          } else {
            blk = blk "\n" line
            depth += no - nc
            if (line ~ /^[[:space:]]*mnemonic:/) bmnem = line
            if (line ~ /^[[:space:]]*primary_output_id:/) bout = line
            if (depth <= 0) flush()
          }
        }
      '
  )"

  # Non-action ("B") lines: keep original file order.
  printf '%s\n' "$tagged" | { grep '^B' || true; } | sort -t"$TAB" -k2,2 | cut -f3-
  # Action ("A") blocks: sort by mnemonic+primary-output key, then decode.
  printf '%s\n' "$tagged" | { grep '^A' || true; } | sort -t"$TAB" -k2,2 | cut -f3- \
    | while IFS= read -r enc; do
        [[ -n "$enc" ]] || continue
        printf '%s\n' "$enc" | tr '\002' '\n'
      done
}

tmp_a="$(mktemp)"
tmp_b="$(mktemp)"
trap 'rm -f "$tmp_a" "$tmp_b"' EXIT

normalize "$1" >"$tmp_a"
normalize "$2" >"$tmp_b"

if diff -u "$tmp_a" "$tmp_b"; then
  exit 0
fi
exit 1
