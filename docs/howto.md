# HOW TO

## BATS

shellpack for resolution of file paths, launches the interpreter.
Unfortunately, bats can't be used for it, so instead bash is launched.
This leads two considerations.

### Test definitions

Bats support two styles of test definitions:

1. `@test` function

```bats
@test "very important test" {
  local actual="narrow path"
  [ "${actual}" = "narrow path" ]
}
```

2. function with @test comment

```bats
very_important_test() { # @test
  local actual="narrow path"
  [ "${actual}" = "narrow path" ]
}
```

You would be fine with both styles in cases you source only bash files,
but if you source bats files, you will be better off using the second style.
Since Bash is used for resolution of these files, the first style will not work,
because bash is not familiar with @test syntax.

### Paths resolution in bats files

When fetching or packing bats files, you can't rely on `BATS_TEST_FILENAME` for absolute paths resolution.
When running Bash, this variable is not set. Therefore, other than relying on `${BATS_TEST_FILENAME}`
you should use `"${BATS_TEST_FILENAME:-"${BASH_SOURCE[0]:-$0}"}"`, or something similar.

Example:

```bats
#!/usr/bin/env bats

# Path Initialization
_TEST_DIR="$(cd "$(dirname "${BATS_TEST_FILENAME:-"${BASH_SOURCE[0]:-$0}"}")" && pwd -P || exit 1)"
_ROOT_DIR="$(cd "${_TEST_DIR}/.." && pwd -P || exit 1)"
# Library Sourcing
source "${_ROOT_DIR}/.https/raw.githubusercontent.com/ztombol/bats-assert/v0.3.0/src/assert.bash/color.bash"
```
