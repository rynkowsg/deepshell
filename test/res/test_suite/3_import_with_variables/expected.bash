#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# shellcheck source=test/res/test_suite/3_import_with_variables/lib/lib1.bash
# source "${SCRIPT_DIR}/lib/lib1.bash" # BEGIN
#!/usr/bin/env bash

lib1_run_log1() {
  echo "Some log from lib1 - 1"
}

lib1_run_log2() {
  echo "Some log from lib1 - 2"
}
# source "${SCRIPT_DIR}/lib/lib1.bash" # END

echo "This is a sample script"

echo "There is nothing, only these two logs."

lib1_run_log1
