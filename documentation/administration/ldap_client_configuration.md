---
title: LDAP Client Configuration
icon: fas fa-cogs
tags: Administration, LDAP Client
---

This article will show you how you can configure the ldap clients the Middleware uses to make requests to ldap servers.

# Overview

Starting with version 7.10.6 of the Open Xchange Server it is possible to configure individual ldap clients for various features. For example the newly created ldap contact provider. 
This article shows how you configure those ldap clients and which option are available.

# Basic configuration

The clients (or rather the client pools) are configured via the `ldap-client-config.yml` yaml file. A more comprehensive example can also be found in our [core repository](https://gitlab.open-xchange.com/middleware/core/-/blob{{ site.branchName }}/com.openexchange.ldap.common/doc/examples/ldap-client-config-template.yml). Before we go into the details of the configuration options let me show you a simple working example:

```yml
myLdapClient1:
  pool:
    type: simple
    trust-store:
      trustAll: true
    host:
      address: localhost
      port: 389
    pool-min: 2
    pool-max: 10
    requiretls: true
  auth:
    type: adminDN
    adminDN: 
      dn: cn=admin,dc=ox,dc=test
      password: secret
  baseDN: dc=ox,dc=test      
```

Let me explain this example. As you can see the first level separates the different ldap clients via a unique id. 
The ldap client is always referenced by this id. Therefore every feature using ldap clients must be configured to use the client id. 
See the relevant feature documentation on how to do this. 

Below the client name you find two relevant configuration elements: `pool` on one hand and `auth` on the other. 
In the `pool` element you can configure all sorts of relevant attributes of the ldap client pool. 
For examle which type of pool to use, the size of the pool, connection properties like the connection timeout and so forth. 
The `auth` element on the other side defines how this pool authenticates against the ldap server. 
In the example above a simple pool with a trust all trust store is created. It connects against a local server on port 389 and creates at least 2 and maximal 10 connections.
It uses admin dn to authenticate with the provided credentials. 

Additionally, the optional `baseDN` configures a base path on the directory server, where all Distinguished Names supplied to and received from LDAP operations will be relative to. 
If not defined, the default naming context of the RootDSE is used as baseDN.


# Overview over all configuration options

## pool

### type

The type of the connection pool.  
Possible values:

* `simple` - a simple connection pool to a single ldap server
* `dnsRoundRobin` - a connection pool who uses dns round robin to connect to multiple ldap servers
* `roundRobin` - a connection pool who uses round robin to connect to multiple ldap servers
* `fewestConnections` - a connection pool who connects to the ldap server with the fewest connections
* `failover` - a connection pool with connects to a single ldap server but uses others as failovers
* `readWrite` - a connection pool which uses two other pools. One for read and one for write operations


### hosts 

An array of hosts. Each element is described with two elements: an address and an optional port. 
The port defaults to `10389`. 

Note:
You can also use `host` instead of `hosts` and provide just a single element instead of an array.


### requiretls

Whether to require TLS. Defaults to `true`.

### addressSelectionMode

The address selection mode used for a connection pool of the dnsRoundRobin type.  
Possible values:

* `failover` - always uses the returned serves in order they are returned
* `random` - uses a random address
* `round_robin` - uses round robin to select the address

Defaults to `random`.

### cacheTimeoutMillis

The cache timeout in milliseconds of resolved dns addresses. A value of zero or below indicates no caching. Defaults to `-1`. 
This option is only viable for the dnsRoundRobin type.

### onlyDns

Per default the java JNDI is used to acquire the ldap address. But if this setting is set to `true` then only the system dns is used. 
Defaults to `false`.

### initialConnections

The minimal / initial number of connection in the connection pool. Defaults to `5`.

### maxConnections

The maximum number of connections in the connection pool. Defaults to `20`.

### abandonOnTimeout

Whether to abandon requests which run into a timeout. Defaults to `false`.

### keepAlive

Whether to use the SO_KEEPALIVE socket option or not. Defaults to `true`.

### tcpNoDelay

Whether to use the TCP_NODELAY socket option or not. Default to `true`.

### retryFailedOperations

Set to `true` to retry failed operations with a new connection in case the failed operation indicates that the connection is not usable anymore. 
Defaults to `false`.

### synchronousMode

Whether to send request synchronous or not. Defaults to `true`.

### followReferrals

Whether to automatically follow referrals or not. Default to `false`.

### createIfNecessary

Whether to create a connection if necessary rather than waiting for a connection to become available or not.
Default to `true`.

### readPool

The id of another connection pool to be used for read requests. Only valid for `readWrite` pools.

### writePool

The id of another connection pool to be used for write requests. Only valid for `readWrite` pools.

### connectionTimeoutMillis

The connection timeout in milliseconds. Defaults to `10000`.

### maxMessageSize

The maximal message size in bytes. Defaults to 20,971,520 (\~20 MB)

### referralHopLimit

In case `followReferrals` is set to `true` this limit defines the maximum hop limit.
Defaults to `5`.

### maxConnectionAgeMillis

The maximum length of time in milliseconds that a connection should be allowed to be established 
before terminating and re-establishing the connection. A value of zero or below means no age check.
Defaults to `0`.

### maxWaitTimeMillis

Specifies the maximum length of time in milliseconds to wait for a connection to become available 
when trying to obtain a connection from the pool. A value of zero should be used to indicate that 
the pool should not block at all. Defaults to `0`.

### responseTimeoutMillis

The response timeout in milliseconds. A value of zero or less means that no default limit should be used.
Defaults to `0`. 


### healthCheckIntervalMillis

The length of time in milliseconds between periodic health checks against the available connections in this pool.
Defaults to `60000`.

### keyStore

The ssl keystore configuration.

#### id

The id of the keystore.

#### file

The path to the keystore file. This is an alternative to id.

#### password

The optional keystore password

#### alias

The certificate alias.

### trust-store

The ssl truststore configuration.

#### trustAll

Whether to use a trust all truststore or not. If set to `true` the truststore file is ignored.

#### id

The id of the truststore.

#### file

The path to the truststore file. This is an alternative to id.

#### password

The optional truststore password.

#### examineValidityDates

Indicates whether to reject certificates if the current time is outside the validity window for the certificate.
Defaults to `false`.

## auth

### type

The authentication type used to bind against the ldap server.  
Possible values:

* `ANONYMOUS` - anonymous authentication
* `ADMINDN` - authentication using static administrative credentials
* `USERDN_RESOLVED` - authentication using user individual credentials in combination with a template
* `USERDN_TEMPLATE` - authentication using user individual credentials. The dn is dynamically resolved
* `OAUTHBEARER` - authentication using the users oauth token

### adminDN

Additional options for the `adminDN` auth type.

#### dn

The distinguished name used for authentication.

#### password

The password used for authentication.

### userDNTemplate

Additional options for the `USERDN_TEMPLATE` auth type.

#### nameSource

Defines the source of the user name.
Possible values:

* SESSION - the login name found in the user's session is used as-is
* MAIL - the user's primary mail address is used
* USERNAME - the user name is used

#### dnTemplate

The template to use. Use `[value]` as a placeholder for the value defined by `nameSource`. 
For example: `cn=[value],dc=ox,dc=test`.

### userDNResolved

Additional options for the `USERDN_RESOLVED` auth type.

#### nameSource

Defines the source of the user name. See userDNTemplate for possible values.

#### searchFilterTemplate

The ldap filter template to use for the dn resolve. Use `[value]` as a placeholder for the value defined by `nameSource`. 
For example: `(&(objectClass=person)(mail=[value]))` 

#### searchScope

The ldap scope to use for the dn resolve.

#### searchAuthType

The auth type used for the dn discovery request.
Possible values:

* ANONYMOUS
* ADMINDN

## baseDN

The configured base LDAP path to use. If defined, all Distinguished Names supplied to and received from 
LDAP operations will be relative to the LDAP path supplied. If not defined, the default naming context of the RootDSE is used as baseDN.
