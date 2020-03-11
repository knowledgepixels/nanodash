#!/bin/bash

set -e

cd -- "$(dirname "$0")"
cd ..
VERSION=$(cat pom.xml | grep "<version>" | head -1 | sed -r 's/[^.0-9]//g')
mvn clean install tomcat7:exec-war-only
cp target/nanobench-$VERSION*.jar package/nanobench.jar
cd package
rm nanobench-$VERSION.zip
zip -r nanobench-$VERSION.zip nanobench.jar run run.bat
rm nanobench.jar