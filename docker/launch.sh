#!/usr/bin/env bash

cd "$(dirname "$0")"
cd ..

set -e

docker-compose pull
docker-compose up -d
