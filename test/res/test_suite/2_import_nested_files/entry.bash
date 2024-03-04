#!/usr/bin/env bash

# shellcheck source=test/res/test_suite/2_import_nested_files/lib/lib1.bash
source ./lib/lib1.bash

echo "This is a sample script"

echo "There is nothing, only these two logs."

lib1_run_log
