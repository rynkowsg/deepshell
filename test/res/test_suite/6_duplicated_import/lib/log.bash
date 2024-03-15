#!/usr/bin/env bash

# Path Initialization
SCRIPT_DIR="${SCRIPT_DIR:-"$(cd "$(dirname "${BASH_SOURCE[0]}}")" && pwd -P || exit 1)"}"
# Library Sourcing
# shellcheck source=test/res/test_suite/6_duplicated_import/lib/color.bash
source "${SCRIPT_DIR}/lib/color.bash"

info() {
  printf "${GREEN}%s${NC}\n" "${1}"
}
