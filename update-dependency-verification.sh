#!/bin/bash
set -e

# do not clear metadata file because of https://github.com/gradle/gradle/issues/19228

# calculating new checksums for all tasks usually need (add if missing)
./gradlew --write-verification-metadata sha256 check lint connectedCheck
