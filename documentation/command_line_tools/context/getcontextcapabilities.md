---
title: getcontextcapabilities
icon: far fa-circle
tags: Administration, Command Line tools, Context, Capabilities
package: open-xchange-admin
---

# NAME

getcontextcapabilities - lists available capabilities for a certain context. 

# SYNOPSIS

**getcontextcapabilities** [OPTION]...

# DESCRIPTION

This command line tool lists available capabilities for a certain context. 

# OPTIONS

**-c**, **--contextid** *contextId*
: The context identifier. Mandatory and mutually exclusive with `-N`.

**-N**, **--contextname** *contextName*
: The context name. Mandatory and mutually exclusive with `-c`.

**-A**, **--adminuser** *masterAdmin*
: Master admin user name for authentication. Optional, depending on your configuration.

**-P**, **--adminpass** *masterAdminPassword*
: Master admin password for authentication. Optional, depending on your configuration.

**-h**, **--help**
: Prints a help text.

**--environment**
: Show info about commandline environment.

**--nonl**
: Remove all newlines (\\n) from output.

**--responsetimeout**
: The optional response timeout in seconds when reading data from server (default: 0s; infinite).

# EXAMPLES

**getcontextcapabilities -A masterAdmin -P secret -c 1138**

Retrieves the capabilities of the context with the specified identifier.

# SEE ALSO

[createcontext(1)](createcontext.html), [enablecontext(1)](enablecontext.html), [changecontext(1)](changecontext.html), [enableallcontexts(1)](enableallcontexts.html), [disableallcontexts(1)](disableallcontexts.html), [deletecontext(1)](deletecontext.html), [disablecontext(1)](disablecontext.html), [listcontext(1)](listcontext.html)
