#!/usr/bin/env bash

# This is a sample library that entry.bash refers to.
# To make it easier to maintain, I put it here, as you can see in entry.bash it is not sourced directly.
# It is sourced first with @github token and then with @https token.

printf "%s\n" "Log from library"
