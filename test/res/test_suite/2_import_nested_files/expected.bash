#!/usr/bin/env bash

# shellcheck source=test/res/test_suite/2_import_nested_files/lib/lib1.bash
# source ./lib/lib1.bash # BEGIN
#!/usr/bin/env bash

# shellcheck source=test/res/test_suite/2_import_nested_files/lib/internal/lib.bash
# source ./lib/internal/lib.bash # BEGIN
#!/usr/bin/env bash

internal_lib_log() {
  echo "Some log from internal/lib"
}
# source ./lib/internal/lib.bash # END


lib1_run_log() {
  internal_lib_log
  echo "Some log from lib1 - 1"
}
# source ./lib/lib1.bash # END


echo "This is a sample script"

echo "There is nothing, only these two logs."

lib1_run_log
