#!/usr/bin/env bash

set -e

package/package.sh
docker build -t nanopub/nanobenchx .
docker push nanopub/nanobenchx
