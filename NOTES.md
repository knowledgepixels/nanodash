Nanodash: Notes
===============

## Stylesheet update

Copy stylesheet into running container:

    $ docker compose cp src/main/webapp/style.css nanodash:/usr/local/tomcat/webapps/ROOT/


## Docker

Make Docker container:

    $ package/package.sh
    $ docker build -t nanopub/nanodash .

Publish Docker container:

    $ docker push nanopub/nanodash


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

    $ mvn versions:use-latest-versions && mvn versions:update-properties
