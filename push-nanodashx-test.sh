#!/usr/bin/env bash

set -e

mvn clean package

docker build -t nanopub/nanodashx-test .
docker push nanopub/nanodashx-test
