---
title: Secondary Mail Accounts
icon: fa-at
tags: Mail, Configuration
---

# Introduction

With v7.10.6 the Open-Xchange Server offers possibility to allow users or groups access to so-called secondary mail accounts. Secondary mail accounts exist on the primary mail server in the same way as primary mail accounts, as "full" mail accounts. Thus, secondary mail accounts know the same users/groups and permissions to these entities.
In contrast to the primary mail accounts, secondary mail accounts aren't bound to a specific person, instead they're usually accessed and used by multiple users. Nevertheless, secondary mail accounts can be used to send mails, not as a specific user or a delegatee but with the specific mail address of the secondary mail account.

The typical use case for secondary mail accounts is, for example, access to a shared mailbox, such as info@mycompany.com.

# Installation

The secondary mail account feature will be shipped with the core packages of the Open-Xchange server. So, no further packages have to be installed.

# Provisioning

*Note:* The mail account which shall be used for a secondary mail account must exist in the mail server.

To manage certain users'/groups' access to secondary mail accounts there are a few command-line tools as well as a corresponding SOAP endpoint:

* `createsecondaryaccount` grants users/groups access to an existent secondary mail account; see [createsecondaryaccount]({{ site.baseurl }}/middleware/command_line_tools/secondary_accounts/createsecondaryaccount.html)

* `deletesecondaryaccount ` removes users'/groups' access to an existent secondary mail account; see [deletesecondaryaccount]({{ site.baseurl }}/middleware/command_line_tools/secondary_accounts/deletesecondaryaccount.html)

* `listsecondaryaccount ` list users'/groups' accesses to an existent secondary mail accounts; see [listsecondaryaccount]({{ site.baseurl }}/middleware/command_line_tools/secondary_accounts/listsecondaryaccount.html)

* `updatesecondaryaccount ` updates users'/groups' accesses to an existent secondary mail accounts; see [updatesecondaryaccount]({{ site.baseurl }}/middleware/command_line_tools/secondary_accounts/updatesecondaryaccount.html)

It is possible to provision the mail accounts to a certain list of users, as well as to user groups. However, please mind that when assigning secondary accounts to groups, the members are directly resolved to the corresponding users where the account will be added for individually, i.e. when the list of group members changes later on, this won't affect previously provisioned secondary mail accounts. So, these group arguments should only be treated as shorthand for listing the user ids explicitly. The `create`, `update` and `delete` calls require at least one user- or group identifier being specified. If the operation should be performed for all users of a context, one can use the special group id `0`.

The mail- and transport endpoint sources can be defined in three different modes. The "primary" option uses same end-point settings as primary account. "localhost" uses localhost as end-point. With mode "none", the endpoint data is taken over from the supplied options. If transport settings are not specified, the account acts a mail-access-only account; meaning it is not capable to send, but only access mails. The same applies to standard folders. If not set, the standard folders are initially empty, but are automatically determined on first access to that mail account individually for each user.

# Example

Let's say you have a mail address like support@mycompany.com. Further, we assume that currently the mail address is only used by the mail server to forward mails to multiple support team members. So currently each team member
* has to respond to an incoming mail with their personal mail
* can't see if a mail has been processed (if the mail has been read)
* can't see if a mail has been responded to (reply to the mail)

So some side-channel communication must be used to synchronize about the current status of the incoming mails. With secondary accounts we can solve those issues. So how do we introduce secondary mail accounts to each support team member?

First, you must create a real mail account on the mail server for the mail "support@mycompany.com". For this specific example you should also ensure that forwarding is removed beforehand. Once the new mail account is available, you can use the CLTs to add the mail account as secondary mail account for the whole team. Normally, "teams" are represented by "groups" at the Open-Xchange Server. Therefore, we assume here that the support team is also a known group of the Open-Xchange Server and it has the identifier `9`. Then, the command can look like this:

```
./createsecondaryaccount -A oxadmin -P secret -c 1 --groups 9 --login support@mycompany.com --password secret -e support@mycompany.com --name Support --personal Support --reply-to support@mycompany.com --mail-server imap.mycompany.com --mail-port 143 --mail-protocol imap --mail-secure false --mail-starttls false --transport-server smtp.mycompany.com --transport-port 25 --transport-protocol smtp --transport-secure false --transport-starttls false

Secondary account with primary address support@mycompany.com successfully created for specified users/groups in context 1
```

When the team members of the support team now log in into the App Suite UI, they are able to use the new mailbox "Support".

Let's say some time has passed and you must add a new member to the support team. Since for each member of the support team a dedicated mail account entry is created on the DB, we need to add the new team member in the secondary mail account, too. First, we start with displaying all available accounts:

```
./listsecondaryaccount -A oxadmin -P secret -c 1

context-id user-id account-id primary-address       name    login                 mail-server-URI               transport-login transport-server-URI        
         1       3          2 support@mycompany.com Support support@mycompany.com imap://imap.mycompany.com:143                 smtp://smtp.mycompany.com:25
         1       4          2 support@mycompany.com Support support@mycompany.com imap://imap.mycompany.com:143                 smtp://smtp.mycompany.com:25
```

Now we are going to add the member with the user ID 5 to the account:

```
./createsecondaryaccount -A oxadmin -P secret -c 1 --users 5 -e support@mycompany.com

Secondary account with primary address support@mycompany.com successfully updated from specified users/groups in context 1
```

Note: The `updatesecondaryaccount` CLT is used to update specific properties of secondary mail accounts like the name of the account. It is not used to add or remove accounts.

To verify that everything went as expected, we will run the list command again, but now with the `-cvs` option:

```
./listsecondaryaccount -A oxadmin -P secret -c 1 --csv

context-id,user-id,account-id,primary-address,name,login,password,personal,reply-to,mail-server-URL,mail-starttls,transport-login,transport-password,transport-server-URL,transport-starttls,archive-fullname,drafts-fullname,sent-fullname,spam-fullname,trash-fullname,confirmed-spam-fullname,confirmed-ham-fullname
"1","3","4","support@mycompany.com","Support","support@mycompany.com","secret","Support","support@mycompany.com","imap://imap.mycompany.com:143","","","secret","smtp://smtp.mycompany.com:25","",,,,,,,
"1","4","5","support@mycompany.com","Support","support@mycompany.com","secret","Support","support@mycompany.com","imap://imap.mycompany.com:143","","","secret","smtp://smtp.mycompany.com:25","",,,,,,,
"1","5","6","support@mycompany.com","Support","support@mycompany.com","secret","Support","support@mycompany.com","imap://imap.mycompany.com:143","","","secret","smtp://smtp.mycompany.com:25","",,,,,,,
```

Again, let's assume time has passed. The user with ID 4 has moved to another department and needs to be removed from the list of members of the secondary account. For that purpose, we use another CLT

```
./deletesecondaryaccount -A oxadmin -P secret -c 1 --users 4 -e support@mycompany.com

Secondary account with primary address asd@mycompany.com successfully deleted from specified users/groups in context 1
```

Again, we verify

```
./listsecondaryaccount -A oxadmin -P secret -c 1 --csv

context-id,user-id,account-id,primary-address,name,login,password,personal,reply-to,mail-server-URL,mail-starttls,transport-login,transport-password,transport-server-URL,transport-starttls,archive-fullname,drafts-fullname,sent-fullname,spam-fullname,trash-fullname,confirmed-spam-fullname,confirmed-ham-fullname
"1","3","4","support@mycompany.com","Support","support@mycompany.com","secret","Support","support@mycompany.com","imap://imap.mycompany.com:143","","","secret","smtp://smtp.mycompany.com:25","",,,,,,,
"1","5","6","support@mycompany.com","Support","support@mycompany.com","secret","Support","support@mycompany.com","imap://imap.mycompany.com:143","","","secret","smtp://smtp.mycompany.com:25","",,,,,,,
```
