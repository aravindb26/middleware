---
title: listcontextsbyfilestore
icon: far fa-circle
tags: Administration, Command Line tools, Context, Filestore
package: open-xchange-admin
---

# NAME

listcontextsbyfilestore - lists all contexts that are assigned to a specific filestore.

# SYNOPSIS

**listcontextsbyfilestore** [OPTION]...

# DESCRIPTION

This command line tool lists all contexts that are assigned to a specific filestore. Optionally, with the use of '-s' you can search for a context name that matches the specified search pattern.

# OPTIONS

**-f**, **--filestore** *filestoreId*
: The filestore identifier. Mandatory and mutually exclusive with `-n`.

**-A**, **--adminuser** *masterAdmin*
: Master admin user name for authentication. Optional, depending on your configuration.

**-P**, **--adminpass** *masterAdminPassword*
: Master admin password for authentication. Optional, depending on your configuration.

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

**listcontextsbyfilestore -A masterAdmin -P secret -d 1138**

Lists all contexts in the specified filestore.

# SEE ALSO

[createcontext(1)](createcontext.html), [enablecontext(1)](enablecontext.html), [changecontext(1)](changecontext.html), [enableallcontexts(1)](enableallcontexts.html), [disableallcontexts(1)](disableallcontexts.html), [deletecontext(1)](deletecontext.html), [disablecontext(1)](disablecontext.html), [getcontextcapabilities(1)](getcontextcapabilities.html)
