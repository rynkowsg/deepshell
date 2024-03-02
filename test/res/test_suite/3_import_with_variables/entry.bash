#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source ${SCRIPT_DIR}/lib/lib1.bash

echo "This is a sample script"

echo "There is nothing, only these two logs."

lib1_run_log1
