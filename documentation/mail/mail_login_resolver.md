---
title: Mail Login Resolver
icon: fa-share
tags: LDAP, Contacts, Mail Login Resolver
---

# Introduction

Sharing mail folders requires resolving mailbox owners, or, more precisely, IMAP ACL's to user and context identifiers. Another use case for this kind of resolve operation is when iMIP messages pushed to the server are used in combination with the IMAP login (as per [``com.openexchange.calendar.pushedIMipResolveMode``](/components/middleware/config{{ site.baseurl }}/index.html#mode=search&term=com.openexchange.calendar.pushedIMipResolveMode)).

Since 7.10.6 Open-Xchange Servers allows resolving them using a dedicated Mail Login Resolver Service. The standard LDAP-based Mail Login Resolver will be described in this article.

# LDAP-based Mail Login Resolver

The standard LDAP-based Mail Login Resolver is able to resolve mailbox owners to user and context identifiers. In order to show user information about an already shared mail folder, the resolver is also capable to resolve user and context identifiers to mail logins.

## Configuration

To enable the Mail Login Resolver Service, it is necessary to set `com.openexchange.mail.login.resolver.enabled=true`. This will make the service available in the system generally.

In order to enable the standard LDAP-based Mail Login Resolver as well, it is needed to set `com.openexchange.mail.login.resolver.ldap.enabled=true` and requires a proper client configuration to be set via ``com.openexchange.mail.login.resolver.ldap.clientId``. Therefore, a section within ``ldap-client-config.yml`` needs to be defined first. See [LDAP Client Configuration]({{ site.baseurl }}/middleware/administration/ldap_client_configuration.html) for further details. 

Additionally, the resolver requires some more LDAP-specific configuration, which will be explained in the following.

Note that different configurations can be used by configuring the values through the config-cascade (up to scope ``context``). However, if the mail login resolver needs to be used by the iMIP push notification endpoint ([see here for details](https://documentation.open-xchange.com{{ site.baseurl }}/middleware/calendar/iTip.html#implementation-details)), only a system-wide defined mail login resolver can and will be used.

### ACL2Entity

Resolving mailbox owners (ACLs) to user and context identifier can be configured with the following properties:

- `com.openexchange.mail.login.resolver.ldap.mailLoginSearchFilter` Specifies the LDAP search filter to find mail logins by userId and contextId. The placeholder <code>[mailLogin]</code> will be replaced by the given mail login. Default is `(oxLocalMailRecipient=[mailLogin])`
- `com.openexchange.mail.login.resolver.ldap.mailLoginSearchScope` Specifies the LDAP search scope to resolve mail logins. Default is `SUB`
- `com.openexchange.mail.login.resolver.ldap.userIdAttribute` Specifies the attribute which is used to find the userId in the LDAP search result. The LDAP search result need to contain a value for this attribute or for `userNameAttribute` in order to successfully resolve a mail login. Default is `oxUserId`
- `com.openexchange.mail.login.resolver.ldap.userNameAttribute` Specifies the attribute which is used to find the user name in the LDAP search result. The LDAP search result need to contain a value for this attribute or for `userIdAttribute` in order to successfully resolve a mail login. Default is `oxUserName`
- `com.openexchange.mail.login.resolver.ldap.contextIdAttribute` Specifies the attribute which is used to find the contextId in the LDAP search result. The LDAP search result need to contain a value for this attribute or for `contextNameAttribute` in order to successfully resolve a mail login. Default is `oxContextId`
- `com.openexchange.mail.login.resolver.ldap.contextNameAttribute` Specifies the attribute which is used to find the context name in the LDAP search result. The LDAP search result need to contain a value for this attribute or for `contextIdAttribute` in order to successfully resolve a mail login. Default is `oxContextId`

 

Please see the [Configuration Documentation](/components/middleware/config{{ site.baseurl }}/index.html#mode=search&term=com.openexchange.mail.login.resolver) for more details.

### Entity2ACL

Resolving user and context identifiers to mailbox owners (ACLs) can be configured with the following properties:

- `com.openexchange.mail.login.resolver.ldap.entitySearchFilter` Specifies the LDAP search filter to find entities by their mail logins. Placeholder <code>[cid]</code> and <code>[uid]</code> will be replaced by the given userId and contextId. Default is `(&(oxContextId=[cid])(oxUserId=[uid]))`
- `com.openexchange.mail.login.resolver.ldap.entitySearchScope ` Specifies the LDAP search scope to resolve entities. Default is `SUB`
- `com.openexchange.mail.login.resolver.ldap.mailLoginAttribute` Specifies the attribute which is used to find the mail login in the LDAP search result. Default is `oxLocalMailRecipient`

Please see the [Configuration Documentation](/components/middleware/config{{ site.baseurl }}/index.html#mode=search&term=com.openexchange.mail.login.resolver) for more details.

### Cache

To speed up frequent accesses for the same mail login, the LDAP-based Mail Resolver is able to cache already resolved mail logins (or a non-resolvable) for a short period of time. Since it is possible to configure multiple LDAP services, which could resolve a single mail login to different user and context identifiers, the resolved mail logins are cached per LDAP configuration. The expiry time can be configured via ``com.openexchange.mail.login.resolver.ldap.cacheExpire`` (Default: 10 minutes).