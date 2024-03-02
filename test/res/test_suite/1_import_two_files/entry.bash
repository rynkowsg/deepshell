#!/usr/bin/env bash

source ./lib/lib1.bash
source ./lib/lib2.bash

echo "This is a sample script"

echo "There is nothing, only these two logs."

lib1_run_log1
lib2_run_log2
lib2_run_log1
lib2_run_log2

