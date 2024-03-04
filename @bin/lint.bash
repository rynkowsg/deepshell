#!/usr/bin/env bash

set -uo pipefail

find . -type f \( -name '*.bash' -o -name '*.sh' \) | \
  grep -vE 'test/res/.*/(output|expected)\.bash$' | \
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
