Optional Config
===============
Offering native support for optional properties for [Typesafe Config](https://github.com/lightbend/config).

Overview
========
Typesafe Config is one of the more popular configuration libraries for JVM languages. Unfortunately it doesn't offer an elegant way to handle _optional_ properties.

This project is a companion to Typesafe Config, allowing users a more convenient way to handle optional properties and providing a number of additional enhancements.

**Please refer to the [Wiki](https://github.com/zleonov/optional-config/wiki) for details, API examples, and FAQ.**

Goals
=====
- Create `getOptionalXXXX` and `getOptionalXXXXList` methods analogous to their `getXXXX` and `getXXXXList` counterparts.
- Handle missing properties.
- Handle `null` values (including in lists).
- Raise identical exceptions as Typesafe Config.
- No dependencies (other than Typesafe Config).
- Java 8 compatible.

Documentation
=============
The latest API documentation can be accessed [here](https://zleonov.github.io/optional-config/latest/apidocs).

But if you want something else?
===============================
- [cfg4j](https://github.com/cfg4j/cfg4j) - cfg4j ("configuration for Java") is a configuration library for Java distributed apps (and more).
- [externalized-properties](https://github.com/joel-jeremy/externalized-properties) - A lightweight and extensible library to resolve application properties from various external sources.
- [gestalt](https://github.com/gestalt-config/gestalt) - Gestalt is a powerful Java configuration library designed to simplify the way you handle and manage configurations in your software projects.
- [Owner](https://matteobaccan.github.io/owner) - Get rid of the boilerplate code in properties based configuration.
- [SmallRye Config](https://github.com/smallrye/smallrye-config) - SmallRye Config is a library that provides a way to configure applications, frameworks and containers.
- [Configur8](http://dentondav.id/configur8) - Nano-library which provides the ability to define typesafe (!) Configuration templates for applications.
- [KAConf](https://github.com/mariomac/kaconf) - KickAss Configuration. An annotation-based configuration system for Java and Kotlin.
- [dotenv](https://github.com/shyiko/dotenv) - A twelve-factor configuration (12factor.net/config) library for Java 8+.
- [ini4j](https://ini4j.sourceforge.net) - A simple Java API for handling configuration files in Windows .ini format.
