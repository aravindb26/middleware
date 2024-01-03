---
title: Redis Session Storage
icon: far fa-clock
tags: Session, Installation, Configuration
---

# Introduction
With v8.0.0 the Open-Xchange Middleware offers an alternative way to manage and store sessions using [Redis in-memory data structure store](https://redis.io/). The Redis store is used as central session storage replacing both - in-memory per-node SessionD and (Hazelcast-backed) session storage.

Since it acts as a central session storage, many problems related to remote invalidation of sessions are thus solved while maintaining the benefits offered by Hazelcast-backed session storage: sessions survive server restarts and/or upgrades, and are accessible for each node, independently from where the session got spawned.

# Installation
This feature is included in ``open-xchange-core`` package. Thus, no additional packages need to be installed.

However, to finally enable Redis Session Storage, the property `"com.openexchange.sessiond.redis.enabled"` needs to be set to `"true"`. By doing this, the former in-memory SessionD storage is effectively disabled. Also, the Hazelcast-backed session storage should be disabled by setting `"com.openexchange.sessionstorage.hazelcast.enabled"` to `"false"`. 

Of course, a running Redis installation is required and appropriate configuration settings for the Open-Xchange Redis Connector need to be defined.

```
# Properties for Redis connector

# Enable Redis connector
com.openexchange.redis.enabled=true
# Configure accessing Redis end-point
com.openexchange.redis.mode=standalone
com.openexchange.redis.hosts=localhost:6379
com.openexchange.redis.ssl=false
com.openexchange.redis.starttls=false

# If left empty means no user-name and/or password required
com.openexchange.redis.username=
com.openexchange.redis.password=
```

There are a bunch more properties that can be set.

# Configuration
As mentioned previously, the property `"com.openexchange.sessiond.redis.enabled"` needs to be set to `"true"`. Also, the Hazelcast-backed session storage should be disabled by setting `"com.openexchange.sessionstorage.hazelcast.enabled"` to `"false"`. 