---
title: createsecondaryaccount
icon: far fa-circle
tags: Administration, Command Line tools
package: open-xchange-admin
---

# NAME

createsecondaryaccount - publishes a secondary account to Open-Xchange Middleware

# SYNOPSIS

**createsecondaryaccount** [-h|--help]

**createsecondaryaccount** [-c *contextId*] [--users *userIds*] [--groups *groupIds*] -A *adminUser* -P *adminPassword*  [-e *primary-address*] [--login *login*] [--password *password*] [--name *name*] [--personal *personal*] [--reply-to *reply-to*] [--mail-endpoint-source *mail-endpoint-source*] [--mail-server *mail-server*] [--mail-port *mail-port*] [--mail-protocol *mail-protocol*] [--mail-secure *mail-secure*] [--mail-starttls *mail-starttls*] [--transport-login *transport-login*] [--transport-password *transport-password*] [--transport-endpoint-source * transport-endpoint-source*] [--transport-server *transport-server*] [--transport-port *transport-port*] [--transport-protocol *transport-protocol*] [--transport-secure *transport-secure*] [--transport-starttls *transport-starttls*] [--archive-fullname *archive-fullname*] [--drafts-fullname *drafts-fullname*] [--sent-fullname *sent-fullname*] [--spam-fullname *spam-fullname*] [--trash-fullname *trash-fullname*] [--confirmed-spam-fullname *confirmed-spam-fullname*] [--confirmed-ham-fullname *confirmed-ham-fullname*] [-p *RMI-Port*] [-s *RMI-Server*] [--responsetimeout *responseTimeout*]

# DESCRIPTION

This command line tool publishes a secondary account to Open-Xchange Middleware for dedicated users/groups in a context.

**Note**: If transport settings are not specified, the account acts an mail-access-only account; meaning it is not capable to send, but only access mails. Same applies to standard folders. If not set, the standard folders are initially empty, but are automatically determined on first access to that mail account individually for each user.

# OPTIONS

**-c**, **--context** *contextId*
: The context identifier. Mandatory

**--users** *userIds*
: The user identifiers as a comma-separated list. Required if no 'groupIds' are set.

**--groups** *groupIds*
: The group identifiers as a comma-separated list. Required if no 'userIds' are set.

**-h**, **--help**
: Prints a help text

**-A**, **--adminuser** *admin*
: The context admin user name for authentication.

**-P**, **--adminpass** *adminPassword*
: The context admin password for authentication.

**-s**,**--server** *rmiHost*
: The optional RMI server (default: localhost)

**-p**,**--port** *rmiPort*
: The optional RMI port (default:1099)

**--responsetimeout** *timeout*
: The optional response timeout in seconds when reading data from server (default: 0s; infinite).

**-e**,**--primary-address** *primary-address*
: The primary address of the account

**--login** *login*
: The account login informaton

**--password** *password*
: The account password or secret; if not set password information is used as for primary account

**--name** *name*
: The account name            

**--personal** *personal*
: The personal for the account's E-Mail address; e.g. Jane Doe *jane.doe@example.com*

**--reply-to** *reply-to*
: The reply-to address to use for the account

**--mail-endpoint-source** *mail-endpoint-source*
: The mail end-point source; either primary, localhost or none. "primary" uses same end-point settings as primary account. "localhost" uses localhost as end-point. "none" to expect end-point data from appropriate options ('mail-server', 'mail-port', 'mail-protocol', 'mail-secure', and 'mail-starttls')

**--mail-server** *mail-server*
: The mail server host name or IP address; see also option 'mail-endpoint-source'

**--mail-port** *mail-port*
: The mail server port (e.g. 143); see also option 'mail-endpoint-source'

**--mail-protocol** *mail-protocol*
: The mail server protocol (e.g. imap); see also option 'mail-endpoint-source'

**--mail-secure** *mail-secure*
: Whether SSL is supposed to be used when connecting against mail server; see also option 'mail-endpoint-source'

**--mail-starttls** *mail-starttls*
: Whether STARTTLS is enforced when connecting plain against mail server; see also option 'mail-endpoint-source'

**--transport-login** *transport-login*
: The transport server login informaton (if any). If absent the mail server one is used

**--transport-password** *transport-password*
: The transport server password (if any). If absent the mail server one is used

**--transport-endpoint-source** *transport-endpoint-source*
: The transport end-point source; either primary, localhost or none. "primary" uses same end-point settings as primary account. "localhost" uses localhost as end-point. "none" to expect end-point data from appropriate options ('transport-server', 'transport-port', 'transport-protocol', 'transport-secure', and 'transport-starttls')

**--transport-server** *transport-server*
: The transport server host name or IP address; see also option 'transport-endpoint-source'

**--transport-port** *transport-port*
: The transport server port (e.g. 25); see also option 'transport-endpoint-source'

**--transport-protocol** *transport-protocol*
: The transport server protocol (e.g. smtp); see also option 'transport-endpoint-source'

**--transport-secure** *transport-secure*
: Whether SSL is supposed to be used when connecting against transport server; see also option 'transport-endpoint-source'

**--transport-starttls** *transport-starttls*
: Whether STARTTLS is enforced when connecting plain against transport server; see also option 'transport-endpoint-source'

**--archive-fullname** *archive-fullname*
: The full name for the standard archive folder

**--drafts-fullname** *drafts-fullname*
: The full name for the standard drafts folder

**--sent-fullname** *sent-fullname*
: The full name for the standard sent folder

**--spam-fullname** *spam-fullname*
: The full name for the standard spam folder

**--trash-fullname** *trash-fullname*
: The full name for the standard trash folder

**--confirmed-spam-fullname** *confirmed-spam-fullname*
: The full name for the standard confirmed-spam folder

**--confirmed-ham-fullname** *confirmed-ham-fullname*
: The full name for the standard confirmed-ham folder

# Examples

```
./createsecondaryaccount -A oxadmin -P secret -c 1 --users 3 --login info@mycompany.com --password secret
 -e info@mycompany.com --name Info --personal Info --reply-to info@mycompany.com 
 --mail-server imap.mycompany.com --mail-port 143 --mail-protocol imap --mail-secure false 
 --mail-starttls false --transport-server smtp.mycompany.com --transport-port 25 
 --transport-protocol smtp --transport-secure false --transport-starttls false
```

```
./createsecondaryaccount -A oxadmin -P secret -c 1 --users 3 --login info@mycompany.com --password secret
 -e info@mycompany.com --name Info --personal Info --reply-to info@mycompany.com 
 --mail-endpoint-source primary --transport-endpoint-source primary
```