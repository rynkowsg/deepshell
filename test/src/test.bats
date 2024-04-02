#!/usr/bin/env bats

# detect ROOT_DIR - BEGIN
TEST_DIR="$(cd "$(dirname "${BATS_TEST_FILENAME}")" && pwd -P || exit 1)"
REPO_DIR="$(cd "${TEST_DIR}/../.." && pwd -P || exit 1)"
# detect ROOT_DIR - end

test_composed_as_expected_relative() {
  local name=$1
  local ext="${2:-"bash"}"
  local input_path="entry.${ext}"
  local output_path="output.${ext}"
  local expected_path="expected.${ext}"
  (
    cd "${REPO_DIR}/test/res/test_suite/${name}" || exit 1
    "${REPO_DIR}/main/src/pl/rynkowski/sosh.cljc" pack -i "${input_path}" -o "${output_path}"
  )
  local output_value expected_value
  output_value="$(cat "${REPO_DIR}/test/res/test_suite/${name}/${output_path}")"
  expected_value="$(cat "${REPO_DIR}/test/res/test_suite/${name}/${expected_path}")"
  [ "${output_value}" == "${expected_value}" ]
}

test_composed_as_expected_absolute() {
  local name=$1
  local ext="${2:-"bash"}"
  local input_path="${REPO_DIR}/test/res/test_suite/${name}/entry.${ext}"
  local output_path="${REPO_DIR}/test/res/test_suite/${name}/output.${ext}"
  local expected_path="${REPO_DIR}/test/res/test_suite/${name}/expected.${ext}"
  "${REPO_DIR}/main/src/pl/rynkowski/sosh.cljc" pack -i "${input_path}" -o "${output_path}"
  local output_value expected_value
  output_value="$(cat "${output_path}")"
  expected_value="$(cat "${expected_path}")"
  [ "${output_value}" == "${expected_value}" ]
}

test_compare_results_relative() {
  local name=$1
  (
    cd "${REPO_DIR}/test/res/test_suite/${name}" || exit 1
    "${REPO_DIR}/main/src/pl/rynkowski/sosh.cljc" pack -i "entry.bash" -o "output.bash"
  )
  local res_entry res_bundled res_entry_status res_bundled_status
  res_entry="$(
    cd "${REPO_DIR}/test/res/test_suite/${name}" || exit 1
    bash "entry.bash"
  )"
  res_entry_status=$?
  res_bundled="$(
    cd "${REPO_DIR}/test/res/test_suite/${name}" || exit 1
    bash "output.bash"
  )"
  res_bundled_status=$?
  [ "${res_entry_status}" -eq "${res_bundled_status}" ]
  [ "${res_entry}" == "${res_bundled}" ]
}

test_compare_results_absolute() {
  local name=$1
  "${REPO_DIR}/main/src/pl/rynkowski/sosh.cljc" pack -i "${REPO_DIR}/test/res/test_suite/${name}/entry.bash" -o "${REPO_DIR}/test/res/test_suite/${name}/output.bash"
  local res_entry res_bundled res_entry_status res_bundled_status
  res_entry="$(
    cd "${REPO_DIR}/test/res/test_suite/${name}" || exit 1
    bash "entry.bash"
  )"
  res_entry_status=$?
  res_bundled="$(
    cd "${REPO_DIR}/test/res/test_suite/${name}" || exit 1
    bash "output.bash"
  )"
  res_bundled_status=$?
  [ "${res_entry_status}" -eq "${res_bundled_status}" ]
  [ "${res_entry}" == "${res_bundled}" ]
}

@test "0_nothing - composed_as_expected" {
  test_composed_as_expected_relative "0_nothing"
}

@test "0_nothing - compare_results" {
  test_compare_results_relative "0_nothing"
}

@test "1_import_two_files - composed_as_expected (relative)" {
  test_composed_as_expected_relative "1_import_two_files"
}

@test "1_import_two_files - compare_results (relative)" {
  test_compare_results_relative "1_import_two_files"
}

@test "2_import_nested_files - composed_as_expected (relative)" {
  test_composed_as_expected_relative "2_import_nested_files"
}

@test "2_import_nested_files - compare_results (relative)" {
  test_compare_results_relative "2_import_nested_files"
}

@test "3_import_with_variables - composed_as_expected" {
  test_composed_as_expected_relative "3_import_with_variables"
  test_composed_as_expected_absolute "3_import_with_variables"
}

@test "3_import_with_variables - compare_results" {
  test_compare_results_relative "3_import_with_variables"
  test_compare_results_absolute "3_import_with_variables"
}

@test "4_import_remote - composed_as_expected" {
  local name="4_import_remote"
  rm -rfv "${REPO_DIR}/test/res/test_suite/${name}/lib"
  "${REPO_DIR}/main/src/pl/rynkowski/sosh.cljc" fetch -i "${REPO_DIR}/test/res/test_suite/${name}/entry.bash"
  # test
  test_composed_as_expected_relative "4_import_remote"
  test_composed_as_expected_absolute "4_import_remote"
  # cleanup
  rm -rfv "${REPO_DIR}/test/res/test_suite/${name}/lib"
}

@test "4_import_remote - compare_results" {
  local name="4_import_remote"
  rm -rfv "${REPO_DIR}/test/res/test_suite/${name}/lib"
  "${REPO_DIR}/main/src/pl/rynkowski/sosh.cljc" fetch -i "${REPO_DIR}/test/res/test_suite/${name}/entry.bash"
  # test
  test_compare_results_relative "4_import_remote"
  test_compare_results_absolute "4_import_remote"
  # cleanup
  rm -rfv "${REPO_DIR}/test/res/test_suite/${name}/lib"
}

@test "5_remote_sourcing_same_repo - composed_as_expected" {
  local name="5_remote_sourcing_same_repo"
  rm -rfv "${REPO_DIR}/test/res/test_suite/${name}/.sosh_deps"
  "${REPO_DIR}/main/src/pl/rynkowski/sosh.cljc" fetch -i "${REPO_DIR}/test/res/test_suite/${name}/repo/entry.bash"
  # test
  test_composed_as_expected_relative "5_remote_sourcing_same_repo/repo"
  test_composed_as_expected_absolute "5_remote_sourcing_same_repo/repo"
  # cleanup
  rm -rfv "${REPO_DIR}/test/res/test_suite/${name}/.sosh_deps"
}

@test "5_remote_sourcing_same_repo - compare_results" {
  local name="5_remote_sourcing_same_repo"
  rm -rfv "${REPO_DIR}/test/res/test_suite/${name}/.sosh_deps"
  "${REPO_DIR}/main/src/pl/rynkowski/sosh.cljc" fetch -i "${REPO_DIR}/test/res/test_suite/${name}/repo/entry.bash"
  # test
  test_compare_results_relative "5_remote_sourcing_same_repo/repo"
  test_compare_results_absolute "5_remote_sourcing_same_repo/repo"
  # cleanup
  rm -rfv "${REPO_DIR}/test/res/test_suite/${name}/.sosh_deps"
}

@test "6_duplicated_import - composed_as_expected" {
  test_composed_as_expected_relative "6_duplicated_import"
  test_composed_as_expected_absolute "6_duplicated_import"
}

@test "6_duplicated_import - compare_results" {
  test_compare_results_relative "6_duplicated_import"
  test_compare_results_absolute "6_duplicated_import"
}

@test "7_bats_import - composed_as_expected" {
  test_composed_as_expected_relative "7_bats_import" "bats"
  test_composed_as_expected_absolute "7_bats_import" "bats"
}
