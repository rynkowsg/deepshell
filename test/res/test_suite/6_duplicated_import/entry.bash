#!/usr/bin/env bash

# Path Initialization
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}}")" && pwd -P || exit 1)"
# Library Sourcing
# shellcheck source=test/res/test_suite/6_duplicated_import/lib/color.bash
source "${SCRIPT_DIR}/lib/color.bash"
# shellcheck source=test/res/test_suite/6_duplicated_import/lib/log.bash
source "${SCRIPT_DIR}/lib/log.bash"

printf "${GREEN}%s${NC}\n" "Print log setting colors myself"
info "Print via info function"
