# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

More infos about this file : http://keepachangelog.com/

## [Unreleased] - no_due_date

- **Use `sbt-tpolecat` plugin instead of our manually defined scalac options**
- **Change used scalafmt plugin**
- **Update Scala 2.12 version**

## [v0.5.3] - 2018.04.26

- **Update Scala 2.12 version**
- **Update dependencies**

## [v0.5.2] - 2018.02.14

- **Improve how `GoogleGeocoder.geocodeNonAmbigueAddressT` retrieves geocoding**

**WARNING**: Read the method documentation before using it !

- **Add tests for `GoogleGeocoder.geocodeNonAmbigueAddressT`**

## [v0.5.1] - 2018.02.12

- **[HOTFIX] The new Distance API implementation was bugged**
- **Add some tests for the Distance API**

## [v0.5.0] - 2018.02.12

- **Use the `global` monix Scheduler instead of our own badly crafted one**
- **Update libs**
- **Add the possibility to geocode non ambigue addresses**
- **Handle travel modes**

## [v0.4.0] - 2018.01.19

- **Distance ordering should not be hard coded**

## [v0.3.1] - 2018.01.19

- **Small optimisation: Do not call the geocoder if origin == destination**

## [v0.3.0] - 2018.01.18

- **Simplify types thanks to type aliases**
- **Small optimization: Small optimization: if origin == destination, immediately return a distance to 0**
- **Small optimisation: Call the geocoder only once if origin == destination**

## [v0.2.3] - 2018.01.18

- **FIX: Distances should be compared by their durations and not by their lenghts**
- **Improve `scalacOptions` config**
- **Update dependencies**

## [v0.2.2] - 2017.12.12

- **Add the possibility to tune the database used in Redis without any password**

## [v0.2.1] - 2017.12.12

- **Add the possibility to tune the database used in Redis**

## [v0.2.0] - 2017.12.12

- **Simplify the use of the library**
- **Add the possibility to use a Redis protected by password**
- **Add the possibility to tune the connection and the read timeouts of the Google Geo API client**

## [v0.1.3] - 2017.12.12

- **Try fix build**

## [v0.1.2] - 2017.12.12

- **Try fix build by adding scmInfo**

## [v0.1.1] - 2017.12.12

- **Try fix build**

## [v0.1.0] - 2017.12.03

- **Configure Codecov**
- **Configure `sbt-release-early`, Travis and Bintray intergration** 
- **Open source the library**