#!/usr/bin/env bats

# detect ROOT_DIR - BEGIN
TEST_DIR="$(cd "$(dirname "${BATS_TEST_FILENAME}")" || exit 1; pwd -P)"
REPO_DIR="$(cd "${TEST_DIR}/../.." || exit 1; pwd -P)"
# detect ROOT_DIR - end

test_composed_as_expected() {
  local name=$1
  (cd "${REPO_DIR}/test/res/test_suite/${name}" || exit 1; "${REPO_DIR}/main/src/pl/rynkowski/shellpack.bb" --cwd "${REPO_DIR}/test/res/test_suite/${name}" --entry "${REPO_DIR}/test/res/test_suite/${name}/entry.bash" --output "${REPO_DIR}/test/res/test_suite/${name}/output.bash")
  local output_value expected_value
  output_value="$(cat "${REPO_DIR}/test/res/test_suite/${name}/output.bash")"
  expected_value="$(cat "${REPO_DIR}/test/res/test_suite/${name}/expected.bash")"
  [ "${output_value}" == "${expected_value}" ]
}

test_compare_results() {
  local name=$1
  "${REPO_DIR}/main/src/pl/rynkowski/shellpack.bb" --cwd "${REPO_DIR}/test/res/test_suite/${name}" --entry "${REPO_DIR}/test/res/test_suite/${name}/entry.bash" --output "${REPO_DIR}/test/res/test_suite/${name}/output.bash"
  local res_entry res_bundled res_entry_status res_bundled_status
  res_entry="$(cd "${REPO_DIR}/test/res/test_suite/${name}" || exit 1; bash "entry.bash")"
  res_entry_status=$?
  res_bundled="$(cd "${REPO_DIR}/test/res/test_suite/${name}" || exit 1; bash "output.bash")"
  res_bundled_status=$?
  [ "${res_entry_status}" -eq "${res_bundled_status}" ]
  [ "${res_entry}" == "${res_bundled}" ]
}

@test "0_nothing - composed_as_expected" {
  test_composed_as_expected "0_nothing"
}

@test "0_nothing - compare_results" {
  test_compare_results "0_nothing"
}

@test "1_import_two_files - composed_as_expected" {
  test_composed_as_expected "1_import_two_files"
}

@test "1_import_two_files - compare_results" {
  test_compare_results "1_import_two_files"
}

@test "2_import_nested_files - composed_as_expected" {
  test_composed_as_expected "2_import_nested_files"
}

@test "2_import_nested_files - compare_results" {
  test_compare_results "2_import_nested_files"
}

@test "3_import_with_variables - composed_as_expected" {
  test_composed_as_expected "3_import_with_variables"
}

@test "3_import_with_variables - compare_results" {
  test_compare_results "3_import_with_variables"
}
