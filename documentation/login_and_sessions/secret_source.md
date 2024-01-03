---
title: Secret Source
icon: fa-solid fa-key
tags: Secret
---

# Secret Source

This article describes how to configure different secret sources. 

The secret source value is used to decrypt/encrypt the users password. E.g. for external mail accounts. 
It is provided via the `com.openexchange.secret.secretSource` property.

The source accepts either a single source or a list.

## Single source

A source is a combination of literals and placeholders connected by the '+' sign. 
Literals are surrounded by single quotes (e.g. '@'). 
Placeholders are as the name suggests placeholders for values injected during runtime.
They are surrounded by smaller than and greater than signs (e.g. <password>).

A valid source could look like this: `<user-id> + '@' + <context-id>`

Here are the supported placeholders:

### password

Denotes the user password.

### user-id

Denotes the user identifier.

### context-id

Denotes the context identifier.

### random

Denotes the value specified in property `com.openexchange.secret.secretRandom`.

### session-parameter:[parameterName]

This denotes a session parameter. This is a special placeholder which accepts the parameter name as an argument.

E.g. `<session-parameter:__session.hostname>`

The above example would use the hostname of the session as a secret source.

## List

As suggested by the name, it is also possible to define a list of secret sources. A list can be useful in case you want 
to change your secret source and still want to support the decryption/encryption via the old secret source.

To configure the usage of lists, use the special `<list>` placeholder.

```
com.openexchange.secret.secretSource=<list>
```

Afterwards, the `secrets` file is used to load secret sources. Every line inside the secret file contains a secret source.
The secret sources are used in order to decrypt/encrypt the users password, until decryption/encryption is successful.

