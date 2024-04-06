#!/usr/bin/env bash

set -euo pipefail

SCRIPT_PATH="${BASH_SOURCE[0]:-$0}"
SCRIPT_DIR="$(cd "$(dirname "${SCRIPT_PATH}")" && pwd -P || exit 1)"
SCRIPT_PATH_ABSOLUTE="${SCRIPT_DIR}/$(basename "${SCRIPT_PATH}")"
sosh fetch -i "${SCRIPT_PATH_ABSOLUTE}" >&2 || exit 1

# shellcheck source=test/res/test_suite/8_self_sosh/lib/test_log.bash
# source "${SCRIPT_DIR}/lib/test_log.bash" # BEGIN
#!/usr/bin/env bash

# This is a sample library that entry.bash refers to.
# To make it easier to maintain, I put it here, as you can see in entry.bash it is not sourced directly.
# It is sourced first with @github token and then with @https token.

printf "%s\n" "Log from library"
# source "${SCRIPT_DIR}/lib/test_log.bash" # END

printf "%s\n" "All good"
