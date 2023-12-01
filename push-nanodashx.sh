#!/usr/bin/env bash

set -e

LATESTVERSION=$(git describe --tags `git rev-list --tags --max-count=1`)
git checkout $LATESTVERSION

mvn clean package

docker build -t nanopub/nanodashx .
docker push nanopub/nanodashx

git switch -
