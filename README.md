# shellpack

[![CircleCI Build Status][ci-build-badge]][ci-build]
[![License][license-badge]][license]

Bundle your shell scripts into one file.

Status: pre-ALPHA

## Motivation

So, it all started when I was trying to share code between CircleCI orbs and their commands.
The thing is, orbs only let you use one bash script per command.
That's a bummer because if your commands share similarities or worse, they are kind of the same just with different defaults, you end up copying and pasting the same stuff over and over.

After a couple of days, it hit me that it'd be super handy to share not just bits
and pieces between orb commands, but also with all the other bash scripts I had lying around.
I'm talking about the stuff we use all the time, like logging, handling errors, and other goodies.

## Install

```sh
curl -s https://raw.githubusercontent.com/rynkowsg/shellpack/main/src/pl/rynkowski/shellpack.bb -O ~/.bin/shellpack
```
The line above installs the script in `~/.bin`. That installation directory needs to be added to `PATH`.

> [!WARNING]
> The tool requires [Babashka](https://github.com/babashka/babashka) to work. If you like to see this a standalone binary rise your voice [HERE](https://github.com/rynkowsg/shellpack/issues/1).

## Usage

```sh
shellpack --entry script.bash --output gen/script.bash
```

## License

Copyright © 2024 Greg Rynkowski

Released under the [MIT license][license].

[ci-build-badge]: https://circleci.com/gh/rynkowsg/shellpack.svg?style=shield "CircleCI Build Status"
[ci-build]: https://circleci.com/gh/rynkowsg/asdf-orb
[license-badge]: https://img.shields.io/badge/license-MIT-lightgrey.svg
[license]: https://raw.githubusercontent.com/rynkowsg/shellpack/main/LICENSE
