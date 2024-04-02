#!/usr/bin/env bash

# Path Initialization
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}}")" && pwd -P || exit 1)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd -P || exit 1)"
SOSH_DEPS_DIR="${SOSH_DEPS_DIR:-"${ROOT_DIR}/.sosh_deps"}"
export TEST_LIB_DIR="${SOSH_DEPS_DIR}/@github/rynkowsg/sosh@dev/test/res/test_suite/5_remote_sourcing_same_repo"

{
  echo "------- entry.bash - BEGIN --------"
  echo "0: ${0}"
  echo "BASH_SOURCE[0]: ${BASH_SOURCE[0]}"
  echo "SCRIPT_DIR: ${SCRIPT_DIR}"
  echo "ROOT_DIR: ${ROOT_DIR}"
  echo "SOSH_DEPS_DIR: ${SOSH_DEPS_DIR}"
  echo "TEST_LIB_DIR: ${TEST_LIB_DIR}"
  echo "------- entry.bash - END --------"
} >>/tmp/test-log.txt

# Library Sourcing
# shellcheck source=test/res/test_suite/5_remote_sourcing_same_repo/.sosh_deps/@github/rynkowsg/sosh@dev/test/res/test_suite/5_remote_sourcing_same_repo/lib/log.bash
# source "${TEST_LIB_DIR}/lib/log.bash" # BEGIN
#!/usr/bin/env bash

# detect _TEST_LIB_DIR - BEGIN
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}}")" && pwd -P || exit 1)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd -P || exit 1)"
SOSH_DEPS_DIR="${SOSH_DEPS_DIR:-"${ROOT_DIR}/.sosh_deps"}"
_TEST_LIB_DIR="${TEST_LIB_DIR:-"${SOSH_DEPS_DIR}/@github/rynkowsg/sosh@dev/test/res/test_suite/5_remote_sourcing_same_repo"}"
# detect _TEST_LIB_DIR - END

{
  echo "------- log.bash - BEGIN --------"
  echo "0: ${0}"
  echo "BASH_SOURCE[0]: ${BASH_SOURCE[0]}"
  echo "SCRIPT_PATH: ${SCRIPT_PATH}"
  echo "SCRIPT_DIR: ${SCRIPT_DIR}"
  echo "ROOT_DIR: ${ROOT_DIR}"
  echo "SOSH_DEPS_DIR: ${SOSH_DEPS_DIR}"
  echo "TEST_LIB_DIR: ${TEST_LIB_DIR}"
  echo "_TEST_LIB_DIR: ${_TEST_LIB_DIR}"
  echo "------- log.bash - END --------"
} >>/tmp/test-log.txt

# shellcheck source=test/res/test_suite/5_remote_sourcing_same_repo/.sosh_deps/@github/rynkowsg/sosh@dev/test/res/test_suite/5_remote_sourcing_same_repo/lib/color.bash
# source "${_TEST_LIB_DIR}/lib/color.bash" # BEGIN
#!/usr/bin/env bash

# shellcheck disable=SC2034
GREEN=$(printf '\033[32m')
NC=$(printf '\033[0m')
# source "${_TEST_LIB_DIR}/lib/color.bash" # END

info() {
  printf "${GREEN}%s${NC}\n" "${1}"
}
# source "${TEST_LIB_DIR}/lib/log.bash" # END

printf "${GREEN}%s${NC}\n" "All good"
