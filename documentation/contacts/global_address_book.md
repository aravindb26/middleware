---
title: Global Address Book
icon: fa-user-circle
tags: Contacts, Global Address Book
---

# Introduction

The Global Address Book is a folder inside the Address Book module, which contains a contact for each user of the context. In order to see the Global Address Book, users need to have the `gab` capability. 

# Folder Permission Mode

The permissions in the global address book folder can be either assigned individually per user (mode ``individual``), or as single group permission entity (mode ``global``).  If the mode `global` is chosen, the special "all users and groups" permission will grant access to the global address book for users. If the users have edit permissions or not is decided based on the property [ENABLE_INTERNAL_USER_EDIT](https://documentation.open-xchange.com/components/middleware/config/latest/#mode=search&term=ENABLE_INTERNAL_USER_EDIT). If the mode `individual` is chosen, each user will have a dedicated permission for the global address book. Please keep in mind that the modus will affect the response for folder requests regarding the global address book, `global` delivering two entries, `individual` delivering `1 + n` entries where `n` is the number of users in the context.

Whether one or the other is used can be defined during context creation, or later on via the administrative utility [restoregabdefaults](https://documentation.open-xchange.com{{ site.baseurl }}/middleware/command_line_tools/contacts/gabrestore.html).

# Folder Name

The default name is `Global Address Book`, but can be configured to a different name. In order to change the default, the following property needs to be set:

- `com.openexchange.contacts.gabFolderName` Defines the Global Address Book folder name. *Default:* `global_address_book`

The property supports various pre-defined values, e.g. `global_address_book`, `internal_users` and `all_users`. Those values result in the folder names `Global Address Book`, `Internal Users` and `All Users` or their corresponding translation.

Furthermore, it is possible to configure a custom folder name along with its translation. Therefore, ``com.openexchange.contacts.gabFolderName`` needs to be set to `custom`. The actual folder name as well as translations have to be set via ``com.openexchange.contacts.customGabFolderName.[locale]``. It is also recommended to add custom folder names as folder reserved names in file `/opt/open-xchange/etc/folder-reserved-names` to prevent unintended side effects.

Here is an example how to configure a custom Global Address Book name:

```properties
com.openexchange.contacts.gabFolderName=custom
com.openexchange.contacts.customGabFolderName=Family Members
com.openexchange.contacts.customGabFolderName.de_DE=Familienmitglieder
```

All the above properties are reloadable and config-cascade aware down to scope `context`. See [Configuration Documentation](https://documentation.open-xchange.com/components/middleware/config{{site.baseurl}}/index.html#mode=tags&tag=Global%20Address%20Book) for more details.