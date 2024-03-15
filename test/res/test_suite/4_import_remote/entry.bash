#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P || exit 1)"

# 1. Here we import by @github token:

# shellcheck source=test/res/test_suite/4_import_remote/lib/@github/rynkowsg/shellpack@main/test/res/test_suite/4_import_remote/test_lib/test_log.bash
source "${SCRIPT_DIR}/lib/@github/rynkowsg/shellpack@main/test/res/test_suite/4_import_remote/test_lib/test_log.bash"

# 2. And here by @https token:

# shellcheck source=test/res/test_suite/4_import_remote/lib/@https/raw.githubusercontent.com/rynkowsg/shellpack/main/test/res/test_suite/4_import_remote/test_lib/test_log.bash
source "${SCRIPT_DIR}/lib/@https/raw.githubusercontent.com/rynkowsg/shellpack/main/test/res/test_suite/4_import_remote/test_lib/test_log.bash"

printf "${GREEN}%s${NC}\n" "All good"
