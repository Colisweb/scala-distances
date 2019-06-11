# Release process

## Setup

* Create an account on Bintray and link it to the organization `colisweb`
* Retrieve your Bintray API key
* Install `sbt-bintray` sbt plugin
* Launch `sbt bintrayChangeCredentials`
* Follow the instructions (they may repeat)

## Release

* Update `version.sbt`
* Launch `sbt release cross`
