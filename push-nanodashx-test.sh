#!/usr/bin/env bash

set -e

package/package.sh

docker build -t nanopub/nanodashx-test .
docker push nanopub/nanodashx-test
