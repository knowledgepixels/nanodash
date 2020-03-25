#!/bin/bash

set -e

docker-compose down
git pull
docker-compose pull
docker-compose up -d

echo "Successfully updated"
