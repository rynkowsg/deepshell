#!/usr/bin/env bats
#  Copyright (c) 2024 Greg Rynkowski. All rights reserved.
#  License: MIT License

# Path Initialization
_TEST_DIR="$(cd "$(dirname "${BATS_TEST_FILENAME:-"${BASH_SOURCE[0]:-$0}"}")" && pwd -P || exit 1)"
_ROOT_DIR="$(cd "${_TEST_DIR}" && pwd -P || exit 1)"
_SHELL_GR_DIR="${_ROOT_DIR}"
# Library Sourcing

# source "${_SHELL_GR_DIR}/lib/color.bash" # BEGIN
#!/usr/bin/env bash

# shellcheck disable=SC2034
GREEN=$(printf '\033[32m')
NC=$(printf '\033[0m')
# source "${_SHELL_GR_DIR}/lib/color.bash" # END

printf "${GREEN}%s${NC}\n" "All good"
