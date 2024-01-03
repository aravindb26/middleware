---
title: disablecontext
icon: far fa-circle
tags: Administration, Command Line tools, Context
package: open-xchange-admin
---

# NAME

disablecontext - disables contexts.

# SYNOPSIS

**disablecontext** [OPTION]...

# DESCRIPTION

This command line tool lists disable contexts. Whenever a customer tries to log in to a disabled context, the login is denied.

# OPTIONS

**-c**, **--contextid** *contextId*
: The context identifier. Mandatory and mutually exclusive with `-N`.

**-N**, **--contextname** *contextName*
: The context name. Mandatory and mutually exclusive with `-c`.

**-A**, **--adminuser** *masterAdmin*
: Context admin user name for authentication. Optional, depending on your configuration.

**-P**, **--adminpass** *masterAdminPassword*
: Context admin password for authentication. Optional, depending on your configuration.

**--csv**
: Format output to csv.

**-h**, **--help**
: Prints a help text.

**--environment**
: Show info about commandline environment.

**--nonl**
: Remove all newlines (\\n) from output.

**--responsetimeout**
: The optional response timeout in seconds when reading data from server (default: 0s; infinite).

# EXAMPLES

**disablecontext -A masterAdmin -P secret -c 1138**

Disables the context with the specified identifier.

# SEE ALSO

[createcontext(1)](createcontext.html), [listcontext(1)](listcontext.html), [changecontext(1)](changecontext.html), [enablecontext(1)](enablecontext.html), [disableallcontexts(1)](disableallcontexts.html), [deletecontext(1)](deletecontext.html), [enableallcontexts(1)](enableallcontexts.html), [getcontextcapabilities(1)](getcontextcapabilities.html)