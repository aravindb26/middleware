---
title: Injecting variables into Open-Xchange Middleware configuration
icon: fas fa-globe
tags: Configuration
---

# Introduction

With version v8.0.0 the Open-Xchange Middleware allows injecting variables into the configuration of the Middleware.

# Injecting through tokens

It is possible to specify tokens or placeholders directly in .properties, .yml and .yaml configuration files nested in the `/opt/open-xchange/etc` directory (and sub-directories respectively); except files:

* `/opt/open-xchange/etc/external-domains.properties`
* `/opt/open-xchange/etc/HTMLEntities.properties`

A placeholder begins with the character sequence {% raw %}`"{{"`{% endraw %} and it ends with character sequence {% raw %}`"}}"`{% endraw %}.

The notation accepts an optional source identifier, the actual variable name (required) and an optional default value.

The complete syntax is:

<!-- {% raw %} -->
```
"{{" + <source-id> + " " + <variable-name> + ":" + <default-value> + "}}"
```
<!-- {% endraw %} -->

The source identifier tells from what source to look-up the given variable name and the default value defines the value to use in case no such variable is available in looked-up source.

## Supported source identifiers

* `"env"` determines that system environment is supposed to be look-ed up for a certain variable name. Furthermore, it is assumed as default in case no source identifier is present.
* `"file"` sets that a certain .properties file is supposed to be look-ed up. Moreover, this identifier expects specification of the actual .properties file in surrounding arrow brackets; e.g. `"file<tokenvalues.properties>"`. That .properties file is supposed to reside in the `/opt/open-xchange/etc` directory or any of its sub-directories.
* Any other source identifier refers to a programmatically registered instance of `com.openexchange.config.VariablesProvider` in case a custom source needs to be used.

## Examples

Given that `"PROP1=value1"` has been set as environment variable and there is a file `/opt/open-xchange/etc/tokenvalues.properties` with content:

```
# Content of tokenvalues.properties
com.openexchange.test.value1=mytokenvalue
```

Specifying a .properties file with the following content:

<!-- {% raw %} -->
```
# "env" used as default source, no default value
com.openexchange.test.token1={{PROP1}}

# No default value
com.openexchange.test.token2={{env PROP1}}

# "env" used as default source, with "defaultforprop2" as default value
com.openexchange.test.token3={{PROP2:defaultforprop2}}

# Fully qualifying notation with source, variable name and default value
com.openexchange.test.token4={{env PROP3:defaultforprop3}}

# Look-up property "com.openexchange.test.value1" in file "tokenvalues.properties"
com.openexchange.test.token5={{file<tokenvalues.properties> com.openexchange.test.value1}}

# Look-up property "com.openexchange.test.value2" in file "tokenvalues.properties", use "foobar" as default value
com.openexchange.test.token6={{file<tokenvalues.properties> com.openexchange.test.value2:foobar}}
```
<!-- {% endraw %} -->

The result used by Middleware would be:

```
com.openexchange.test.token1=value1
--> Use value "value1" from environment variable "PROP1"

com.openexchange.test.token2=value1
--> Use value "value1" from environment variable "PROP1"

com.openexchange.test.token3=defaultforprop2
--> No such environment variable "PROP2", therefore use default value

com.openexchange.test.token4=defaultforprop3
--> No such environment variable "PROP3", therefore use default value

com.openexchange.test.token5=mytokenvalue
 --> Read "mytokenvalue" as value for "com.openexchange.test.value1" from file "tokenvalues.properties"

com.openexchange.test.token6=foobar
--> Since no such property "com.openexchange.test.value2" available in file "tokenvalues.properties", the default value is used
```
