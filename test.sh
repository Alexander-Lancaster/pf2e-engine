#!/bin/sh
# Compatibility entry point; Maven is the single build/test implementation.
set -eu
cd "$(dirname "$0")"
exec ./mvnw test "$@"
