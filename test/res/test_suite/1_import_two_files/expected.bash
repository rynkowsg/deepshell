#!/usr/bin/env bash

# shellcheck source=test/res/test_suite/1_import_two_files/lib/lib1.bash
# source ./lib/lib1.bash # BEGIN
#!/usr/bin/env bash

lib1_run_log1() {
  echo "Some log from lib1 - 1"
}

lib1_run_log2() {
  echo "Some log from lib1 - 2"
}
# source ./lib/lib1.bash # END
# shellcheck source=test/res/test_suite/1_import_two_files/lib/lib2.bash
# source ./lib/lib2.bash # BEGIN
#!/usr/bin/env bash

lib2_run_log1() {
  echo "Some log from lib2 - 1"
}

lib2_run_log2() {
  echo "Some log from lib2 - 2"
}
# source ./lib/lib2.bash # END

echo "This is a sample script"

echo "There is nothing, only these two logs."

lib1_run_log1
lib2_run_log2
lib2_run_log1
lib2_run_log2
