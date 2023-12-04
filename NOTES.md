Nanodash: Notes
===============

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


## Old API usage

- ExploreDataTable: `find_signed_nanopubs_with_uri ? ref=`
- Group: `find_valid_signed_things ? type= searchterm=`
- PublishForm: `getLatestVersionId` (2x)
- SearchPage: `find_valid_signed_nanopubs_with_uri ? ref= pubkey=` / `find_valid_signed_nanopubs_with_text ? text= pubkey=`
- Template: `find_signed_things ? sarchterm=` / `find_signed_nanopubs_with_pattern ? pred= obj= graphpred=`
- TermForwarder: `find_valid_signed_nanopubs_with_pattern ? pred= obj= graphpred=`
- User: `getLatestVersionId` / `find_signed_nanopubs_with_pattern ? pred=approvesOf`
- UserPage: `(getRecent) find_signed_nanopubs ? pubkey=`
