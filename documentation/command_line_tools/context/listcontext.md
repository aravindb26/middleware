---
title: listcontext
icon: far fa-circle
tags: Administration, Command Line tools, Context
package: open-xchange-admin
---

# NAME

listcontext - lists all contexts.

# SYNOPSIS

**listcontext** [OPTION]...

# DESCRIPTION

This command line tool lists all contexts. Optionally, with the use of '-s' you can search for a context name that matches the specified search pattern.

# OPTIONS

**-s**, **--searchpattern** *searchPattern*
: The optional search pattern.

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

**--length**
: Limits the result size.

**--offset**
: The beginning offset of the result list.

# EXAMPLES

**listcontext -A masterAdmin -P secret**

Lists all contexts.

**listcontext -A masterAdmin -P secret -s foobar**

Lists all contexts that match the search pattern.

# SEE ALSO

[createcontext(1)](createcontext.html), [enablecontext(1)](enablecontext.html), [changecontext(1)](changecontext.html), [enableallcontexts(1)](enableallcontexts.html), [disableallcontexts(1)](disableallcontexts.html), [deletecontext(1)](deletecontext.html), [disablecontext(1)](disablecontext.html), [getcontextcapabilities(1)](getcontextcapabilities.html)
