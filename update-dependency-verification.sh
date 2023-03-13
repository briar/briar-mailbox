#!/bin/bash
set -e

# As a workaround for https://github.com/gradle/gradle/issues/19228, use
# a temporary directory as GRADLE_USER_HOME so that it does not matter
# what is currently in the Gradle cache.
TMPDIR=$(mktemp -d)
echo "using $TMPDIR as GRADLE_USER_HOME"
trap 'rm -rf -- "$TMPDIR"' EXIT

export GRADLE_USER_HOME="$TMPDIR"

# add some initial trusted artifacts manually
cat <<EOT > gradle/verification-metadata.xml
<?xml version="1.0" encoding="UTF-8"?>
<verification-metadata xmlns="https://schema.gradle.org/dependency-verification" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="https://schema.gradle.org/dependency-verification https://schema.gradle.org/dependency-verification/dependency-verification-1.1.xsd">
   <configuration>
      <verify-metadata>true</verify-metadata>
      <verify-signatures>false</verify-signatures>
      <trusted-artifacts>
         <trust group="com.android.tools.build" name="aapt2" file="aapt2-[0-9\.\-]+-(osx|windows).jar$" regex="true"/>
         <trust file=".*-javadoc[.]jar$" regex="true"/>
         <trust file=".*-sources[.]jar$" regex="true"/>
      </trusted-artifacts>
   </configuration>
</verification-metadata>
EOT

# calculating new checksums for all tasks usually needed (add if missing)
./gradlew --no-daemon --write-verification-metadata sha256 check lint connectedCheck
