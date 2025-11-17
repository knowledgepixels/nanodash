#!/usr/bin/env bash

cd "$( dirname "${BASH_SOURCE[0]}" )"

./mvnw clean jetty:run
