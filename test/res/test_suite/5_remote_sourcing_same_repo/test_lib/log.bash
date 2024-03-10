#!/usr/bin/env bash

# detect SCRIPT_DIR - BEGIN
SCRIPT_PATH="$([ -L "$0" ] && readlink "$0" || echo "$0")"
SCRIPT_DIR="$(cd "$(dirname "${SCRIPT_PATH}")" || exit 1; pwd -P)"
# detect SCRIPT_DIR - END

source "${SCRIPT_DIR}/color.bash"

warning() {
  printf "${YELLOW}%s${NC}\n" "${1}"
}
