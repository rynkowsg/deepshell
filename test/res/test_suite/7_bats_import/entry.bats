#!/usr/bin/env bats
#  Copyright (c) 2024 Greg Rynkowski. All rights reserved.
#  License: MIT License

# Path Initialization
_SCRIPT_PATH=$([[ $0 =~ /bats-exec-test$ ]] && echo "${BATS_TEST_FILENAME}" || echo "${BASH_SOURCE[0]:-$0}")
_TEST_DIR="$(cd "$(dirname "${_SCRIPT_PATH}")" && pwd -P || exit 1)"
_ROOT_DIR="$(cd "${_TEST_DIR}" && pwd -P || exit 1)"
_SHELL_GR_DIR="${_ROOT_DIR}"
# Library Sourcing
# shellcheck source=test/res/test_suite/7_bats_import/lib/color.bash
source "${_SHELL_GR_DIR}/lib/color.bash"

printf "${GREEN}%s${NC}\n" "All good"
