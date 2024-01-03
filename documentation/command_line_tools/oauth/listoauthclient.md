---
title: listoauthclient
icon: far fa-circle
tags: Administration, Command Line tools, OAuth
package: open-xchange-admin-oauth-provider
---

# NAME

listoauthclient - List OAuth clients

# SYNOPSIS

**listoauthclient** [OPTIONS]

# DESCRIPTION

This command line tool lists OAuth clients

# OPTIONS

**--csv**
Format output as CSV

**-c**, **--context-group-id** *id*
: The id of the context group

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

**listoauthclient -A masteradmin -P secret --c 10 --csv**

Lists OAuth clients

# SEE ALSO

[createoauthclient(1)](createoauthclient.html), [disableoauthclient(1)](disableoauthclient.html), [enableoauthclient(1)](enableoauthclient.html), [getoauthclient(1)](getoauthclient.html), [removeoauthclient(1)](removeoauthclient.html), [revokeoauthclient(1)](revokeoauthclient.html), [updateoauthclient(1)](updateoauthclient.html)