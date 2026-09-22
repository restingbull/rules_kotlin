#!/usr/bin/env bash
# aquery_gate_selftest.sh <normalizer> <identical_a> <identical_b> <changed>
#
# Exercises aquery_normalize_diff.sh over hand-authored textproto fixtures:
#   * identical_a vs identical_b differ ONLY in exec-root prefix, config-hash
#     path segments and action ordering, so they MUST normalize-equal (exit 0).
#   * identical_a vs changed carry a genuine argv difference that survives
#     normalization, so they MUST differ (exit 1).
set -euo pipefail

normalizer="$1"
identical_a="$2"
identical_b="$3"
changed="$4"

fail=0

if "$normalizer" "$identical_a" "$identical_b" >/dev/null; then
  echo "OK: identical_a and identical_b normalize-equal"
else
  echo "FAIL: identical_a and identical_b should normalize-equal but differed" >&2
  fail=1
fi

if "$normalizer" "$identical_a" "$changed" >/dev/null; then
  echo "FAIL: identical_a and changed should differ but were reported equal" >&2
  fail=1
else
  echo "OK: identical_a and changed differ as expected"
fi

exit "$fail"
