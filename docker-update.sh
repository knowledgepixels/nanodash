#!/usr/bin/env bash

set -e

docker-compose down
git pull
./docker-launch.sh
