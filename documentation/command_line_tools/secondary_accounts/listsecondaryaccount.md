---
title: listsecondaryaccount
icon: far fa-circle
tags: Administration, Command Line tools
package: open-xchange-admin
---

# NAME

listsecondaryaccount - lists all secondary accounts either context-wide or per user/group

# SYNOPSIS

**listsecondaryaccount** [-h|--help]

**listsecondaryaccount** [-c *contextId*] [--users *userIds*] [--groups *groupIds*] -A *adminUser* -P *adminPassword*  [-p *RMI-Port*] [-s *RMI-Server*] [--responsetimeout *responseTimeout*]

# DESCRIPTION

This command line tool lists all secondary accounts either context-wide or per user/group since `--users` and `groups` options are optional.

# OPTIONS

**-c**, **--context** *contextId*
: The context identifier. Mandatory

**--users** *userIds*
: The user identifiers as a comma-separated list.

**--groups** *groupIds*
: The group identifiers as a comma-separated list.

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

# EXAMPLES

**listsecondaryaccount -A masterAdmin -P secret -c 1138**

Listing all secondary accounts for a specific context.

**listsecondaryaccount -A masterAdmin -P secret -c 1138 --users 314**

Listing all secondary accounts for a specific user in a specific context.

**listsecondaryaccount -A masterAdmin -P secret -c 1138 --groups 4**

Listing all secondary accounts for a specific group in a specific context.