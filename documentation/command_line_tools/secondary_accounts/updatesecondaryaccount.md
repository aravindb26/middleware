---
title: updatesecondaryaccount
icon: far fa-circle
tags: Administration, Command Line tools
package: open-xchange-admin
---

# NAME

updatesecondaryaccount - publishes a secondary account to Open-Xchange Middleware

# SYNOPSIS

**updatesecondaryaccount** [-h|--help]

**updatesecondaryaccount** [-c *contextId*] [--users *userIds*] [--groups *groupIds*] -A *adminUser* -P *adminPassword*  [-e *primary-address*] [--login *login*] [--password *password*] [--name *name*] [--personal *personal*] [--reply-to *reply-to*] [--mail-server *mail-server*] [--mail-port *mail-port*] [--mail-protocol *mail-protocol*] [--mail-secure *mail-secure*] [--mail-starttls *mail-starttls*] [--transport-login *transport-login*] [--transport-password *transport-password*] [--transport-server *transport-server*] [--transport-port *transport-port*] [--transport-protocol *transport-protocol*] [--transport-secure *transport-secure*] [--transport-starttls *transport-starttls*] [--archive-fullname *archive-fullname*] [--drafts-fullname *drafts-fullname*] [--sent-fullname *sent-fullname*] [--spam-fullname *spam-fullname*] [--trash-fullname *trash-fullname*] [--confirmed-spam-fullname *confirmed-spam-fullname*] [--confirmed-ham-fullname *confirmed-ham-fullname*] [-p *RMI-Port*] [-s *RMI-Server*] [--responsetimeout *responseTimeout*]

# DESCRIPTION

This command line tool updates a secondary account for dedicated users/groups in a context.

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
: The primary address of the account that identifies the account to update

**--login** *login*
: The account login informaton; if not set login information is used as for primary account

**--password** *password*
: The account password or secret; if not set password information is used as for primary account

**--name** *name*
: The account name            

**--personal** *personal*
: The personal for the account's E-Mail address; e.g. Jane Doe *jane.doe@example.com*

**--reply-to** *reply-to*
: The reply-to address to use for the account

**--mail-server** *mail-server*
: The mail server host name or IP address

**--mail-port** *mail-port*
: The mail server port; e.g. 143

**--mail-protocol** *mail-protocol*
: The mail server protocol; e.g. imap

**--mail-secure** *mail-secure*
: Whether SSL is supposed to be used when connecting against mail server

**--mail-starttls** *mail-starttls*
: Whether STARTTLS is enforced when connecting plain against mail server

**--transport-login** *transport-login*
: The transport server login informaton (if any). If absent the mail server one is used

**--transport-password** *transport-password*
: The transport server password (if any). If absent the mail server one is used

**--transport-server** *transport-server*
: The transport server host name or IP address

**--transport-port** *transport-port*
: The transport server port; e.g. 25

**--transport-protocol** *transport-protocol*
: The transport server protocol; e.g. smtp

**--transport-secure** *transport-secure*
: Whether SSL is supposed to be used when connecting against transport server

**--transport-starttls** *transport-starttls*
: Whether STARTTLS is enforced when connecting plain against transport server

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
