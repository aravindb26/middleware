---
title: disableoauthclient
icon: far fa-circle
tags: Administration, Command Line tools, OAuth
package: open-xchange-admin-oauth-provider
---

# NAME

disableoauthclient - disables an OAuth client

# SYNOPSIS

**disableoauthclient** [OPTIONS]

# DESCRIPTION

This command line tool disables an OAuth client

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

**disableoauthclient -A masteradmin -P secret --id 5**

Disables an OAuthClient

# SEE ALSO

[createoauthclient(1)](createoauthclient.html), [enableoauthclient(1)](enableoauthclient.html), [getoauthclient(1)](getoauthclient.html), [listoauthclient(1)](listoauthclient.html), [removeoauthclient(1)](removeoauthclient.html), [revokeoauthclient(1)](revokeoauthclient.html), [updateoauthclient(1)](updateoauthclient.html)