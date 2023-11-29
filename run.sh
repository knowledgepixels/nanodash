#!/usr/bin/env bash

cd "$( dirname "${BASH_SOURCE[0]}" )"

docker compose down
mvn package
docker compose up
