#!/usr/bin/env bash

source ./lib/internal/lib.bash

lib1_run_log() {
  internal_lib_log
  echo "Some log from lib1 - 1"
}
