#!/usr/bin/env bash

set -e

mvn clean package

docker build -t nanopub/nanodash-test .
docker push nanopub/nanodash-test
