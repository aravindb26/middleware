---
title: Contacts Provider LDAP
icon: fa-address-book
tags: LDAP, Contacts, Administration
---

This article describes how to setup and configure a contact provider plugin connecting to a director server via LDAP (Lightweight Directory Access Protocol). This integration option supersedes the previously available [LDAP Contact Storage](https://documentation.open-xchange.com/7.10.6/middleware/contacts/ldap_contact_storage.html).

# Overview

In enterprise installations, there is often a central directory service that hosts the user data or other addressbook related entries. The Open-Xchange Server can be configured to access such data available from LDAP directories and integrate it in terms of address book folders in the groupware. For end-users and from a client's perspective, there's basically no difference to other contact folders, so that they can access the contents from the LDAP directory in a transparent way, e.g. when looking up participants for an appointment or choosing recipients for an e-mail message. 

Internally, one or more LDAP search filters will represent read-only address books in the public folders tree. Entries matching these filters are then converted to OX contacts and distribution lists. All operations in these folders are passed through to the directory server directly, i.e. there is no synchronization to the local database storage in the background.

# Requirements

* Open-Xchange Server v7.10.6 and above (``open-xchange-core``, ``open-xchange-contacts-provider-ldap``)
* An LDAP compatible directory server (e.g. OpenLDAP, Microsoft Active Directory)

# Configuration

The following steps provide a walkthrough to configure access to the LDAP directory. All configuration files and properties are *reloadable*, so that no additional server restart is required after the plugin's package was installed.

## Install required Bundles

If not yet done, install the following additional package on the server: ``open-xchange-contacts-provider-ldap``.

## LDAP Client Configuration

All connection-related settings are made through the common LDAP client configuration. Therefore, a section within ``ldap-client-config.yml`` needs to be defined first. See [LDAP Client Configuration]({{ site.baseurl }}/middleware/administration/ldap_client_configuration.html) for further details.

## Configure Mappings

Besides the LDAP client configuration, another prerequisite are the attribute mappings to define how LDAP entries can be converted to groupware contacts and distribution lists. After installation, mapping templates for typical OpenLDAP and Active Directory Servers can be found in our [core repository](https://gitlab.open-xchange.com/middleware/core/-/blob{{ site.branchName }}/com.openexchange.contact.provider.ldap/doc/examples/contacts-provider-ldap-mappings-template.yml). It's recommended to copy over this template file to ``/opt/open-xchange/etc/contacts-provider-ldap-mappings.yml`` and adjust the mappings as needed. Each configured set of mappings can be used for an LDAP contact provider (as defined later through separate file ``contacts-provider-ldap.yml``), by using the corresponding identifier used in this ``.yml`` file.

Generally, contact properties are set based on an entry's value of the mapped LDAP attribute name. Empty mappings are ignored. It's possible to define a second LDAP attribute name for a property that is used as fall-back if the first one is empty in an LDAP result, e.g. to define multiple attributes for a display name, or to have multiple mappings for contacts and distribution lists. Therefore, the second attribute name can be appended as comma-separated value.

For the data-types, each LDAP attribute value is converted/parsed to the type necessary on the server (Strings, Numbers, Booleans). Dates are assumed to be in UTC and parsed using the pattern ``yyyyMMddHHmmss``. Binary properties may be indicated by appending ``;binary`` to the LDAP attribute name, and as special variant, Microsoft GUIDs can be decorated with the ``;guid`` flag for proper conversion. Boolean properties may also be set based on a comparison with the LDAP attribute value, which is defined by the syntax ``[LDAP_ATTRIBUTE_NAME]=[EXPECTED_VALUE]``, e.g. to set the ``mark_as_distribution_list`` property based on a specific ``objectClass`` value. Alternatively, a Boolean value may also be assigned based on the the existence of any attribute value using ``*``.

## Define Contacts Provider

In order to make LDAP address books available for users, the contacts provider needs to be defined and registered in the system. Therefore, a new section in ``contacts-provider-ldap.yml`` needs to be inserted, whose key becomes the identifier of the contacts provider. When starting from scratch, it is recommended to copy the template file, which can be found in our [core repository](https://gitlab.open-xchange.com/middleware/core/-/blob{{ site.branchName }}/com.openexchange.contact.provider.ldap/doc/examples/contacts-provider-ldap-template.yml).

In the general section, besides a name, values for ``ldapClientId`` and ``mappings`` need to be provided. The ``ldapClientId`` references the corresponding section in ``ldap-client-config.yml``, while the ``mappings`` point to the set of mappings defined at ``contacts-provider-ldap-mappings.yml``.

Then, in the ``folders`` section, the configuration of the available address book folders can be made. For this purpose, three different modes are available. Also, it is possible to configure how the folders of the contacts provider behave regarding appearance in the folder tree and contact picker dialogs, as well as towards synchronization. See the examples in the template file for further details.

### static

In *static* folder mode, a fixed list of folder definitions is used, each one with its own contact filter and name (the names must be unique). Additionally, a ``commonContactFilter`` needs to be defined, which is used for operations that are not bound to a specific folder, like lookups across all visible folders. The filter's search scopes relative to the LDAP client's 'baseDN' can be configured as ``one`` (only immediate subordinates) or ``sub`` (base entry itself and any subordinate entries to any depth), and all default to ``sub`` unless specified otherwise.

### dynamicAttributes

With mode *dynamic attributes*, all possible values for one attribute are fetched periodically and serve as folders. The list of values is fetched by querying all entries that match the ``contactFilterTemplate`` (with the wildcard ``*`` as value) and ``contactSearchScope`` (``one``/``sub``). Then, the folders are derived based on all distinct attribute values found, with the value as name.

Depending on the configured authentication mode, this is either done per user individually, or globally. Therefore, this mode is neither recommended nor supported when using per-user authentication. The ``refreshInterval`` determines how often the list of attributes is refreshed.

### fixedAttributes

With mode *fixed attributes*, all entries matching a filter and having an attribute set to one of the defined values do form a folder. Works similar to *dynamic attributes*, but with a static list of possible values, and can also be used with individual / per-user authentication. All items defined in the ``attributeValues`` array are used as folder (with the value as name). When listing the contents of a specific folder, this folder's specific attribute value is inserted in the configured ``contactFilterTemplate``, using the 
``contactSearchScope`` (``one``/``sub``).

## Enable Contacts Provider

After all necessary things are configured for the contacts provider, it can be enabled for users using its identifier (as used as key of the provider definition in ``contact-provider-ldap.yml``). Therefore, the property [com.openexchange.contacts.ldap.accounts](https://documentation.open-xchange.com/components/middleware/config{{ site.baseurl }}/#mode=search&term=com.openexchange.contacts.ldap.accounts) can be used, which accepts a comma-separated list of contacts provider identifiers. This property can be defined in the most flexible way through the [Config Cascade]({{ site.baseurl }}/miscellaneous/config_cascade.html), so that different sets of users can be granted access to providers as needed.

# Upgrading from Contact Storage LDAP

The previously available plugin ``open-xchange-contact-storage-ldap`` will continue to work temporary in v7.10.6 but is removed with v8.0.0. Therefore, it is recommended to upgrade to the new contacts provider plugin.

Compared with the legacy contact storage plugin, the new provider does integrate in a native way where the addressbook folders are directly provided by the plugin itself, i.e. there is no longer a regular database folder underneath where just the actual contents are delivered by the storage plugin. Therefore, it is no longer bound to a certain database folder that is configured. Furthermore, a defined storage is also not bound to a specific context anymore, so that the same contacts provider configuration can be shared across multiple contexts of the installation (assignable to users through the config cascade). Therefore, these kind of configuration settings are no longer required. Also, the previously required ID mapping is no longer needed, as textual identifiers can be used as-is in the meantime.

Besides that, the new plugin requires mostly the same configuration settings, however, the format was adjusted from plain ``.properties`` files to structured ``.yml`` files. So, transferring a previous configuration to the new format involves the following steps:

- Configure Connection Settings
  The connection-related settings (like LDAP server URI or Base DN), as well as the authentication configuration need to be done in the common LDAP client file ``ldap-client-config.yml``.
- Define Mappings
  The previously used mappings (in ``.properties`` format) need to be transferred into the corresponding definitions in the configuration file ``contacts-provider-ldap-mappings.yml``. 
- Prepare Provider Configuration
  A new provider configuration can then be inserted in the file ``contacts-provider-ldap.yml`` (or copied over from the corresponding template). There, the ``mappings`` and ``ldapClientId`` configurations need to be linked. Then, all other required settings can be taken over (like cache configuration etc.).
- Setup Folder(s)
  Now, one or more address book folder(s) can be specified. A previously configured filter from ``com.openexchange.contact.storage.ldap.searchfilter`` can be directly transferred to a single folder definition in ``static`` mode. Alternatively, one can configure additional folders in the provider, too. 
- Enable Contacts Provider
  As last step, the contacts provider can be actually enabled for a specific group of users, by specifying the provider's identifier (the key used in the YAML file) in the config-cascade enabled property ``com.openexchange.contacts.provider.ldap``.
- Remove Legacy Folder
  The previous storage integration worked on top of an ordinary public folder that is stored in the database. After the legacy configuration from ``open-xchange-contact-storage-ldap`` is no longer active, a corresponding folder remnant can be removed manually by logging in as context admin into App Suite and deleting it from there if it is no longer needed.


# Misc

## Extended Folder Settings

The following extended settings are available to control the appearance of the address book folders in App Suite. For each of the settings, a default value can be specified, and additionally a *protected* flag that prevents changing the actual value through the client APIs. The settings can be defined in the ``folders`` section of the configuration file ``contacts-provider-ldap.yml``, see the provided template for further details.

### usedForSync

Configures if the addressbook folders can be synchronized to external clients via CardDAV or not. If set to ``false``, the folders are only available in the web client. If set to ``true``, folders can be activated for synchronization. Should only be enabled if attribute mappings for the ``changing_date`` and ``uid`` contact properties are available, and the LDAP server supports the special "LDAP Show Deleted Control" to query tombstone entries via ``isDeleted=TRUE``.

### usedInPicker

Defines whether addressbook folders will be available in the contact picker dialog of App Suite. If enabled, contacts from this provider can be looked up through this dialog, otherwise they are hidden.

### shownInTree

Defines whether addressbook folders will be shown as 'subscribed' folders in the tree or not. If enabled, the folders will appear in the contacts module of App Suite as regular, subscribed folder. Otherwise, they're treated as hidden, unsubscribed folders.

## Read-only

The LDAP contact storage works in read-only mode, meaning that any attempts to create new, delete, or modify existing contacts in the folder are rejected.
  
## Incremental Synchronization

Different external Clients accessing the server via CardDAV have the requirement that the server is able to report all changes in a contact folder since the last synchronization. This includes updates to existing contacts, as well as deleted and newly created contacts. In order to use folders backed by the LDAP contact storage with such clients, the LDAP directory needs to be able to deliver these information, especially the so-called *tombstones* for deleted contacts, i.e. the knowledge about deleted directory entries, which is currently only available in Active Directory and must be explicitly turned on via ``isDeletedSupport`` in the provider configuration.

Furthermore, a valid attribute mapping for "last modified" must be provided.

## Caching

To speed-up access to the LDAP directory, some contact properties can be held in a local cache. However, this can only be used if no individual authentication is used which would lead to different views on the LDAP server. If enabled, a certain set of contact properties is kept in memory, and all client operations accessing the contacts will use the cached data preferably to speed up access. However, a corresponding amount of memory is consumed locally by the middleware process on each node, and potentially stale data is used.

To enable caching just add a `cache` element to your contact provider configuration which contains a `useCache` field set to `true`. Additionally you can also configure the fields to cache by adding the `cachedFields` field with a comma separated list of contact fields.

You can also configure the cache expire time by configuring the `com.openexchange.contacts.ldap.cache.expire` property. This way the cache will be regulary refreshed with new data and it will also be removed from memory in case it is not needed.

## Internal Users / Global Addressbook

Using the corresponding mappings for the contact properties ``internal_userid`` and ``contextid``, it is also possible to make contact data from internal users provisioned to the system available in LDAP address books, and let them be recognized as such by App Suite. The mapping can either be performed directly when pointing to properties yielding the numerical identifiers, or by using attributes that hold the corresponding login information (username / contextname), which are then resolved dynamically by the middleware, which can be indicated by the ``;logininfo`` flag in the property mappings. Please see the explanations in file ``contacts-provider-ldap-mappings-template.yml``, which can be found in our [core repository](https://gitlab.open-xchange.com/middleware{{ site.branchName }}/blob/main/com.openexchange.contact.provider.ldap/doc/examples/contacts-provider-ldap-mappings-template.yml) for further details.

**Direct Mapping of User- / Context-ID:**

Given that the App Suite context identifier is stored within an _Integer_ attribute named ``oxContextId``, and the App Suite user identifier in an attribute named ``oxUserId`` in the directory server, an appropriate mapping would look like the following:
```properties
  internal_userid : oxUserId
  contextid       : oxContextId
```

**Indirect Mapping of User- / Contextname :**

If no numerical identifiers are available as LDAP attributes, but the context name as provisioned in App Suite is stored within an attribute named ``oxContextName``, and the provisioned user name in an attribute named ``oxUserName`` in the directory server, the mapping would look like:
```properties
  internal_userid : oxUserName;logininfo
  contextid       : oxContextName;logininfo
```

Please note that it is required to have mappings for both the internal user- as well as the context identifiers, as the same LDAP contacts provider definition can be used throughout multiple contexts of the system. Based on the context information, user contacts are then exposed as internal user contacts or normal/external contacts dynamically based on the context of the actually requesting user.

If the contact information from all provisioned users is stored in the directory server and made available through the LDAP contacts provider plugin, the special context-internal global address book can also be disabled to avoid ambiguities and redundant data appearing from multiple sources. This can be achieved by setting the corresponding module permission ``globaladdressbookdisabled``.

Note: In order to disable the global address book for non-PIM users, a rather historic permission check needs to be disabled by setting [com.openexchange.admin.bypassAccessCombinationChecks](https://documentation.open-xchange.com/components/middleware/config{{ site.baseurl }}/#mode=search&term=com.openexchange.admin.bypassAccessCombinationChecks) to ``true``.

Also, the configuration switch [ENABLE_INTERNAL_USER_EDIT](https://documentation.open-xchange.com/components/middleware/config{{ site.baseurl }}/#mode=search&term=ENABLE_INTERNAL_USER_EDIT) should be set to ``FALSE`` in such scenarios, which will effectively hide the corresponding dialog in App Suite that would otherwise access the contact data bits that have been provisioned to the system.
