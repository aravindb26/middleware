---
title: Deputy permissions
icon: fa-lock
tags: Permissions, Capabilities
---

# Introduction

Since version v7.10.6 the Open-Xchange Middleware offers the feature to grant deputy permissions to one ore more users from the same context that are supposed to act as representatives (e.g. vacation replacement).

A deputy has configurable access to granting user's primary mail account's Inbox and/or standard Calendar folder. Moreover, a deputy might get the "send on behalf of" permission granted, which allows him to send E-Mails on behalf of the granting user.

# Installation

To install the deputy permission feature, the `open-xchange-deputy` package needs to be installed.

# Enable deputy permissions

To enable deputy permission management the `com.openexchange.deputy.enabled` property needs to be set to `true`, which is `false` by default. That property is fully config-cascade-aware.
After the feature is enabled, the user will gain access to a new deputy permission management menu in the App Suite UI.

# Behavior

Deputy permissions do overwrite possibly existent permissions. Moreover, granted deputy permissions cannot be changed/deleted via regular permission dialogue, but only via deputy permission management by the granting user.

Currently, deputy permissions for the following modules can be granted:

 * Mail
 * Calendar

Furthermore for the mail module, the granting user of the deputy permission can decide whether the deputy can sent mails using the granting user's mail account. If granted, mails can be send by the deputy "on behalf of" the granting user. To indicate that a deputy and not the granting user responded, the `Sender` header filled with the deputy's mail address will be set within the mail. Whereby the `From` header will be set to granting user's default sender address. E.g.:

```
From: Granting user <grantinguser@example.org>
Sender: Deputy <deputy@example.org>
```

In case the deputy has been given write permissions to the granting user's calendar, outgoing notification and/or iTIP mails (see also the [iTIP article]({{ site.baseurl }}/middleware/calendar/iTip.html)) will automatically be decorated with the "on behalf" relationship. This applies to the outgoing mails as well as for the generated iCAL files sent along the mails. E.g.:

```
ORGANIZER;CN=Granting user;SENT-BY="mailto:deputy@example.org";EMAIL=grantinguser@example.org:mailto:grantinguser@example.org
```

# Requirements

To grant deputy permission to the primary mail account's Inbox folder, the mail account is required to be an IMAP account that supports the RFC 2086 `ACL` and RFC 5464 `METADATA` extensions, and supports the `/shared/vendor/vendor.open-xchange/deputy` METADATA key.

For further reading on METADATA and METADATA keys, read for example the [Dovecot IMAP METADATA](https://doc.dovecot.org/configuration_manual/imap_metadata/) documentation
