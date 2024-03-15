#!/usr/bin/env bash

# detect _TEST_LIB_DIR - BEGIN
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}}")" && pwd -P || exit 1)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd -P || exit 1)"
SHELLPACK_DEPS_DIR="${SHELLPACK_DEPS_DIR:-"${ROOT_DIR}/.shellpack_deps"}"
_TEST_LIB_DIR="${TEST_LIB_DIR:-"${SHELLPACK_DEPS_DIR}/@github/rynkowsg/shellpack@dev/test/res/test_suite/5_remote_sourcing_same_repo"}"
# detect _TEST_LIB_DIR - END

{
  echo "------- log.bash - BEGIN --------"
  echo "0: ${0}"
  echo "BASH_SOURCE[0]: ${BASH_SOURCE[0]}"
  echo "SCRIPT_PATH: ${SCRIPT_PATH}"
  echo "SCRIPT_DIR: ${SCRIPT_DIR}"
  echo "ROOT_DIR: ${ROOT_DIR}"
  echo "SHELLPACK_DEPS_DIR: ${SHELLPACK_DEPS_DIR}"
  echo "TEST_LIB_DIR: ${TEST_LIB_DIR}"
  echo "_TEST_LIB_DIR: ${_TEST_LIB_DIR}"
  echo "------- log.bash - END --------"
} >>/tmp/test-log.txt

# shellcheck source=test/res/test_suite/5_remote_sourcing_same_repo/.shellpack_deps/@github/rynkowsg/shellpack@dev/test/res/test_suite/5_remote_sourcing_same_repo/lib/color.bash
source "${_TEST_LIB_DIR}/lib/color.bash"

info() {
  printf "${GREEN}%s${NC}\n" "${1}"
}
