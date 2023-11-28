Optional Config
===============
Offering native support for optional properties for [Typesafe Config](https://github.com/lightbend/config).

Overview
========
Typesafe Config is one of the more popular configuration libraries for JVM languages. The authors of Typesafe Config take an opinionated view on the handling of [default properties](https://github.com/lightbend/config#how-to-handle-defaults):

 >  _Many other configuration APIs allow you to provide a default to the getter methods, like this:
 >      
 >      boolean getBoolean(String path, boolean fallback)
 >  
 >  Here, if the path has no setting, the fallback would be returned. An API could also return null for unset values, so you would check for null:
 > 
 >      // returns null on unset, check for null and fall back
 >      Boolean getBoolean(String path)
 > 
 >  The methods on the Config interface do NOT do this, for two major reasons:
 > 
 >      1. If you use a config setting in two places, the default fallback value gets cut-and-pasted and typically out of sync.
 >         This can result in Very Evil Bugs.
 >      2. If the getter returns null (or None, in Scala) then every time you get a setting you have to write handling code
 >         for null/None and that code will almost always just throw an exception. Perhaps more commonly, people
 >         forget to check for null at all, so missing settings result in NullPointerException._

While this is generally applicable to _default_ values, it does not address scenarios where properties have no defaults and are inherently _optional_. For instance, consider a directory copy utility capable of filtering on file extensions. The utility would be expected to transfer all files indiscriminately if no file extensions are specified. Without native capability to handle optional properties, developers would have to resort to writing cumbersome boilerplate code or use special placeholder value to indicate _no filter_.

See issues [186](https://github.com/lightbend/config/issues/282), [282](https://github.com/lightbend/config/issues/282), [286](https://github.com/lightbend/config/issues/286), [110](https://github.com/lightbend/config/issues/110), [440](https://github.com/lightbend/config/issues/440) for historical discussion.

This project is a companion to Typesafe Config, allowing users a more convenient way to handle _optional_ properties and provides a number of additional enhancements.

