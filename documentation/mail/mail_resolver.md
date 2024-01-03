---
title: Mail Resolver
icon: fa fa-share-alt
tags: Mail, LDAP, Mail Resolver
---

# Introduction

For certain scenarios, the middleware needs to be able to perform an installation-wide lookup of a user by its email address, which means to resolve the address string to the context this user resides in. As the groupware databases are typically sharded (and contexts are distributed across multiple database schemas), the lookup needs to be performed on a higher level. 

Example use cases include [application-specific passwords](https://documentation.open-xchange.com/{{ site.baseurl }}/middleware/login_and_sessions/application_passwords.html#login-and-context-lookup) or [cross-context free/busy and conflict checks](https://documentation.open-xchange.com/{{ site.baseurl }}/middleware/calendar/implementation_details.html#cross-context) in the calendar. The functionality is also exposed as REST endpoint under /preliminary/utilities/mailResolver/v1/resolve/{mail*}. See [REST API Documentation ](https://documentation.open-xchange.com/components/middleware/rest{{ site.baseurl }}/index.html#!Preliminary/resolveMailAddress) for more details.

Besides a default / fallback implementation that makes use of the configuration database, also a generic LDAP-based resolver is available. There may also be use cases where a custom mail resolver plugin, dedicated for the environment, is used.

# Default Mail Resolver

The default Mail Resolver is able to resolve mail addresses by searching the database. It is always enabled and is able to perform two different kinds of look-ups.

The default mechanism tries to find mail addresses on a per database schema basis. For each known schema a query is performed to check whether the mail address can be resolved to an internal user or not. 

This behavior can be changed by setting ``com.openexchange.mailmapping.lookUpByDomain=true``, which will result in mail address look-ups by their domain part (the part after the ``@`` sign). It is used to find a matching context by checking the login mappings. That mechanism does only work if Open-Xchange setup strictly defines a dedicated and unique domain per context. Otherwise, that look-up mechanism will lead to wrong results.


# LDAP-based Mail Resolver

The LDAP-based Mail Resolver is able to resolve mail addresses by consulting a directory service.

The resolver can be enabled with ``com.openexchange.mailmapping.ldap.enabled=true`` and requires a proper client configuration to be set via ``com.openexchange.mailmapping.ldap.clientId``. Therefore, a section within ``ldap-client-config.yml`` needs to be defined first. See [LDAP Client Configuration]({{ site.baseurl }}/middleware/administration/ldap_client_configuration.html) for further details. 

Searching the directory service can be configured with the following properties

- `com.openexchange.mailmapping.ldap.searchFilter` Specifies the LDAP search filter to find user entries by mail address. Placeholder <code>[value]</code> will be replaced by the given mail address. Default is `(mail=[value])`
- `com.openexchange.mailmapping.ldap.searchScope` Specifies the LDAP search scope. Default is `SUB`
- `com.openexchange.mailmapping.ldap.userIdAttribute` Specifies the attribute which is used to find the userId in the LDAP search result. The LDAP search result need to contain a value for this attribute or for `userNameAttribute` in order to successfully resolve a mail address. Default is `oxUserId`
- `com.openexchange.mailmapping.ldap.userNameAttribute` Specifies the attribute which is used to find the user name in the LDAP search result. The LDAP search result need to contain a value for this attribute or for `userIdAttribute` in order to successfully resolve a mail address. Default is `oxUserName`
- `com.openexchange.mailmapping.ldap.contextIdAttribute` Specifies the attribute which is used to find the contextId in the LDAP search result. The LDAP search result need to contain a value for this attribute or for `contextNameAttribute` in order to successfully resolve a mail address. Default is `oxContextId`
- `com.openexchange.mailmapping.ldap.contextNameAttribute` Specifies the attribute which is used to find the context name in the LDAP search result. The LDAP search result need to contain a value for this attribute or for `contextIdAttribute` in order to successfully resolve a mail address. Default is `oxContextName`

Please see the [Configuration Documentation](/components/middleware/config{{ site.baseurl }}/index.html#mode=search&term=com.openexchange.mailmapping.ldap) for more details.

To speed up frequent accesses for the same mail addresses, the LDAP-based Mail Resolver is able to cache already resolved mail addresses (or a non-resolvable) for a short period of time. The expiry time can be configured via ``com.openexchange.mailmapping.ldap.cacheExpire`` (Default: 10 minutes).

# Installation
All Mail Resolvers are already included in package ``open-xchange-core``. The REST endpoint needs to be installed with package ``open-xchange-rest``.