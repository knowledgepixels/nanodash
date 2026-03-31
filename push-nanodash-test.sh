#!/usr/bin/env bash

cd "$( dirname "${BASH_SOURCE[0]}" )"

set -e

./mvnw clean package -Dmaven.test.skip=true
docker build -t nanopub/nanodash:test .
docker push nanopub/nanodash:test
