#!/usr/bin/env bash

# shellcheck source=test/res/test_suite/2_import_nested_files/lib/internal/lib.bash
source ./lib/internal/lib.bash

lib1_run_log() {
  internal_lib_log
  echo "Some log from lib1 - 1"
}
