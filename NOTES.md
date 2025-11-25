Nanodash: Notes
===============

## Docker

Make Docker container:

    $ package/package.sh
    $ docker build -t nanopub/nanodash .

Publish Docker container:

    $ docker push nanopub/nanodash


## Maven

If Maven has problems with recent Java:

    $ export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64/


## Dependencies Report

Run:

    $ mvn project-info-reports:dependencies

Report is then generated here: target/site/dependencies.html


## Update Dependencies

    $ mvn versions:use-latest-versions && mvn versions:update-properties


## Backup archive of user data

Make password-protected backup file of user data (private keys):

    $ tar -czv local-data/nanodash-users/ | openssl enc -aes-256-cbc -e > nanodash-users.tar.gz.enc

Decrypt and extract:

    $ openssl aes-256-cbc -d -in nanodash-users.tar.gz.enc | tar -xzv
