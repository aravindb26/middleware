---
title: existscontext
icon: far fa-circle
tags: Administration, Command Line tools, Context
package: open-xchange-admin
---

# NAME

existscontext - Check for context existence.

# SYNOPSIS

**deletecontext** [OPTION]...

# DESCRIPTION

This command line tool allows to check whether a context exists. It uses either the id or the name of the context 

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

**--inserver**
: Whether check should be limited to the registered server this provisioning node is running in

# EXAMPLES

**existscontext -A masterAdmin -P secret -c 1138**

Checks if the context with the specified identifier exists.

# SEE ALSO

[createcontext(1)](createcontext.html), [listcontext(1)](listcontext.html), [changecontext(1)](changecontext.html), [enablecontext(1)](enablecontext.html), [disablecontext(1)](disablecontext.html), [disableallcontexts(1)](disableallcontexts.html), [enableallcontexts(1)](enableallcontexts.html), [getcontextcapabilities(1)](getcontextcapabilities.html)