#!/usr/bin/env bash

set -e

git pull

THIS_VERSION=$(grep -oPm1 "(?<=<version>)[^<]+" pom.xml)
echo "Current version: $THIS_VERSION"

if [[ "$THIS_VERSION" != *-SNAPSHOT ]]; then
  echo "ERROR: Not a snapshot version"
  exit 1
fi

NEW_VERSION=${THIS_VERSION%-SNAPSHOT}
V_MAJOR=${NEW_VERSION%.*}
V_MINOR=${NEW_VERSION#*.}
NEXT_VERSION=$V_MAJOR.$(($V_MINOR+1))-SNAPSHOT

echo "New version to be released: $NEW_VERSION"
echo "Next version: $NEXT_VERSION"
echo

if [ -z $1 ] || [[ "$1" != "-" ]]; then
  echo "Perform release with:"
  echo "./release.sh -"
  exit
fi

if ! git diff-index --quiet HEAD --; then
  echo "ERROR: Uncommitted local changes"
  exit 1
fi

echo "Change version to: $NEW_VERSION"
mvn versions:set versions:commit -DnewVersion="$NEW_VERSION"
git add pom.xml
git commit -m "Version $NEW_VERSION"

echo "Pushing new release..."
git tag nanodash-$NEW_VERSION
git push
git push --tags

echo "Make snapshot version: $NEXT_VERSION"
mvn versions:set versions:commit -DnewVersion="$NEXT_VERSION"
git add pom.xml
git commit -m "Snapshot version $NEXT_VERSION"

git push
