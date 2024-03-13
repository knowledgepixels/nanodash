#!/usr/bin/env bash

set -e

THIS_VERSION=$(grep -oPm1 "(?<=<version>)[^<]+" pom.xml)
echo "Current version: $THIS_VERSION"

if [[ "$THIS_VERSION" != *-SNAPSHOT ]]; then
  echo "ERROR: Not a snapshot version"
  exit 1
fi

NEW_VERSION=${THIS_VERSION%-SNAPSHOT}

echo "Setting href=\"style.css?v=$NEW_VERSION\""

find src -type f -name '*.html' -exec \
  sed -i -r "s/href=\"style\.css\?v=[^\"]+\"/href=\"style\.css\?v=$NEW_VERSION\"/g" {} \;
