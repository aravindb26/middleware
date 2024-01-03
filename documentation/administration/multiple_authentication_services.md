---
title: Running multiple authentication services
icon: fas fa-file-alt
tags: Administration, Authentication
---

# Introduction
Starting with version 8.0.0 of the Open Xchange Server it is possible to run multiple authentication services.

# Configuration
New properties have been introduced to configure a server with more than one authentication service. Those properties contains a placeholder
``[provider]`` that has to be replaced with the authentication service's identifier. Available identifier in core are ``database``, ``oidc``,
``imap``, ``ldap``, ``parallels`` and ``oauth``.

## Ranking
Each authentication service has a ranking, which can be configured via the property ``com.openexchange.authentication.[provider].ranking``
which defaults to ``0``. Authentication services are ranked from high to low to authenticate the user. If both services has an equal ranking, sorting
will be done by their identifier as second sort criteria. If an user cannot be authenticated, the authentication will be retried with the next service.

## Hostname
Each authentication service has an optional comma-separated list of refering hostnames, for which it is responsible. The list can be configured via the
property ``com.openexchange.authentication.[provider].hostnames``. By default this propery has no value.
When authentication a user and this property is set, the service checks if the user's refering hostname (e.g. ``example.org`` for the user trying to
log-in from ``https://example.org/appuite/ui``) is on this list. If the list does not contain the hostname this service is skipped and the
authentication will be tried with the next service.

## HTTP-Header
Each authentication service has optional expected headers and their values to determine if it is responsible for this authentication. The expected headers
can be configured via the property ``com.openexchange.authentication.[provider].customHeaders``. Headers can be configured in key-value notation
like ``headerName=headerValue`` or only ``headerName`` if the header does not have a value. Multiple headers can be configured as comma-separated list.
By default this property has no value.
When authentication a user and these properties are set, the service checks if the HTTP request contains headers with configured names and (optional) values.
If the request does not contain a matching header and value, this service is skipped and the authentication will be tried with the next service.

### Example for a valid configuration with multiple headers
```
com.openexchange.authentication.yourprovider.customHeaders=header1=headervalue1,headerWithoutValue,header2=headervalue2
```

## HTTP-Cookie
Each authentication service has optional expected cookies and their values to determine if it is responsible for this authentication. The expected cookies
can be configured via the property ``com.openexchange.authentication.[provider].customCookies``. Cookies are configured in key-value notation
like ``cookieName=cookieValue``. Multiple cookies can be configured as comma-separated list. By default this property has no value.
When authentication a user and these properties are set, the service checks if the HTTP request contains cookies with configured name and value.
If the request does not contain a matching cookie and value, this service is skipped and the authentication will be tried with the next service.

### Example for a valid configuration with multiple cookies
```
com.openexchange.authentication.yourprovider.customCookies=cookie1=cookievalue1,cookie2=cookievalue2
