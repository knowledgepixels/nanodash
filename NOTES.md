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


## Pointing nanodash at particular services

Two pairs of environment variables are involved, and both pairs are needed to move a deployment
as a whole -- for example onto a private registry and query service:

    $ NANOPUB_QUERY_INSTANCES=https://query.example.org/ \
      NANOPUB_REGISTRY_INSTANCES=https://registry.example.org/ \
      NANODASH_MAIN_QUERY=https://query.example.org/ \
      NANODASH_MAIN_REGISTRY=https://registry.example.org/ \
      ./run-dev.sh

`NANOPUB_QUERY_INSTANCES` and `NANOPUB_REGISTRY_INSTANCES` (whitespace-separated lists, read by
the `nanopub` library) decide where queries are actually dispatched and where nanopubs are
fetched from and published to. Without them the library keeps using the public instances it
discovers, whatever nanodash is configured with.

`NANODASH_MAIN_QUERY` and `NANODASH_MAIN_REGISTRY` pin the single service nanodash itself names:
outgoing links to the query and registry UIs, the account list, and the health and restricted-mode
probes. An explicitly set value always wins; if it is not in the library's instance list, that is
logged as a warning -- it usually means the library pair above is missing -- but the configured
URL is still used.

To make a local run use only one query instance, set just the query pair.


## Backup archive of user data

Make password-protected backup file of user data (private keys):

    $ tar -czv local-data/nanodash-users/ | openssl enc -aes-256-cbc -e > nanodash-users.tar.gz.enc

Decrypt and extract:

    $ openssl aes-256-cbc -d -in nanodash-users.tar.gz.enc | tar -xzv
