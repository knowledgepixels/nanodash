#!/usr/bin/env bash

set -e

cd -- "$(dirname "$0")"
cd ..
VERSION=$(cat pom.xml | grep "<version>" | head -1 | sed -r 's/[^.0-9]//g')
mvn clean install org.apache.tomcat.maven:tomcat7-maven-plugin:2.1:exec-war-only
cp target/nanodash-$VERSION*.jar package/nanodash.jar
cd package
rm -f nanodash-$VERSION.zip
zip -r nanodash-$VERSION.zip nanodash.jar run run-under-windows.bat update update-under-windows.bat
