#!/usr/bin/env bash

set -e

cd -- "$(dirname "$0")"
cd ..
VERSION=$(cat pom.xml | grep "<version>" | head -1 | sed -r 's/[^.0-9]//g')
mvn -o clean install tomcat7:exec-war-only
cp target/nanobench-$VERSION*.jar package/nanobench.jar
cd package
rm -f nanobench-$VERSION.zip
zip -r nanobench-$VERSION.zip nanobench.jar run run-under-windows.bat update update-under-windows.bat
