---
title: Audit Logging
icon: fa-tasks
tags: Logging, Configuration, Installation
---

# How it works

Starting with v7.8.2 the Open-Xchange Server provides a special SLF4J logger named ``com.openexchange.log.audit.AuditLogService`` that currently tracks the following events

 - Login to Open-Xchange Server
 - Logout from Open-Xchange Server
 - Connect to internal/external IMAP
 - Connect to internal/external SMTP
 - Connect to internal/external POP3

The logger uses the regular Open-Xchange Server logback configuration. Then the logger outputs its log messages to ``stdout`` in the same way as configured for Open-Xchange Server through ``logback.xml`` configuration file.

# Installation

This feature is included in ``open-xchange-core`` package. Thus, no additional packages are required being installed.

# Configuration

An administrator is able to configure this feature through `/opt/open-xchange/etc/slf4j-auditlog.properties` file. The options are reloadable at any time.

## Enabling

The feature is enabled via [com.openexchange.log.audit.slf4j.enabled](/components/middleware/config{{ site.baseurl }}/index.html#com.openexchange.log.audit.slf4j.enabled) property, which defaults to ``false``. If set to ``true``the property [com.openexchange.log.audit.slf4j.level](/components/middleware/config{{ site.baseurl }}/index.html#com.openexchange.log.audit.slf4j.level) defines what log level to choose when outputting log messages (default is ``info``).

## Message layout

This section describes all properties that influence how a log message looks like. The log messages are logged in JSON format, the field ``msg`` contains the log
message. All audit log messages are decorated with the ``audit=true`` field.

 - [com.openexchange.log.audit.slf4j.delimiter](/components/middleware/config{{ site.baseurl }}/index.html#com.openexchange.log.audit.slf4j.delimiter) specifies the delimiter string to use between individual attributes in one log message; e.g. ``com.openexchange.log.audit.slf4j.delimiter="|"`` yields:

  ```
  {
    "timestamp" : 1640007913852,
    "level" : 6,
    "file" : "com.openexchange.log.audit.slf4j.Slf4jAuditLogService",
    "msg" : "imap.primary.login|login=alissa|ip=172.18.0.1|timestamp=2021-12-20T13:45:13Z|imap.login=alissa@metal.int|imap.server=localhost|imap.port=143",
    "audit" : true,
    "details" : {
      "com.openexchange.audit" : "true",
      "thread" : "Slf4jAuditLogService"
    }
  }
  ```

 - [com.openexchange.log.audit.slf4j.includeAttributeNames](/components/middleware/config{{ site.baseurl }}/index.html#com.openexchange.log.audit.slf4j.includeAttributeNames) defines whether attribute names are supposed to be included in log message or not; e.g. ``com.openexchange.log.audit.slf4j.includeAttributeNames=false``yields:

  ```
  {
    "timestamp" : 1640008202640,
    "level" : 6,
    "file" : "com.openexchange.log.audit.slf4j.Slf4jAuditLogService",
    "msg" : "imap.primary.login|alissa|172.18.0.1|2021-12-20T13:50:02Z|alissa@metal.int|localhost|143",
    "audit" : true,
    "details" : {
      "com.openexchange.audit" : "true",
      "thread" : "Slf4jAuditLogService"
    }
  }
  ```

 - [com.openexchange.log.audit.slf4j.date.pattern](/components/middleware/config{{ site.baseurl }}/index.html#com.openexchange.log.audit.slf4j.date.pattern) allows to specify in what format a date/time-stamp is logged. By default ISO-8601 formatting is used. The administrator is able to specify any date pattern according to [Date and Time Patterns](https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html). Accompanying options [com.openexchange.log.audit.slf4j.date.locale](/components/middleware/config{{ site.baseurl }}/index.html#com.openexchange.log.audit.slf4j.date.locale) and [com.openexchange.log.audit.slf4j.date.timezone](/components/middleware/config{{ site.baseurl }}/index.html#com.openexchange.log.audit.slf4j.date.timezone) allow to also set utilized locale and time zone for date formatting.
