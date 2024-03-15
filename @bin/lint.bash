#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P || exit 1)"
REPO_DIR="$(cd "${SCRIPT_DIR}/.." && pwd -P || exit 1)"

# linter will complain if I don't fetch deps for 4_import_remote
"${REPO_DIR}/main/src/pl/rynkowski/shellpack.cljc" fetch "${REPO_DIR}/test/res/test_suite/4_import_remote/entry.bash"

find . -type f \( -name '*.bash' -o -name '*.sh' \) | \
  grep -vE 'test/res/.*/(output|expected|.*@github.*|.*@http.*)\.bash$' | \
  while IFS= read -r file; do
    echo "Processing file: $file"
     shellcheck \
      --shell=bash \
      --external-sources \
      "${file}"
  done

find . -type f -name '*.bats'| \
  while IFS= read -r file; do
    echo "Processing file: $file"
     shellcheck \
      --shell=bash \
      --external-sources \
      "${file}"
  done
