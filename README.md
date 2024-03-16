# shellpack

[![CircleCI Build Status][ci-build-badge]][ci-build]
[![License][license-badge]][license]

Two pillars:
- `source` bash files from remote locations like GitHub, and
- bundle your shell scripts into one file.

Status: **ALPHA**

## Motivation

So, it all started when I was trying to share code between CircleCI orbs and their commands.
The thing is, orbs only let you use one bash script per command.
That's a bummer because if your commands share similarities or worse, they are kind of the same just with different defaults, you end up copying and pasting the same stuff over and over.

After a couple of days, it hit me that it'd be super handy to share not just bits
and pieces between orb commands, but also with all the other bash scripts I had lying around.
I'm talking about the stuff we use all the time, like logging, handling errors, and other goodies.

## Install

```sh
curl -s https://raw.githubusercontent.com/rynkowsg/shellpack/v0.1.1/main/src/pl/rynkowski/shellpack.cljc -o ~/.bin/shellpack
chmod +x ~/.bin/shellpack
```
The line above installs the script in `~/.bin`. That installation directory needs to be added to `PATH`.

> [!WARNING]
> The tool requires [Babashka](https://github.com/babashka/babashka) to work. If you like to see this a standalone binary rise your voice [HERE](https://github.com/rynkowsg/shellpack/issues/1).

## Usage

**FETCH**

```sh
shellpack fetch ./test/res/test_suite/4_import_remote/entry.bash
```

**PACK**

```sh
# pack the script on input to the path on output
shellpack pack -i ./test/res/test_suite/3_import_with_variables/entry.bash -o ./bundled.bash

# optionally you can set current working directory (useful if the entry script doesn't use absolute path for sourced files)
shellpack pack -i ./entry.bash -o ./bundled.bash
```

## Examples

I use this tool in the following repositories:
- https://github.com/rynkowsg/asdf-orb - CircleCI orb providing asdf ([script example](https://github.com/rynkowsg/asdf-orb/blob/main/src/scripts/install_asdf.bash))
- https://github.com/rynkowsg/shell-gr - my library of bash snippets ([lib examples](https://github.com/rynkowsg/shell-gr/blob/main/lib/))

## License

Copyright © 2024 Greg Rynkowski

Released under the [MIT license][license].

[ci-build-badge]: https://circleci.com/gh/rynkowsg/shellpack.svg?style=shield "CircleCI Build Status"
[ci-build]: https://circleci.com/gh/rynkowsg/shellpack
[license-badge]: https://img.shields.io/badge/license-MIT-lightgrey.svg
[license]: https://raw.githubusercontent.com/rynkowsg/shellpack/main/LICENSE
