#!/usr/bin/env bash

# Path Initialization
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}}")" && pwd -P || exit 1)"
# Library Sourcing
# shellcheck source=test/res/test_suite/6_duplicated_import/lib/color.bash
# source "${SCRIPT_DIR}/lib/color.bash" # BEGIN
#!/usr/bin/env bash

# shellcheck disable=SC2034
GREEN=$(printf '\033[32m')
NC=$(printf '\033[0m')
# source "${SCRIPT_DIR}/lib/color.bash" # END
# shellcheck source=test/res/test_suite/6_duplicated_import/lib/log.bash
# source "${SCRIPT_DIR}/lib/log.bash" # BEGIN
# source "${SCRIPT_DIR}/lib/color.bash" # SKIPPED

info() {
  printf "${GREEN}%s${NC}\n" "${1}"
}
# source "${SCRIPT_DIR}/lib/log.bash" # END

printf "${GREEN}%s${NC}\n" "Print log setting colors myself"
info "Print via info function"
