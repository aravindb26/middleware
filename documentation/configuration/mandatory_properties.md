---
title: Mandatory Properties
icon: fa fa-exclamation-circle
tags: Configuration, Kubernetes, Helm
---


# Mandatory Properties

For security reasons, the Open-Xchange Server requires some property values to be unique for every deployment. Therefore, the following properties do not have a default value after 8.2 anymore:

`/opt/open-xchange/etc/server.properties`
```
# Cookie hash salt to avoid a potential brute force attack to cookie hashes.
# This property is mandatory and needs to be set to any random String with at least 16 Characters.
com.openexchange.cookie.hash.salt=
```

`/opt/open-xchange/etc/share.properties`
```
# Defines a key that is used to encrypt the password/pin of anonymously 
# accessible shares in the database.  
# This property is mandatory and needs to be set before the creation of the first share on the system.
# Default is no value
com.openexchange.share.cryptKey=
```

`/opt/open-xchange/etc/sessiond.properties`
```
# Key to encrypt passwords during transmission during session migration. 
# This property is mandatory. Please make sure it's the same in the entire cluster
com.openexchange.sessiond.encryptionKey=
```

If those properties are not set, the Open-Xchange Server will not start successfully and log prominent errors during startup.

## Upgrade from 8.2

⚠️ If you are upgrading from 8.2 and have NOT changed the above properties before, you have to set them manually now. In order to avoid data loss, you need to set them to their previous defaults.

The old defaults are:

```
com.openexchange.cookie.hash.salt=replaceMe1234567890
com.openexchange.share.cryptKey=erE2e8OhAo71
com.openexchange.sessiond.encryptionKey=auw948cz,spdfgibcsp9e8ri+<#qawcghgifzign7c6gnrns9oysoeivn
```

## Helm Chart

In Kubernetes, the configuration is usually managed via Helm Charts.
Since 3.2.1 the Open-Xchange Server Helm Chart manages the above properties in a Kubernetes Secret called `<RELEASE>-common-env`, where `<RELEASE>` is the release name of your Helm Release.
The Secret contains environment variables, which are injected into the containers and then written into the corresponding `.properties` files. 
It can be configured via global chart values:

```
global:
  appsuite:
    cookieHashSalt: "KtLUTLKZrbXvCAOn"
    shareCryptKey: "lJZEFPzUYfapWbXL"
    sessiondEncryptionKey: "auw948cz,spdfgibcsp9e8ri+<#qawcghgifzign7c6gnrns9oysoeivn"
```

This will create or update an already existing Kubernetes Secret with the provided values. If a value for a mandatory property was not set, Helm will generate a random value and update or create a new Secret for you.
Since losing the Secret will cause data loss, removing a Helm Release will not delete the Secret. Nevertheless, please make sure you do not lose the Secret!

