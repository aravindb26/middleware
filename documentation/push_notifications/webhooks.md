---
title: Webhooks
icon: fa-webhook
tags: Push, Installation, Configuration
---

# Webhook documentation

With Open-Xchange Middleware version 8.18.0 Webhooks are supported. However, only known end-points are called.

## Configuration

The configuration of known Webhooks is done in file `webhooks.yml` in YAML notation. On root level a unique Webhook identifier is specified.
Then under that identifier the Webhook properties are described:

* `uri`
  The URI end-point of the Webhook. May be overridden during subscribe depending on "uriValidationMode".
* `uriValidationMode`
  Specifies how the possible client-specified URI for a Webhook end-point is supposed to be validated against the URIfrom configured Webhook end-point. Possible values: `none`, `prefix`, and `exact`. For `none` no requirements given. Any client-specified URI is accepted. For `prefix` he client-specified and configured URI for a Webhook end-point are required to start with same prefix. For `exact` the client-specified and configured URI for a Webhook end-point are required to be exactly the same. `prefix` is default.
* `webhookSecret`
  The value for the "Authorization" HTTP header to pass on calling Webhook's URI. May be overridden during subscribe.
* `login`
  The login part for HTTP Basic Authentication if no value for the "Authorization" HTTP header is specified. May be overridden during subscribe.
* `password`
  The password part for HTTP Basic Authentication if no value for the "Authorization" HTTP header is specified. May be overridden during subscribe.
* `signatureSecret`
  Specifies shared secret known by caller and Webhook host. Used for signing.
* `version`
  Specifies the version of the Webhook. Used for signing.
* `signatureHeaderName`
  Specifies the name of the signature header that carries the signature.
* `maxTimeToLiveMillis`
  The max. time to live in milliseconds for the Webhook before considered as expired. If absent Webhook "lives" forever.
* `maxNumberOfSubscriptionsPerUser`
  The max. number of subscriptions for this Webhook allowed for a single user. Equal or less than 0 (zero) means infinite.
* `allowSharedUri`
  Whether the same URI can be used by multiple different users or not. Optional, defaults to `true`.

Example:

```
# An example using "Authorization" HTTP header
mywebhook:
    uri: https://my.endpoint.com:8080/webhook/event
    webhookSecret: supersecret
    signatureSecret: da39a3ee5e6b4b
    version: 1
    signatureHeaderName: X-OX-Signature
    maxTimeToLiveMillis: 2678400000
    maxNumberOfSubscriptionsPerUser: 2
    uriValidationMode: prefix


# An example using HTTP Basic Authentication
mywebhook2:
    uri: https://my.another-endpoint.com:8080/webhook2/event
    login: admin
    password: topsecret
    signatureSecret: fe39a3e12e6b8a
    version: 1
    signatureHeaderName: X-Custom-Signature
    uriValidationMode: exact

```

Finally, the Webhooks that shall be actually accessed by users are required to be specified through `com.openexchange.webhooks.enabledIds` property.

Moreover, there are associated properties for the Webhook Push Transport itself that need to be aligned as needed.

h3. Full example

`webhooks.yml`

```
webhook-test:
    uri: https://webhook-test.example.com/webhook2/event
    signatureSecret: da39a3ee5e6b4b
    uriValidationMode: prefix
```

`webhook.properties`

```
com.openexchange.webhooks.enabledIds=webhook-test
com.openexchange.pns.transport.webhooks.enabled=true
com.openexchange.pns.transport.webhooks.httpsOnly=true
com.openexchange.pns.transport.webhooks.allowLocalWebhooks=false
com.openexchange.pns.transport.webhooks.allowTrustAll=false
com.openexchange.pns.transport.webhooks.http.maxConnections=100
com.openexchange.pns.transport.webhooks.http.maxConnectionsPerHost=100
com.openexchange.pns.transport.webhooks.http.connectionTimeout=5000
com.openexchange.pns.transport.webhooks.http.socketReadTimeout=5000
```

