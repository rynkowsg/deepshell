#!/usr/bin/env bats
#  Copyright (c) 2024 Greg Rynkowski. All rights reserved.
#  License: MIT License

# Path Initialization
_TEST_DIR="$(cd "$(dirname "${BATS_TEST_FILENAME:-"${BASH_SOURCE[0]:-$0}"}")" && pwd -P || exit 1)"
_ROOT_DIR="$(cd "${_TEST_DIR}" && pwd -P || exit 1)"
_SHELL_GR_DIR="${_ROOT_DIR}"
# Library Sourcing

source "${_SHELL_GR_DIR}/lib/color.bash"

printf "${GREEN}%s${NC}\n" "All good"
