---
title: Contact Collector
icon: fa-solid fa-rectangle-history-circle-user
tags: Contacts
---

This article describes the contact collector feature.

# Overview

The contact collector feature helps users to find contacts more easily. 
For this purpose the contact collector provides two functionalities.
For one as the name suggests it collects contacts (e-mails) used by the user and stores them inside a special address book folder. 
This way once a user has interacted with a contact in any way (e.g. mail or calendar) the mail address is stored an can be accessed again without the need to create a contact manually.

On the other hand it also tracks the usage of contacts and groups and helps to sort them according to this usage. 
This feature also known as object use count is especially helpful for users in case a they have many contacts with the same or similar names. 
The contacts which are used more are then shown first according to their use count.
For example if a user has two contacts named "John Doe" and "Jane Doe" and he/she interacts with John more than with Jane then John's contact is shown first during auto-complete of "Doe".

# Details

This chapter describes the details of the contact collector and explains how it works internally.

## Folder Creation

The folder used to collect contacts is created for each user during login. It's name ("Collected addresses") is predefined but always localized according to the configured language of the user.

## Contact Collector trigger

Currently the contact collector is triggered if the user "uses" an email address or contact. This includes but is not necessarily limited to the following use cases:

* Sending or accessing an email containing the contact
* Inviting the contact to an event
* Sharing something with the contact (e.g. a file or a folder)
* Invite contact to participate in a task

## Contact creation

If the contact collector is triggered and no contact with the given email is found then a new contact within the collected addresses folder is created.
Depending on the configuration the user is able to delete this folder. But it will be recreated during the next login. 

## Use Count

The use count is collected for internal and external contacts, groups and resources.

If the collector is triggered it increases the use count for all used entities by 1. Additionally once a day a cleanup job runs and reduces the use count of all unused entities by one (see chapter Configuration for more informations). Please note that decrementing the use count also touches the entity so that it will only be decremented again after the defined cleanup timespan has passed.

## Configuration

This chapter describes all relevant properties for the contact collector.

First of all you can enable or disable this feature globally with this property: 

```
com.openexchange.contactcollector.enabled = true
```

In case there are multiple contacts in the address book with the same email address (e.g. in case a user tracks private and business contacts separately) the contact collector tries to increase the use count of all contacts with the same email address.
The following property can be used to apply a sane limit to the amount of contacts increased. If this is set to a value bigger than 0 then the use count is only applied to the first x contacts. The contacts are hereby sorted according to their current use count which means that only the x most used contacts are increased.

```
com.openexchange.contactcollector.searchLimit = 5
```

Normally the user can always delete the contact collector folder, but as an administrator you can prevent that with the following property:

```
com.openexchange.contactcollector.folder.deleteDenied = false
```

Sometimes the collected contacts can pile up and old contacts which were only used once or twice unnecessarily stick around. To prevent that the administrator can configure a threshold time-span after which contacts are cleaned up.
Be aware that the cleanup is not triggered periodically but rather every time a user triggers the contact collector. There are also some additional rules in place which should prevent deletion of still used contacts:
* Only contacts with a use count of less than 2
* Only unchanged contacts (only has the initial fields set)

```
com.openexchange.contactcollector.cleanupUnusedAfter = 0
```

Sometimes a user interacts in short bursts with certain contacts. E.g. in case the user participates in a short term project. In those cases the use count of such contacts can reach a certain amount which overshadows other more recently used contacts.
To strengthen the more recently used contacts once a day a cleanup job reduces the use count of older unused contacts. As an administrator you can use the following property to configure a time-span after which the use count of an unused contact is reduced.

```
com.openexchange.objectusecount.cleanupTimespan = 4W
```

Per default the contact collection is triggered whenever the user uses the email address of a contact (e.g. in case he creates an appointment with a contact) 
with the exception of email operations. You can however configure that operations in the mail module trigger contact collection with the following properties: 

```
com.openexchange.user.contactCollectOnMailAccess = true
com.openexchange.user.contactCollectOnMailTransport = true
```

In case of external storages which doesn't have a built in counter you can increase the limit of contacts requested with a look-ahead factor. 
This will improve the quality of the results. By default the limit is increased by factor 10. This means 10 times more contacts are requested. 

```
com.openexchange.contacts.useCountLookAhead = 10
```

