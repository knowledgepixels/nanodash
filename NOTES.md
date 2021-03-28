Nanobench: Notes
================

## Docker

Make Docker container:

    $ package/package.sh
    $ docker build -t nanopub/nanobench .

Get current version:

    $ grep -oPm1 "(?<=<version>)[^<]+" pom.xml

Set new version:

    $ mvn versions:set versions:commit -DnewVersion="1.22-SNAPSHOT"
