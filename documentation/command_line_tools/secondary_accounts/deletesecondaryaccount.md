---
title: deletesecondaryaccount
icon: far fa-circle
tags: Administration, Command Line tools
package: open-xchange-admin
---

# NAME

deletesecondaryaccount - lists all secondary accounts either context-wide or per user/group

# SYNOPSIS

**deletesecondaryaccount** [-h|--help]

**deletesecondaryaccount** [-c *contextId*] [--users *userIds*] [--groups *groupIds*] [-e, *primary-address*] -A *adminUser* -P *adminPassword*  [-p *RMI-Port*] [-s *RMI-Server*] [--responsetimeout *responseTimeout*]

# DESCRIPTION

This command line tool deletes all secondary accounts per user/group in a context.

# OPTIONS

**-c**, **--context** *contextId*
: The context identifier. Mandatory

**-e**, **--primary-address** *primary-address*
: The primary address that uniquely identifies the account to delete

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

**--responsetimeout**
: The optional response timeout in seconds when reading data from server (default: 0s; infinite).
