#!/usr/bin/env bash

# shellcheck source=test/res/test_suite/1_import_two_files/lib/lib1.bash
source ./lib/lib1.bash
# shellcheck source=test/res/test_suite/1_import_two_files/lib/lib2.bash
source ./lib/lib2.bash

echo "This is a sample script"

echo "There is nothing, only these two logs."

lib1_run_log1
lib2_run_log2
lib2_run_log1
lib2_run_log2
