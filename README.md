Optional Config
===============
Offering native support for optional properties for [Typesafe Config](https://github.com/lightbend/config).

Overview
========
Typesafe Config is one of the more popular configuration libraries for JVM languages. Unfortunately it doesn't offer an elegant way to handle _optional_ properties.

This project is a companion to Typesafe Config, allowing users a more convenient way to handle _optional_ properties and provides a number of additional enhancements.

Please refer to the [Wiki([https://github.com/zleonov/optional-config/wiki]) for details, examples, and FAQs.

Goals
=====
- Create `getOptionalXXXX` and `getOptionalXXXXList` methods analogous to their `getXXXX` and `getXXXXList` counterparts
- Handle missing properties
- Handle `null` values (including in lists)
- Raise identical exceptions as Typesafe Config
- No dependencies (other than Typesafe Config)
- Java 8 compatible
