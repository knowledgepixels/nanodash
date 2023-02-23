#!/usr/bin/env bash

set -e

LATESTVERSION=$(git describe --tags `git rev-list --tags --max-count=1`)
git checkout $LATESTVERSION

package/package.sh

docker build -t nanopub/nanodashx .
docker push nanopub/nanodashx

git switch -
