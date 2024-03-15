#!/usr/bin/env bats

# detect ROOT_DIR - BEGIN
TEST_DIR="$(cd "$(dirname "${BATS_TEST_FILENAME}")" && pwd -P || exit 1)"
REPO_DIR="$(cd "${TEST_DIR}/../.." && pwd -P || exit 1)"
# detect ROOT_DIR - end

test_composed_as_expected() {
  local name=$1
  (cd "${REPO_DIR}/test/res/test_suite/${name}" || exit 1; "${REPO_DIR}/main/src/pl/rynkowski/shellpack.cljc" pack -i "entry.bash" -o "output.bash")
  local output_value expected_value
  output_value="$(cat "${REPO_DIR}/test/res/test_suite/${name}/output.bash")"
  expected_value="$(cat "${REPO_DIR}/test/res/test_suite/${name}/expected.bash")"
  [ "${output_value}" == "${expected_value}" ]
}

test_compare_results() {
  local name=$1
  "${REPO_DIR}/main/src/pl/rynkowski/shellpack.cljc" pack -i "entry.bash" -o "output.bash"
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

@test "4_import_remote - composed_as_expected" {
  local name="4_import_remote"
  rm -rfv "${REPO_DIR}/test/res/test_suite/${name}/lib"
  "${REPO_DIR}/main/src/pl/rynkowski/shellpack.cljc" fetch -d -i "${REPO_DIR}/test/res/test_suite/${name}/entry.bash"
  # test
  test_composed_as_expected "4_import_remote"
  # cleanup
  rm -rfv "${REPO_DIR}/test/res/test_suite/${name}/lib"
}

@test "4_import_remote - compare_results" {
  local name="4_import_remote"
  rm -rfv "${REPO_DIR}/test/res/test_suite/${name}/lib"
  "${REPO_DIR}/main/src/pl/rynkowski/shellpack.cljc" fetch -i "${REPO_DIR}/test/res/test_suite/${name}/entry.bash"
  # test
  test_compare_results "4_import_remote"
  # cleanup
  rm -rfv "${REPO_DIR}/test/res/test_suite/${name}/lib"
}


@test "5_remote_sourcing_same_repo - composed_as_expected" {
  local name="5_remote_sourcing_same_repo"
  rm -rfv "${REPO_DIR}/test/res/test_suite/${name}/.shellpack_deps"
  "${REPO_DIR}/main/src/pl/rynkowski/shellpack.cljc" fetch -i "${REPO_DIR}/test/res/test_suite/${name}/repo/entry.bash"
  # test
  test_composed_as_expected "5_remote_sourcing_same_repo/repo"
  # cleanup
  rm -rfv "${REPO_DIR}/test/res/test_suite/${name}/.shellpack_deps"
}

@test "5_remote_sourcing_same_repo - compare_results" {
  local name="5_remote_sourcing_same_repo"
  rm -rfv "${REPO_DIR}/test/res/test_suite/${name}/.shellpack_deps"
  "${REPO_DIR}/main/src/pl/rynkowski/shellpack.cljc" fetch -i "${REPO_DIR}/test/res/test_suite/${name}/repo/entry.bash"
  # test
  test_compare_results "5_remote_sourcing_same_repo/repo"
  # cleanup
  rm -rfv "${REPO_DIR}/test/res/test_suite/${name}/.shellpack_deps"
}

@test "6_duplicated_import - composed_as_expected" {
  test_composed_as_expected "6_duplicated_import"
}

@test "6_duplicated_import - compare_results" {
  test_compare_results "6_duplicated_import"
}
