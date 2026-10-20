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


## The test suite and the public services

The suite still reads the live nanopub network for the templates and nanopubs individual tests
ask for, but it no longer loads anything of its own accord. Two things keep it in bounds, both
set up in the surefire configuration in `pom.xml` (issue #809):

`NANODASH_DATA_REFRESH=false` stops an instance loading and periodically refreshing the shared
data sets -- the users, templates, spaces, maintained resources and resource profiles. Those are
whole-store queries, and some 450 applications are started over a run; the periodic refresh
forces its fetches past the cache, so with several test JVMs and several CI runners at once it
was enough to put the public query services at 1,100-1,800% CPU. The variable works on a real
deployment too, where it means: build each data set when something first asks for it, and leave
it at that.

The `live` JUnit tag marks the tests that read the live corpus as a whole, which for now means
`AllAssertionTemplatesLoadingTest` and its 195 template fetches. An ordinary `mvn test` leaves
them out; the `live-tests` workflow runs them once a day. To run them by hand:

    $ ./mvnw test -Dgroups=live -Dtest.excludedGroups= -Dtest.forkCount=1

`-Dtest.forkCount=1` also serialises an ordinary run, which is worth knowing if a test turns out
to be sensitive to sharing its JVM.

What would take the suite off the public services altogether is pointing it at a service of its
own, with the two pairs of variables above. Some 380 tests fetch a nanopub or expect a query to
answer, so that needs those nanopubs and query results available there first.


## Backup archive of user data

Make password-protected backup file of user data (private keys):

    $ tar -czv local-data/nanodash-users/ | openssl enc -aes-256-cbc -e > nanodash-users.tar.gz.enc

Decrypt and extract:

    $ openssl aes-256-cbc -d -in nanodash-users.tar.gz.enc | tar -xzv
