#!/usr/bin/env bash

cd "$(dirname "$0")"

mvn clean tomcat7:run
