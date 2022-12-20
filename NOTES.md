Nanobench: Notes
================

## Docker

Make Docker container:

    $ package/package.sh
    $ docker build -t nanopub/nanobench .

Publish Docker container:

    $ docker push nanopub/nanobench


## Release

Prepare release:

    $ ./release.sh

Perform release

    $ ./release.sh -


## Maven

If Maven has problems with recent Java:

    $ export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64/


## Dependencies Report

Run:

    $ mvn project-info-reports:dependencies

Report is then generated here: target/site/dependencies.html


## Update Dependencies

    $ mvn versions:use-latest-versions
    $ mvn versions:update-properties
