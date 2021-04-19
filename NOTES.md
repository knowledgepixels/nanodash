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
