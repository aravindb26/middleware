---
title: revokeoauthclient
icon: far fa-circle
tags: Administration, Command Line tools, OAuth
package: open-xchange-admin-oauth-provider
---

# NAME

revokeoauthclient - revoke permission for an OAuth client

# SYNOPSIS

**revokeoauthclient** [OPTIONS]

# DESCRIPTION

This command line tool revokes permissions for an specific OAuth client

# OPTIONS

**--id** *id*
: The id of the OAuth client

**-A**, **--adminuser** *masterAdminUser*
:   Master admin user name for authentication. Optional, depending on your configuration.

**-P**, **--adminpass** *masterAdminPassword*
:   Master admin password for authentication. Optional, depending on your configuration.

**-h**, **--help**
: Prints a help text

**--environment**
:   Show info about commandline environment.

**--nonl**
:   Remove all newlines (\\n) from output.

**--responsetimeout**
: The optional response timeout in seconds when reading data from server (default: 0s; infinite).

# EXAMPLES

**revokeoauthclient -A masteradmin -P secret -i 5**

Removes the OAuth clients

# SEE ALSO

[createoauthclient(1)](createoauthclient.html), [disableoauthclient(1)](disableoauthclient.html), [enableoauthclient(1)](enableoauthclient.html), [getoauthclient(1)](getoauthclient.html), [listoauthclient(1)](listoauthclient.html), [removeoauthclient(1)](removeoauthclient.html), [updateoauthclient(1)](updateoauthclient.html)