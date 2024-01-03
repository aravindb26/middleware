---
title: getmissingservices
icon: far fa-circle
tags: Administration, Command Line tools
package: open-xchange-core
---

# NAME

getmissingservices

# SYNOPSIS

**getmissingservices** [-n *bundleName*] [-p *RMI-Port*] [-s *RMI-Server*] [--responsetimeout *responseTimeout*] | [-h]

# DESCRIPTION

Gets the missing services

# OPTIONS

**-n**,**--name** *bundleName*
: The optional bundle's symbolic name

**-h**, **--help**
: Prints a help text

**-s**,**--server** *rmiHost*
: The optional RMI server (default: localhost)

**-p**,**--port** *rmiPort*
: The optional RMI port (default:1099)

**--responsetimeout**
: The optional response timeout in seconds when reading data from server (default: 0s; infinite).

