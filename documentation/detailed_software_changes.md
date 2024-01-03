---
title: Detailed software changes
icon: fa fa-info-circle
classes: no-counting
---

This page contains detailed information about software changes.


[8.19]
======

3rd Party Libraries/License Change
----------------------------------

### SCR-1308
**Summary**: Update vulnerable 3rd party libraries

*Target platform:*
New libraries:

 * jackson-core-2.15.3.jar
 * jackson-annotations-2.15.3.jar
 * jackson-dataformat-xml-2.15.3.jar
 * jackson-databind-2.15.3.jar
 * jackson-dataformat-cbor-2.15.3.jar
 * jackson-dataformat-yaml-2.15.3.jar
 * jackson-datatype-jsr310-2.15.3.jar
 * jackson-datatype-jsr353-2.15.3.jar
 * jackson-jakarta-rs-base-2.15.3.jar
 * jackson-jakarta-rs-json-provider-2.15.3.jar
 * jackson-jakarta-rs-xml-provider-2.15.3.jar
 * jackson-module-jakarta-xmlbind-annotations-2.15.3.jar
 * jackson-module-jaxb-annotations-2.15.3.jar
 * jakarta.activation-api-2.1.2.jar
 * jakarta.json-api-2.1.2.jar
 * jackrabbit-webdav-2.21.19-custom.jar
 * snappy-java-1.1.10.3.jar
 * commons-fileupload-1.5.jar

Removed libraries:

 * jackson-core-2.14.2.jar
 * jackson-annotations-2.14.2.jar
 * jackson-dataformat-xml-2.14.2.jar
 * jackson-databind-2.14.2.jar
 * jackson-dataformat-cbor-2.14.2.jar
 * jackson-dataformat-yaml-2.14.2.jar
 * jackson-datatype-jsr310-2.14.2.jar
 * jackson-datatype-jsr353-2.14.2.jar
 * jackson-jakarta-rs-base-2.14.2.jar
 * jackson-jakarta-rs-json-provider-2.14.2.jar
 * jackson-jakarta-rs-xml-provider-2.14.2.jar
 * jackson-module-jakarta-xmlbind-annotations-2.14.2.jar
 * jackson-module-jaxb-annotations-2.14.2.jar
 * jakarta.activation-api-2.1.0.jar
 * jakarta.activation-2.0.1.jar
 * jackrabbit-webdav-2.19.1.jar
 * sqlite-jdbc-3.19.3.jar
 * commons-fileupload-1.4.jar

----
*Bundle com.squareup.okhttp3:*
New libraries:

 * kotlin-stdlib-common-1.9.10.jar
 * kotlin-stdlib-1.9.10.jar
 * okio-jvm-3.5.0.jar
 * okio-3.5.0.jar
 * okhttp-4.11.0.jar
 * logging-interceptor-4.11.0.jar

Removed libraries:

 * kotlin-stdlib-common-1.7.22.jar
 * kotlin-stdlib-1.7.22.jar
 * okio-jvm-2.8.0.jar
 * okhttp-4.9.3.jar
 * logging-interceptor-4.9.3.jar

----
*Bundle com.ctc.wstx:*
New library:

 * woodstox-core-6.5.1.jar

Removed library:

 * woodstox-core-6.5.0.jar

----
*Bundle org.yaml.snakeyaml:*
New library:

 * snakeyaml-2.2.jar

Removed library:

 * snakeyaml-1.33.jar

----
*Bundle com.nimbus:*
New libraries:

 * json-smart-2.4.11.jar
 * accessors-smart-2.4.11.jar

Removed libraries:

 * json-smart-2.4.8.jar
 * accessors-smart-2.4.8.jar

### SCR-1266
**Summary**: Upgraded gson from 2.9.0 to 2.10.1

Upgraded the gson library from 2.9.0 to 2.10.1



API - HTTP-API
--------------

### SCR-1304
**Summary**: Dropped `shard` query parameter from SAML request

Dropped `shard` query paramter from SAML request


Configuration
-------------

### SCR-1307
**Summary**: New property to configure allowed URI schemes for external calendar attachments

In order to prevent inaccessible attachment references getting stored for appointments imported to App Suite, a new lean configuration property is introduced. 

Its value can be configured to a comma-separated list of URI schemes that are allowed be stored for externally linked attachments of appointments. Attachments with other URI schemes will be rejected/ignored during import:


```
com.openexchange.calendar.allowedAttachmentSchemes=http,https,ftp,ftps
```

The property is reloadable, and can be defined through the config cascade sown to level "context".

### SCR-1303
**Summary**: Dropped sharding related property

Dropped property `com.openexchange.server.shardName`

### SCR-1277
**Summary**: New properties for Segmenter Client Service

For accessing a segmenter service in a _sharded_ environment with multiple data centers ("Active/Active"), a new configuration property is introduced where the base URI to the service can be defined (empty by default):

```
com.openexchange.segmenter.baseUrl=
```

Also, a new configuration property is introduced through which the identifier of the 'local' site can be defined, defaulting to the value `default`.

```
com.openexchange.segmenter.localSiteId=default
```

Both properties are _reloadable_. By default, if no segmenter service URI is defined, a non-sharded environment is assumed where all segments are served by the local site itself.



Packaging/Bundles
-----------------

### SCR-1315
**Summary**: Deprecation of Kerberos Authentication

The Kerberos authentication integration that was available via supplementary package `open-xchange-authentication-kerberos` is now deprecated and subject for removal in a future release.

### SCR-1312
**Summary**: Removed obsolete bundle `com.openexchange.message.timeline`

As it is no longer used, bundle `com.openexchange.message.timeline` is removed, along with its reference in `open-xchange-core` package.

### SCR-1311
**Summary**: Removed obsolete Rhino Scripting 

As they're no longer used, the following bundles are removed, along with their references in `open-xchange-halo` package:

* `com.openexchange.scripting.rhino`
* `com.openexchange.scripting.rhino.apiBridge`

### SCR-1241
**Summary**: Added new bundles for the request analyzer feature

The following new bundles are added to `open-xchange-core` in order to support request routing in _sharded_ environments with multiple data centers ("Active/Active"):

* `com.openexchange.request.analyzer`
* `com.openexchange.request.analyzer.rest`
* `com.openexchange.segmenter.client`


[8.18]
======

3rd Party Libraries/License Change
----------------------------------

### SCR-1286
**Summary**: Updated lettuce library from v6.2.5 to v6.2.6

Updated lettuce library from v6.2.5 to v6.2.6 in bundle `io.lettuce`

### SCR-1285
**Summary**: Updated Netty NIO libraries from v4.1.94 to v4.1.97

Updated Netty NIO libraries from v4.1.94 to v4.1.97 in bundle `io.netty`



API - HTTP-API
--------------

### SCR-1300
**Summary**: Remove templating as valid format option

The publication and OXMF-based subscriptions features were removed with 7.10.2, see also MW-1089. Now, we remove a leftover within the API. The

`&format=template`

API parameter is no longer supported and will result in an error if used. 

### SCR-1297
**Summary**: Deprecate transport "websocket" in "pns" API

To get rid of the stateful socket between Frontend and App Suite MW, Switchboard will be the only service that maintains a socket connection to clients. Instead of pushing directly from MW to the Client, MW will just use a HTTP webhook of Switchboard to announce new events. Switchboard will then push to the client. 

Therefore the `websocket` transport identifier as used in actions `subscribe` and `unsubscribe` of the `pns` module in the HTTP API is now deprecated and will finally be removed in a future version.



Behavioral Changes
------------------

### SCR-1272
**Summary**: Convert mail user flags to UTF-8

Mail user flags are persisted in UTF-7 on the mail server. However, web clients like the App Suite UI do use UTF-8 as default encoding for strings in communication with the Middleware.

Instead of using user flags as-is, the Middleware now converts incoming or outgoing user flags as need, so web clients can use UTF-8 based strings for mail user flags as usual.



Configuration
-------------

### SCR-1301
**Summary**: Remove properties regarding user templating

With 7.10.2, we removed the publications and OXMF-based subscriptions features, see MW-1089. 

Now the last pieces of code belonging to those features were removed. Along the code, two properties that aren't needed anymore, have been removed:

```
com.openexchange.templating.trusted 
com.openexchange.templating.usertemplating
```

### SCR-1278
**Summary**: Added configuration option to enable/disable encoding of IMAP user flags

Added configuration option controlling whether IMAP user flags are supposed to be encoded using RFC2060's UTF-7 encoding. Thus allowing non-ascii strings being stored as user flags.

Added support for properties:


* `"com.openexchange.imap.useUTF7ForUserFlags"`
  Enables (or disables) whether IMAP user flags are supposed to be encoded/decoded using RFC2060's UTF-7 encoding. Default value is `"false"`. Config-cascade aware.


* `"com.openexchange.imap.primary.useUTF7ForUserFlags"`
  Enables (or disables) whether IMAP user flags are supposed to be encoded/decoded only for the primary IMAP account using RFC2060's UTF-7 encoding. Default value is `"false"`. Config-cascade aware. This property effectively overwrites `"com.openexchange.imap.encodeUserFlagsAsUTF7"` for primary IMAP accounts

### SCR-1229
**Summary**: Introduced new properties for Webhooks support

Introduced new lean properties for Webhooks support.

### Webhook properties


* `com.openexchange.webhooks.enabledIds`
  Specifies a comma-separated list of Webhook identifiers that are considered as enabled. Reloadable and config-cascade aware.

### Webhook PNS properties


* `com.openexchange.pns.transport.webhooks.enabled`
  Specifies whether the Webhook transport is enabled. Reloadable and config-cascade aware.


* `com.openexchange.pns.transport.webhooks.httpsOnly`
  Whether only HTTPS is accepted when communicating with a Webhook. Reloadable and config-cascade aware.


* `com.openexchange.pns.transport.webhooks.allowTrustAll`
  Whether SSL configuration for "trust all" is allowed. If set to "false" only valid certificates are accepted when communicating with a Webhook using a secure connection. Neither reloadable nor config-cascade aware.


* `com.openexchange.pns.transport.webhooks.allowLocalWebhooks`
  Whether Webhooks having end-point set to an internal address are allowed. Neither reloadable nor config-cascade aware.

### Webhook PNS HTTP properties


* `com.openexchange.pns.transport.webhooks.http.maxConnections`
  The number of total connections held in HTTP connection pool for communicating with a certain Webhook end-point. Reloadable and config-cascade aware.


* `com.openexchange.pns.transport.webhooks.http.maxConnectionsPerHost`
  The number of connections per route held in HTTP connection pool for communicating with a certain Webhook end-point. Reloadable and config-cascade aware.


* `com.openexchange.pns.transport.webhooks.http.connectionTimeout`
  Specifies the timeout in milliseconds until a connection is established to a certain Webhook end-point. Reloadable and config-cascade aware.


* `com.openexchange.pns.transport.webhooks.http.socketReadTimeout`
  Specifies the socket timeout in milliseconds, which is the timeout for waiting for data when communicating with a certain Webhook end-point.. Reloadable and config-cascade aware.

### Webhook configuration file

Added new configuration file `webhooks.yml` containing the static configurations for known Webhook end-points. That file is in YAML notation and expects the following structure


```
 <unique-identifier>:
     uri: <URI>
       String. The URI end-point of the Webhook. May be overridden during subscribe depending on "uriValidationMode".

     uriValidationMode: <uri-validation-mode>
       String. Specifies how the possible client-specified URI for a Webhook end-point is supposed to be validated against the URI
               from configured Webhook end-point. Possible values: `none`, `prefix`, and `exact`. For `none` no requirements given.
               Any client-specified URI is accepted. For `prefix` he client-specified and configured URI for a Webhook end-point are
               required to start with same prefix. For `exact` the client-specified and configured URI for a Webhook end-point are
               required to be exactly the same. `prefix` is default.

     webhookSecret: <webhook-secret>
       String. The value for the "Authorization" HTTP header to pass on calling Webhook's URI. May be overridden during subscribe.

     login: <login>
       String. The login part for HTTP Basic Authentication if no value for the "Authorization" HTTP header is specified. May be overridden during subscribe.

     password: <password>
       String. The password part for HTTP Basic Authentication if no value for the "Authorization" HTTP header is specified. May be overridden during subscribe.

     signatureSecret: <signature-secret>
       String. Specifies shared secret known by caller and Webhook host. Used for signing.

     version: <version>
       Integer. Specifies the version of the Webhook. Used for signing.

     signatureHeaderName: <signature-header-name>
       String. Specifies the name of the signature header that carries the signature.

     maxTimeToLiveMillis: <max-time-to-live>
       Number. The max. time to live in milliseconds for the Webhook before considered as expired. If absent Webhook "lives" forever.

     maxNumberOfSubscriptionsPerUser: <max-number-per-user>
       Number. The max. number of subscriptions for this Webhook allowed for a single user. Equal or less than 0 (zero) means infinite.

     allowSharedUri: <allow-shared-uri>
       Boolean. Whether the same URI can be used by multiple different users or not. Optional, defaults to `true`.


```

#### Example

`webhooks.yml`

```
mywebhook:
    uri: https://my.endpoint.com:8080/webhook/event
    webhookSecret: supersecret
    signatureSecret: da39a3ee5e6b4b
    version: 1
    signatureHeaderName: X-OX-Signature
    maxTimeToLiveMillis: 2678400000
    maxNumberOfSubscriptionsPerUser: 2
    uriValidationMode: prefix

```



Database
--------

### SCR-1296
**Summary**: Changed column 'propertyValue' of table 'subadmin_config_properties' to be of type TEXT

Modified Config-DB to have column 'propertyValue' of table 'subadmin_config_properties' to be of type TEXT

New table layout is therefore:


```
CREATE TABLE subadmin_config_properties (
	sid INT4 UNSIGNED NOT NULL,
	propertyKey VARCHAR(64) CHARACTER SET latin1 NOT NULL DEFAULT '',
	propertyValue TEXT CHARACTER SET latin1 NOT NULL DEFAULT '',
	PRIMARY KEY (sid, propertyKey)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;

```

This database change contains no update task since this is a modification of the Config-DB, which is performed through liquibase framework on node start-up

### SCR-1258
**Summary**: Added column `meta` to table `pns_subscription`


<warning>
**Update Task**
com.openexchange.pns.subscription.storage.groupware.PnsSubscriptionsAddMetaColumTask
</warning>


Added `TEXT` column `meta` to table `pns_subscription`:


```
meta TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL

```



Packaging/Bundles
-----------------

### SCR-1294
**Summary**: Added new bundle for Config-Cascade implementation

Added new bundle `com.openexchange.config.cascade.impl` containing Config-Cascade implementation. This separates the API classes from actual implementation and allows less dependencies. That new bundle is added to `open-xchange-core` package

### SCR-1228
**Summary**: New bundles for Webhooks support

Introduced new bundles for Webhooks support


* `com.openexchange.webhooks`
* `com.openexchange.pns.transport.webhooks`




[8.17]
======

3rd Party Libraries/License Change
----------------------------------

### SCR-1275
**Summary**: Upgraded MySQL Connector for Java

Upgraded MySQL Connector for Java from v8.0.29 to v8.0.33 in OSGi  target platform

### SCR-1270
**Summary**: Updated Google Client API libraries

Updated Google Client API libraries

* `google-api-client-1.35.1.jar` to `google-api-client-2.2.0.jar`
* `google-api-client-appengine-1.35.1.jar` to `google-api-client-appengine-2.2.0.jar`
* `google-api-client-gson-1.35.1.jar` to `google-api-client-gson-2.2.0.jar`
* `google-api-client-jackson2-1.35.1.jar` to `google-api-client-jackson2-2.2.0.jar`
* `google-api-client-protobuf-1.35.1.jar` to `google-api-client-protobuf-2.2.0.jar`
* `google-api-client-servlet-1.35.1.jar` to `google-api-client-servlet-2.2.0.jar`
* `google-api-client-xml-1.35.1.jar` to `google-api-client-xml-2.2.0.jar`

* `google-api-services-calendar-v3-rev20220520-1.32.1.jar` to `google-api-services-calendar-v3-rev20230602-2.0.0.jar`
* `google-api-services-drive-v3-rev20220508-1.32.1.jar` to `google-api-services-drive-v3-rev20230610-2.0.0.jar`
* `google-api-services-gmail-v1-rev20220404-1.32.1.jar` to `google-api-services-gmail-v1-rev20230612-2.0.0.jar`
* `google-api-services-oauth2-v2-rev20200213-1.32.1.jar` to `google-api-services-oauth2-v2-rev20200213-2.0.0.jar`
* `google-api-services-people-v1-rev20220531-1.32.1.jar` to `google-api-services-people-v1-rev20230103-2.0.0.jar`



API - HTTP-API
--------------

### SCR-1232
**Summary**: Extended `updateAttendee` call with `tranps` parameter

To allow per-attendee transparency for a certain event, the HTTP API call `updateAttedee` was extended by the optional parameter `transp`.
Allowed values for the new parameter are:

```
TRANSPARENT
OPAQUE
```

If the transparency is set for a certain attendee, the event transparency for the corresponding event is adjusted implicitly, including all other chronos related calls.


Database
--------

### SCR-1264
**Summary**: Update task to insert missing references into 'filestore2user' table


<warning>
**Update Task**
com.openexchange.groupware.update.tasks.Filestore2UserUpdateReferencesTask
</warning>


To ensure the table `filestore2user` (in config-db) holds all references to users with individual filestores in a groupware schema , a new update task named `com.openexchange.groupware.update.tasks.Filestore2UserUpdateReferencesTask` is introduced.

API - SOAP
--------------

### SCR-1280
**Summary**: Added possibility to manage user sessions through SOAP interface

Added the possibility to manage user sessions through the new OXSessionService SOAP interface

Packaging/Bundles
--------------

### SCR-1281
**Summary**: Added new bundle/package to manage sessions via SOAP

Added new bundle `com.openexchange.sessiond.soap` to manage sessions via SOAP. That new bundle is contained in newly introduced package `open-xchange-sessiond-soap`

[8.16]
======

General
-------

### SCR-1252
**Summary**: Updated Netty NIO libraries

Updated Netty NIO libraries from v4.1.89 to v4.1.94 in bundle `io.netty`



3rd Party Libraries/License Change
----------------------------------

### SCR-1256
**Summary**: Upgraded Javassist Library

Library Javasisst is upgraded to v3.29.2-GA in target platform `com.openexchange.bundles` and bundle `com.openexchange.test`.

### SCR-1255
**Summary**: Updated Apache Tika library

Updated Apache Tika library from v2.6.0 to v2.8.0 in bundle `com.openexchange.tika.util`

### SCR-1253
**Summary**: Updated lettuce library

Updated lettuce library from v6.2.3 to v6.2.5 in bundle `io.lettuce`

### SCR-1247
**Summary**: Updated pushy library from v0.15.1 to v0.15.2

Updated pushy library from v0.15.1 to v0.15.2 in bundle `com.eatthepath.pushy`

### SCR-1245
**Summary**: Updated metadata-extractor from v2.17.0 to v2.18.0

Updated 3rd party library `metadata-extractor` from v2.17.0 to v2.18.0 in bundle `com.drew`

### SCR-1244
**Summary**: Updated htmlcleaner from v2.22 to v2.29

Updated 3rd party library `htmlcleaner` from v2.22 to v2.29 in target platform

### SCR-1243
**Summary**: Updated dnsjava from v3.5.1 to v3.5.2

Updated 3rd party library `dnsjava` from v3.5.1 to v3.5.2 in target platform

### SCR-1242
**Summary**: Updated Apache HttpCore and HttpClient libraries

Updated Apache HttpCore and HttpClient libraries

* Updated HttpCore from v4.4.15 to v4.4.16
* Updated HttpClient from v4.5.13 to v4.5.14

### SCR-1234
**Summary**: Updated Hazelcast Core Module

Updated Hazelcast Core Module from v5.2.1 to v5.3.1

### SCR-1231
**Summary**: Updated OSGi target platform bundles

### Updated OSGi target platform bundles

* `org.eclipse.osgi.services_3.10.200.v20210723-0643.jar` updated to `org.eclipse.osgi.services_3.11.100.v20221006-1531.jar`
* `org.eclipse.osgi.util_3.6.100.v20210723-1119.jar` updated to `org.eclipse.osgi.util_3.7.200.v20230103-1101.jar`
* `org.eclipse.osgi_3.18.0.v20220516-2155.jar` updated to `org.eclipse.osgi_3.18.400.v20230509-2241.jar`

### Added new OSGi bundles to target platform

Since content of shipped `org.eclipse.osgi.services` bundle has been changed. Missing classes/interfaces are now contained in separate OSGi bundles.

* Added `org.osgi.annotation.bundle_2.0.0.202202082230.jar`
* Added `org.osgi.annotation.versioning_1.1.2.202109301733.jar`
* Added `org.osgi.service.cm_1.6.1.202109301733.jar`
* Added `org.osgi.service.component_1.5.1.202212101352.jar`
* Added `org.osgi.service.component.annotations_1.5.1.202212101352.jar`
* Added `org.osgi.service.device_1.1.1.202109301733.jar`
* Added `org.osgi.service.event_1.4.1.202109301733.jar`
* Added `org.osgi.service.metatype_1.4.1.202109301733.jar`
* Added `org.osgi.service.metatype.annotations_1.4.1.202109301733.jar`
* Added `org.osgi.service.prefs_1.1.2.202109301733.jar`
* Added `org.osgi.service.provisioning_1.2.0.201505202024.jar`
* Added `org.osgi.service.repository_1.1.0.201505202024.jar`
* Added `org.osgi.service.upnp_1.2.1.202109301733.jar`
* Added `org.osgi.service.useradmin_1.1.1.202109301733.jar`
* Added `org.osgi.service.wireadmin_1.0.2.202109301733.jar`
* Added `org.osgi.util.function_1.2.0.202109301733.jar`
* Added `org.osgi.util.measurement_1.0.2.201802012109.jar`
* Added `org.osgi.util.position_1.0.1.201505202026.jar`
* Added `org.osgi.util.promise_1.3.0.202212101352.jar`
* Added `org.osgi.util.xml_1.0.2.202109301733.jar`




API - HTTP-API
--------------

### SCR-1235
**Summary**: Introduced a new action to the 'mail' module for exporting mails as PDFs

Introduced the action `export_PDF` to the `mail` module.

It is a `PUT` request and has the following URL parameters:

* `folder`: defines the mail folder which holds the mail that shall be exported
* `id`: defines the mail id

The request also accepts a mandatory JSON body with the following attributes:

* `folder_id`: Defines the drive folder in which the exported PDF/A document will be saved. This option is required.
* `pageFormat`: Defines the page format of the export document. It can either be `a4` (which is the default behaviour) or `letter`. This option is not required. If absent, the page format will be derived from the user's locale setting (for `us` or `ca` the page format will be `letter` and for anything else `a4`).
* `preferRichText`: If this option is enabled then, if an e-mail message contains both text and HTML versions of the body, then the latter is preferred and converted to a PDF/A document before it is appended to the exported PDF/A document. If only the text version is available, and the option is enabled, then the text version is converted to a PDF/A document and appended to the exported PDF/A document. This option is not required and by default is set to `true`.
* `includeExternalImages`: If this option is enabled then, and the e-mail contains any external inline images, then those images will be fetched from their respective sources and included to the exported PDF/A document at their supposed positions. This option is not required and is by default `false`.
* `appendAttachmentPreviews`: If this option is enabled, then any previewable attachment (i.e., documents and pictures) is converted from their original format, e.g., from docx or tiff, to a PDF/A document and is appended as one or more pages to the exported PDF/A document. This option is not required and is `false` by default.
* `embedAttachmentPreviews`: If this option is enabled, then any previewable attachment is converted from their original format to a PDF/A document and is embedded as an attachment to the exported PDF/A document. This option is not required and is `false` by default.
* `embedRawAttachments`: If this option is enabled, then all attachments are embedded without further processing to the exported PDF/A document as attachments. This option is not required and is `false` by default.
* `embedNonConvertibleAttachments`: If this option is enabled, then all attachments (previewable and non-previewable, i.e., zips, mp4s, etc.) are embedded without further processing to the exported PDF/A document as attachments. This option is not required and is `false` by default.



Configuration
-------------

### SCR-1240
**Summary**: Introduced a new capability to activate the PDF MailExportService

Introduced the capability `mail_export_pdf` to activate the PDF MailExportService.

### SCR-1239
**Summary**: Introduced new properties for the CollaboraPDFAConverter

Introduced the following properties to configure the \`CollaboraPDFAConverter`:

* `com.openexchange.mail.exportpdf.pdfa.collabora.enabled`: Defines whether the collabora online converter is enabled. Defaults to false
* `com.openexchange.mail.exportpdf.pdfa.collabora.url`: The Collabora URL to use: Allows to specify a dedicated Collabora service only for PDFA creation. By default is empty and uses the server configured via the property \`com.openexchange.mail.exportpdf.collabora.url`.

### SCR-1238
**Summary**: Introduced new properties for the GotenbergMailExportConverter

Introduced the following properties to configure the `GotenbergMailExportConverter`:

* `com.openexchange.mail.exportpdf.gotenberg.enabled`: Defines whether the gotenberg online converter is enabled. Defaults to false
* `com.openexchange.mail.exportpdf.gotenberg.url`: Defines the base URL of the Gotenberg Online server. Defaults to `http://localhost:3000`
* `com.openexchange.mail.exportpdf.gotenberg.fileExtensions`: Defines a comma separated list of file extensions that are handled by the gotenberg converter. Defaults to `htm, html`.
* `com.openexchange.mail.exportpdf.gotenberg.pdfFormat`: Specifies which PDF format to use. "PDF/A-1a", "PDF/A-2b" and "PDF/A-3b" are supported formats, or "PDF" for regular PDF. Defaults to "PDF"

### SCR-1237
**Summary**: Introduced new properties for the CollaboraMailExportConverter

Introduced the following properties to configure the `CollaboraMailExportConverter`:

* `com.openexchange.mail.exportpdf.collabora.enabled`: Defines whether the collabora online converter is enabled. Defaults to false
* `com.openexchange.mail.exportpdf.collabora.url`: Defines the base URL of the Collabora Online server. Defaults to `http://localhost:9980`
* `com.openexchange.mail.exportpdf.collabora.fileExtensions`: Defines a comma separated list of file extensions that are handled by the collabora converter. Defaults to `sxw, odt, fodt, sxc, ods, fods, sxi, odp, fodp, sxd, odg, fodg, odc, sxg, odm, stw, ott, otm, stc, ots, sti, otp std, otg, odb, oxt, doc, dot xls, ppt, docx, docm, dotx, dotm, xltx, xltm, xlsx, xlsb, xlsm, pptx, pptm, potx, potm, wpd, pdb, hwp, wps, wri, wk1, cgm, dxf, emf, wmf, cdr, vsd, pub, vss, lrf, gnumeric, mw, numbers, p65, pdf, jpg, jpeg, gif, png, dif, slk, csv, dbf, oth, rtf, txt, html, htm, xml`.
* `com.openexchange.mail.exportpdf.collabora.imageReplacementMode`: Defines the mode on how to handle/replace inline images. Defaults to `distributedFile`.

### SCR-1236
**Summary**: Introduced new properties for the MailExportService

Introduced the following properties to configure the `MailExportService`:

* `com.openexchange.mail.exportpdf.concurrentExports`: Defines the maximum concurrent mail exports that the server is allowed to process. If the limit is reached an error will be returned to the client, advising it to retry again in a while. Defaults to 10. 
* `com.openexchange.mail.exportpdf.pageMarginTop`:  Defines the top margin (in millimeters) of the exported pages. Defaults to 12.7 millimeters (0.5 inches).
* `com.openexchange.mail.exportpdf.pageMarginBottom`:  Defines the bottom margin (in millimeters) of the exported pages. Defaults to 12.7 millimeters (0.5 inches).
* `com.openexchange.mail.exportpdf.pageMarginLeft`:  Defines the left margin (in millimeters) of the exported pages. Defaults to 12.7 millimeters (0.5 inches).
* `com.openexchange.mail.exportpdf.pageMarginRight`:  Defines the right margin (in millimeters) of the exported pages. Defaults to 12.7 millimeters (0.5 inches).
* `com.openexchange.mail.exportpdf.headerFontSize`: Defines the font size of the exported mail's headers. Defaults to 12 points.
* `com.openexchange.mail.exportpdf.bodyFontSize`: Defines the font size of the exported mail's body. Defaults to 12 points.
* `com.openexchange.mail.exportpdf.autoPageOrientation`: Defines whether PDF pages will be auto-oriented in landscape mode whenever a full page appended image is in landscape mode. Defaults to false



Database
--------

### SCR-1233
**Summary**: Update encryption for passwords of anonymous guest users

<warning>
**Update Task**
com.openexchange.groupware.update.tasks.RecryptGuestUserPasswords</p>
</warning>

Update encryption for anonymous guest user passwords using newly introduced mechanisms with implicit salt

Table `user` altered, extend column `userPassword` from `VARCHAR(128)` to `VARCHAR(512)`


[8.15]
======

General
-------

### SCR-1227
**Summary**: Enhanced existent SOAP end-points by standard "Scheduled" folder

Enhanced existent SOAP end-points by standard "Scheduled" folder. The folder that holds such E-Mails that are scheduled for being sent at a later time.

To do so, the `"User"` data object contained in several SOAP end-points has been extended by `"mail_folder_scheduled_full_name"` element to output/specify the standard "Scheduled" folder.


### SCR-1201
**Summary**: Added separate bundle offering HTTP liveness end-point

Added separate bundle `com.openexchange.http.liveness` part of `open-xchange-core` package list that offers the HTTP liveness end-point at configured HTTP host name (default "127.0.0.1") and liveness port (default 8016).


Configuration
-------------

### SCR-1224
**Summary**: Add property `com.openexchange.log.extensionHttpHeaders`

`com.openexchange.log.extensionHttpHeaders` defines a comma separated list of HTTP headers that shall additionally be logged for incoming requests

Example
```
com.openexchange.log.extensionHttpHeaders=X-custom-Header,X-host
```

The property is neither reloadable nor ConfigCascade-aware.


Database
--------

### SCR-1225
**Summary**: Added new tables for scheduled mail feature

Added new tables in user database for scheduled mail feature


```

CREATE TABLE scheduledMail(
 uuid BINARY(16) NOT NULL,
 cid INT4 unsigned NOT NULL,
 user INT4 unsigned NOT NULL,
 dateToSend BIGINT(64) unsigned NOT NULL,
 mailPath TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
 processing BIGINT(64) unsigned NOT NULL DEFAULT 0,
 meta TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
 PRIMARY KEY (uuid),
 KEY id (cid, user, uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci

```



```

CREATE TABLE scheduledMailLock(
 cid INT4 unsigned NOT NULL DEFAULT 0,
 user INT4 unsigned NOT NULL DEFAULT 0,
 name VARCHAR(16) CHARACTER SET latin1 COLLATE latin1_general_ci NOT NULL,
 stamp BIGINT(64) unsigned NOT NULL,
 PRIMARY KEY (cid, user, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci

```


### SCR-1223
**Summary**: Update task to add the "claim" column to "calendar_alarm_trigger" table


<warning>
**Update Task**
com.openexchange.chronos.storage.rdb.groupware.CalendarAlarmTriggerAddClaimColumnTask</p>
</warning>


Adds the "claim" column to "calendar_alarm_trigger" table


[8.14]
======

3rd Party Libraries/License Change
----------------------------------

### SCR-1219
**Summary**: Upgraded JSoup library

Upgraded JSoup library in target platform (`con.openexchange.bundles`) from v1.15.3 to v1.16.1



API - HTTP-API
--------------

### SCR-1216
**Summary**: Accept parameter `harddelete` for composition space's delete end-point

Accept boolean parameter `"harddelete"` for composition space's delete end-point


```
DELETE /mailcompose/draft.xyz?harddelete=true
```


If set to `"true"` any associated draft message for the denoted composition space gets hard-deleted. That is no copy is created in standard trash folder.

### SCR-1213
**Summary**: New Flag `all_others_declined` for Events

The "flags" enumeration for events in `chronos` module of the HTTP API is extended by the value `all_others_declined`. If set, all other individual attendees in the event have a participation status of *declined*, which could be used by clients to show a hint that one might be alone in a meeting.

See [https://documentation.open-xchange.com/latest/middleware/calendar/implementation_details.html#event-flags] for further details.

### SCR-1198
**Summary**: New Settings for Free/Busy Visibility in JSlob

The `io.ox/calendar` JSlob entry is extended by the following item which indicates the free/busy visibility of the user:

```
{
  "id": "io.ox/calendar",
  "tree": {
    "chronos": {
      "freeBusyVisibility": "all",
    }
  },
  "meta": {
    "chronos": {
      "freeBusyVisibility": {
        "possibleValues": [
          "all",
          "internal-only",
          "none"
        ],
        "configurable": true
      }
    }
  }
}
```

Within the `meta` section, clients are able to derive the possible values - the enumeration will only yield the value `internal-only` if cross-context features are available. Also, the "configurable" flag will indicate whether the property is settable by the user or not.



Configuration
-------------

### SCR-1220
**Summary**: Introduce new properties for DAV client matching

Once in a while, vendors like Apple decide to change the User Agents of their products in a way, we don't recognize the matching clients anymore. Thus, the Open Xchange server isn't able to apply special handling for those clients. This leads to subsequent problems and errors.

Until 8.14, the user agent matching was a static, programmatically pre-defined process. For every user agent's change, there needed to be a patch applied. Now, the mechanism is replaced by a more dynamically approach:

Administrators can define regular expressions for the known \*DAV clients. In detail, the following properties are added for known \*DAV clients:



```
com.openexchange.dav.useragent.mac_calendar
com.openexchange.dav.useragent.mac_contacts
com.openexchange.dav.useragent.ios
com.openexchange.dav.useragent.ios_reminders
com.openexchange.dav.useragent.thunderbird_lightning
com.openexchange.dav.useragent.thunderbird_cardbook
com.openexchange.dav.useragent.em_client
com.openexchange.dav.useragent.ox_sync
com.openexchange.dav.useragent.caldav_sync
com.openexchange.dav.useragent.carddav_sync
com.openexchange.dav.useragent.smooth_sync
com.openexchange.dav.useragent.davdroid
com.openexchange.dav.useragent.davx5
com.openexchange.dav.useragent.outlook_caldav_synchronizer
com.openexchange.dav.useragent.windows_phone
com.openexchange.dav.useragent.windwos
```

All properties have pre-defined default values and are reloadable.

### SCR-1218
**Summary**: New config option for sanitizing CSV cell content on contact export

New lean config option `"com.openexchange.export.csv.sanitize"` for sanitizing CSV cell content on contact export. Default is `false`. Reloadable, but not config-cascade aware

### SCR-1217
**Summary**: New property to limit number of considered filestore candidates

New lean property `com.openexchange.admin.limitFilestoreCandidates` to limit number of considered filestore candidates to a reasonable amount when determining the filestore to use for a new context/user. Neither reloadable, nor config-cascade aware. Default is `100`.

### SCR-1215
**Summary**: Accept specifying a max. running time that must not be exceeded by execution of an individual health check

Accept new lean property for specifying a max. running time that must not be exceeded by execution of an individual health check

* `com.openexchange.health.maxRunningTimeSeconds`
  The max. allowed running time in seconds for an individual health check. It a check's execution is canceled if it exceeds that running time. A value of equal to or less than zero 0 (zero) ignores this setting. Default is 5. Reloadbale, but not config-cascade aware.

### SCR-1197
**Summary**: New Properties for Free/Busy Visibility

In order to define the default free/busy visibility setting of users, and to control whether it is changeable by end users, the following new lean configuration properties are introduced:

 * `com.openexchange.calendar.freeBusyVisibility.default=all`: Defines the default free/busy visibility setting to assume unless overridden by the user. Possible values are:
     * `none` to not expose a user's availability to others at all
     * `internal-only` to make the free/busy data available to other users within the same context
     * `all` to expose availability data also beyond context boundaries (i.e. for cross-context- or other external access if configured)
 * `com.openexchange.calendar.freeBusyVisibility.protected=false`: Configures if the default value that determines if public calendar folders from the default account are considered for synchronization may be overridden by the user or not.

More details are available at [https://documentation.open-xchange.com/components/middleware/config/latest/#mode=search&term=com.openexchange.calendar.freeBusyVisibility] .


[8.13]
======

General
-------

### SCR-1201
**Summary**: Added separate bundle offering HTTP liveness end-point

Added separate bundle `com.openexchange.http.liveness` part of `open-xchange-core` package list that offers the HTTP liveness end-point at configured HTTP host name (default "127.0.0.1") and liveness port (default 8016).


API - HTTP-API
--------------

### SCR-1183
**Summary**: Deprecate `delivery=view` and `content_disposition=inline` options in HTTP API

The parameter options `delivery=view` and `content_disposition=inline` and the possibility to let the client define the content type of documents and attachments, can be used to inject executable scripts into data that is rendered in browsers. This lead to several bugs in the past.
Therefore the usage of those options is deprecated and will be removed.


API - RMI
---------

### SCR-1207
**Summary**: Additional parameter 'auth' for data modification RMI services.

The following registered RMI services now require auth parameters for data modification interfaces. The parameter `com.openexchange.auth.Credentials auth` needs to be provided.

```
DBMigrationRMIService
OXContextGroup
RemoteAdvertisementService
ExternalAccountRMIService
RemoteCompositionSpaceService
SocketLoggerRMIService
LoginCounterRMIService
GABRestorerRMIService
SessiondRMIService
ChronosRMIService
ContactStorageRMIService
DataExportRMIService
ConsistencyRMIService
ContextRMIService
FileChecksumsRMIService
ResourceCacheRMIService
ShareRMIService
PushRMIService
UpdateTaskRMIService
LogbackConfigurationRMIService -> java.lang.String user, java.lang.String password
```

Behavioral Changes
------------------

### SCR-1208
**Summary**: Deprecation of Internal OAuth Authorization Server

Certain APIs of the App Suite middleware can be accessed via OAuth 2.0. In this scenario, the middleware typically acts as resource server only, and the whole client- / grant management is done by an external IDM acting as authorization server. See [the documentation|https://documentation.open-xchange.com/latest/middleware/login_and_sessions/oauth_2.0_provider/01_operator_guide.html] for further details.

Mainly as demo/showcase, it has also been possible to configure the middleware to act as OAuth authorization server itself, with integrated client- and grant management. Since this never was or meant to be used in production, this part of the OAuth provider is now deprecated, and will be removed in an upcoming version.

In practical terms, this means that the setting `auth_server` for [com.openexchange.oauth.provider.mode|https://documentation.open-xchange.com/components/middleware/config/latest/#mode=search&term=com.openexchange.oauth.provider.mode] will no longer be available, along with dependent features and functionality.


Configuration
-------------

### SCR-1203
**Summary**: New property `com.openexchange.share.guestEmailCheckRegex`

In order to prevent creation of guest users with certain email addresses, a new lean configuration property `com.openexchange.share.guestEmailCheckRegex` is introduced. The property is empty by default, reloadable and config-cascade aware.

It allows the definition of a regular expression pattern for email addresses of invited guest users. If defined, the email address of newly invited named guest users must additionally match the pattern (besides regular RFC 822 syntax checks, which are always performed), otherwise creation of the guest user is denied. The pattern is used in a case-insensitive manner.

This may be used to prevent specific email address domains for guests, e.g. by defining a pattern like

```
^((?!(?:@example\.com\s*$)|(?:@example\.org\s*$)).)*$
```

See https://documentation.open-xchange.com/components/middleware/config/latest/#mode=search&term=com.openexchange.share.guestEmailCheckRegex for further details.

### SCR-1191
**Summary**: New property to control format of internal scheduling mails

In order to control whether scheduling-related notification mails to other internal entities are sent as regular iMIP message (including iCalendar attachment) or not, a new lean configuration property named `com.openexchange.calendar.useIMipForInternalUsers` is introduced. It defaults to `false`, is reloadable, and can be set through the config-cascade down to "context" level.

Since automatic scheduling takes place within a context, attendee and organizer copies of appointments are in sync implicitly, and updates don't need to be distributed via iMIP. However, still enabling iMIP mails (in favor of notification messages only) also for internal users may be useful if external client applications are in use, or to ease forwarding invitations to others.

### SCR-1158
**Summary**: Disable mail push implementations by default, made existing properties reloadable

Changed default value for `enabled` properties for mail push features to `false`:
 * `com.openexchange.push.dovecot.enabled`
 * `com.openexchange.push.imapidle.enabled`
 * `com.openexchange.push.mail.notify.enabled`
 * `com.openexchange.push.malpoll.enabled`

Refactored mail push configuration, now all existing mail push related properties are lean and reloadable:
 * `com.openexchange.push.dovecot.*`
 * `com.openexchange.push.imapidle.*`
 * `com.openexchange.push.mail.notify.*`
 * `com.openexchange.push.malpoll.*`



Database
--------

### SCR-1186
**Summary**: New column `uuid` for table `server` in Config-DB

<warning>
**Update Task**
8.12:server:addUuidColumn / com.openexchange.database.internal.change.custom.ServerAddUuidColumnCustomTaskChange</p>
</warning>

The table `server` in the config database will get extended by a new column named `uuid` with the following column definition:

```
`uuid` BINARY(16) NOT NULL
```


This will happen through the Liquibase change set with id "8.12:server:addUuidColumn", using the custom change implemented in class `com.openexchange.database.internal.change.custom.ServerAddUuidColumnCustomTaskChange`. 


[8.12]
======

General
-------

### SCR-1195
**Summary**: New default value for `com.openexchange.sessiond.maxSession` property

With introduction of Redis-backed session storage the property `com.openexchange.sessiond.maxSession` specifying the max. allowed number of sessions becomes obsolete. That pretty old property's intention is to avoid memory problems on Middleware nodes hosting sessions node-local in memory. That is no more the case with Redis.

Hence, the old default value of "50000" for that property is changed to "0" (unlimited) in file `/opt/open-xchange/etc/sessiond.properties`.



API - HTTP-API
--------------

### SCR-1200
**Summary**: Extended the mailfilter?action=config response to include blocked action commands for the apply action

To allow a client to disable the apply button for filter rules with blocked action commands, the response of the action=config call has been extended so that the options object now contains a 'blockedApplyActions' field which contains a string array of all the blocked actions.



Configuration
-------------

### SCR-1199
**Summary**: Introduced the new lean property 'com.openexchange.mail.filter.options.apply.blockedActions' which allows to block certain mail filter actions from the apply action

Introduced the new lean property 'com.openexchange.mail.filter.options.apply.blockedActions' which defaults to "redirect". This property accepts a comma separated lists of mail filter actions which will be denied from the apply mail filter action. This helps, for example, to prevent that a message delivery system is overwhelmed by a lot of simultanous redirect actions.


### SCR-1120
**Summary**: Allow enforcing 'STARTTLS' for IMAP, POP3, SMTP, sieve

Added a few lean properties to enforce usage of `STARTTLS`.

IMAP related properties:
 `com.openexchange.imap.requireTls`
 `com.openexchange.imap.primary.requireTls`

POP3 related properties
 `com.openexchange.pop3.requireTls`

SMTP related properties:
 `com.openexchange.smtp.requireTls`
 `com.openexchange.smtp.primary.requireTls`

Sieve related properties:
 `com.openexchange.mail.filter.requireTls`

All properties are reloadable and config-cascade aware. All properties default to `true`



[8.11]
======

API - Java
----------

### SCR-1145
**Summary**: Refactored CardDAV to use IDBasedContactsAccess

Interfaces changed due to refactoring CardDAV to use `IDBasedContactsAccess`

Added methods in `com.openexchange.contact.provider.composition.IDBasedContactsAccess`:
`Map<String, UpdatesResult<Contact>> getUpdatedContacts(List<String>, Date)` - Gets lists of new and updated as well as deleted contacts since a specific timestamp in certain folders
`Map<String, SequenceResult> getSequenceNumbers(List<String>)` - Gets the sequence numbers of certain contacts folders, which is the highest timestamp of all contained items
`String getCTag(String)` - Retrieves the CTag (Collection Entity Tag) for a folder

Added methods in `com.openexchange.contact.provider.folder.FolderSyncAware`:
`Map<String, UpdatesResult<Contact>> getUpdatedContacts(List<String>, Date)` - Gets lists of new and updated as well as deleted contacts since a specific timestamp in certain folders
`Map<String, SequenceResult> getSequenceNumbers(List<String>)` - Gets the sequence numbers of certain contacts folders, which is the highest timestamp of all contained items



Behavioral Changes
------------------

### SCR-1146
**Summary**: External contacts providers are now synced via CardDAV

External contacts providers are now synced via CardDAV after refactoring to use IDBasedContactsAccess



Configuration
-------------

### SCR-1193
**Summary**: New Property "com.openexchange.admin.autoDeleteGuestsUsingFilestore"

In case a per-user filestore is associated to a guest user, and the "parent" user owning this filestore is deleted, the guest account is purged implicitly as well by default. In order to prevent that, a new lean, reloadable and config-cascade-aware property is introduced: `com.openexchange.admin.autoDeleteGuestsUsingFilestore`.

See https://documentation.open-xchange.com/components/middleware/config/latest/#mode=search&term=com.openexchange.admin.autoDeleteGuestsUsingFilestore for further details.

### SCR-1190
**Summary**: Specify a timeout when reading responses from IMAP server after a command has been issued

Added new lean property `"com.openexchange.imap.readResponsesTimeout"` accepting to define a timeout in milliseconds when reading responses from IMAP server after a command has been issued. That timeout does only apply to subscribed (not provisioned) IMAP accounts; neither primary nor secondary ones.

Default value is 60000 (one minute). A value equal to zero is infinite timeout. Reloadable and config-cascade aware.

### SCR-1189
**Summary**: Option to enable/disable usage of XCLIENT sieve extension

Added new lean configuration option `"com.openexchange.mail.filter.allowXCLIENT"` to explicitly enable (or disable) usage of the XCLIENT sieve extension.
When a sieve server announces support for the XCLIENT command, a sieve client may send information that overrides one or more client-related session attributes.

Default is false (not enabled). Reloadable and config-cascade aware.

### SCR-1188
**Summary**: Introduced a new lean property which allows to omit certain labels

Introduced the new lean property: com.openexchange.http.metrics.label.filter which allows to omit certain labels from http api metrics.



Packaging/Bundles
-----------------

### SCR-1184
**Summary**: Removed com.openexchange.hazelcast.upgrade* bundles

The following upgrade bundles are no longer needed in cloud environments after we introduced the new pre-upgrade framework (MW-1785):
 * com.openexchange.hazelcast.upgrade324
 * com.openexchange.hazelcast.upgrade312
 * com.openexchange.hazelcast.upgrade355
 * com.openexchange.hazelcast.upgrade371
 * com.openexchange.hazelcast.upgrade311
 * com.openexchange.hazelcast.upgrade381
 * com.openexchange.hazelcast.upgrade411
 * com.openexchange.hazelcast.upgrade3100

Corresponding package definitions have been removed as well:
 * open-xchange-cluster-upgrade-from-76x
 * open-xchange-cluster-upgrade-from-780-782
 * open-xchange-cluster-upgrade-from-783
 * open-xchange-cluster-upgrade-from-784
 * open-xchange-cluster-upgrade-from-7100-7101
 * open-xchange-cluster-upgrade-from-7102
 * open-xchange-cluster-upgrade-from-7103-7104
 * open-xchange-cluster-upgrade-from-7105



[8.10]
======

3rd Party Libraries/License Change
----------------------------------

### SCR-1139
**Summary**: Upgraded Socket.IO server components

Upgraded Socket.IO server components in bundle "`com.openexchange.socketio`" to support Engine.IO v4 and Socket.IO v3

* engine.io-server-1.3.5.jar  -->  engine.io-server-6.1.0.jar
* socket.io-server-1.0.3.jar  -->  socket.io-server-4.0.1.jar



API - HTTP-API
--------------

### SCR-1180
**Summary**: Allow adding attachments from other mails during mail composition

The `addAttachment` action from the module `mailcompose` of the HTTP API is extended with an additional "origin" within the existing JSON form field of the multipart/form-data payload.

By specifying "mail" as "origin" the client is allowed to add a file attachment from an existing mail message to the composition space

Example:


```
POST /mailcompose?action=addAttachment
Content-Type: multipart/form-data; boundary=----WebKitFormBoundaryyhuRsdTCa7hO6MJ4

------WebKitFormBoundaryyhuRsdTCa7hO6MJ4
Content-Disposition: form-data; name="contentDisposition"

ATTACHMENT
------WebKitFormBoundaryyhuRsdTCa7hO6MJ4
Content-Disposition: form-data; name="JSON"

{"origin":"mail", "id":"40", "folderId":"default0/INBOX", "attachmentId":"2"}
------WebKitFormBoundaryyhuRsdTCa7hO6MJ4--
```


### SCR-1166
**Summary**: New action "getRecurrence" in Module "chronos"

In order to supply clients with information whether a change exception is considered as rescheduled or overridden, the new action `getRecurrence` is introduced in the `chronos` module of the HTTP API. Prior chosing whether the whole series or just the actual recurrence should be changed by an update operation, this action can be performed to get all necessary information.

Further details are available at https://documentation.open-xchange.com/components/middleware/http/latest/index.html#!Chronos/getRecurrence . 

### SCR-1162
**Summary**: New Parameter "includeDelegates" for Action "needsAction" in Module "chronos"

The `needsAction` action in the module `chronos` of the HTTP API is extended by the new URL parameter `indludeDelegates`. 

If set to `true`, an enhanced response is returned which includes the events needing action of the session user itself, along with the data for other attendees the user has delegate scheduling permissions for. This includes both resource attendees where the user may act as booking delegate for, as well as other attendees where the user has a shared calendar with write access. If the parameter is set to `false`, only events for the current user are included in the response.

Whenever the parameter is set, an enhanced response in form of an array will be returned, where each element lists the attendee, together with the corresponding events needing action for that attendee. As before, for series events, overridden instances that are not considered as re-scheduled are hidden implicitly in the results. For backwards compatibility reasons, if the parameter `includeDelegates` is not set in the request, the previous, 'flat' response is returned to clients for the time being.

Further details are available at https://documentation.open-xchange.com/components/middleware/http/latest/index.html#!Chronos/getEventsNeedingAction .

### SCR-1154
**Summary**: Extended resource model with scheduling privileges

In order to store scheduling privileges for resources of users and groups, the `resource` object of the HTTP API is extended with an array holding scheduling privileges per user names `permissions`. Each array element holds a scheduling privilege object which has the following properties:
* `entity`, `integer`: Internal identifier of the user or group to which this permission applies.
* `group`, `boolean`: Set `true` if entity refers to a group, `false` if it refers to a user
* `privilege`, `string`: One of 
** `none` - No privileges to book the resource 
** `ask_to_book` - May submit a request to book the resource if it is available 
** `book_directly` - May book the resource directly if it is available 
** `delegate` - Act as delegate of the resource and manage bookings

Additionally, the read-only field `own_privilege` is introduced for resource objects, which indicates which effective privileges apply for the requesting user.

More details are available at https://documentation.open-xchange.com/components/middleware/http/latest/index.html#!Resources



API - Java
----------

### SCR-1170
**Summary**: Removed publication of TextXtractService and changed interface IMailMessageStorage

The use of Apache Tika within the Open-Xchange server was reduced to a possible minimum. 

As a result there is no need to keep the publication of '`TextXtractService`'. All implementations and the interface will be removed. There was no need to adapt the usage as it just was used in obsolete code.

The last java related change was the removal of the following method from `IMailMessageStorage`: 

`'public String[] getPrimaryContents(String folder, String[] mailIds) throws OXException;'`

### SCR-1167
**Summary**: New method "getRecurrenceInfo" within Chronos Stack

In order to drive the new action `getRecurrence` of the HTTP API, the Chronos stack is extended with a corresponding method with the following signature:

```
RecurrenceInfo getRecurrenceInfo(EventID eventID) throws OXException;
```

Implementations are available for the default internal, as well as the cross-context provider.

### SCR-1163
**Summary**: Adjusted Signature of "getEventsNeedingAction" Method throughout Chronos Stack

The method `#getEventsNeedingAction` is adjusted throughout the calendar stack, which includes the compositing layer, as well as the interfaces of the implementing services. A new `boolean` method parameter named `includeDelegates` is introduced, and the method response type is now a `Map` associating `Attendee` s  to their `EventsResult` s.



API - SOAP
----------

### SCR-1161
**Summary**: Extended SOAP provisioning interface for managed resources

The resource object for SOAP webservices `OXResourceServicePortType` and `OXResellerResourceServicePortType` have been extended for provisioning managed resources. The resource object has now additional `permissions` parameter:


```
<xsd:permissions>
   <xsd:entity>2</xsd:entity>
   <xsd:group>0</xsd:group>
   <xsd:privilege>book_directly</xsd:privilege>
</xsd:permissions>
```


-Also SOAP webservices `OXResourceServicePortType` and `OXResellerResourceServicePortType` got new operation `removePermissions`-
Permissions are removed by not mentioning them in resource object



Behavioral Changes
------------------

### SCR-1160
**Summary**: Removed direct link from notification mails

Within the internal notification mails for calendar events, there were direct links pointing to the appointment and (if those existed) for their attachments, for a quicker access.

Those direct links however are static and might be, shortly after the generation, out of date. For example, a user only had to move the appointment to a different calendar and the static link in the notification mail doesn't lead anywhere.

Further, the UI requests, renders and links the current event data on notification mails dynamically, efficiently solving the problem the direct links were created for much better. Thus, there is no need for the direct links anymore.



Configuration
-------------

### SCR-1181
**Summary**: New Properties to Control 'used-for-sync" Behavior of Calendar Folders

In order to control whether _public_ or _shared_ calendar folders are considered for synchronization via CalDAV by default or not, the following new lean configuration properties are introduced with the indicated defaults:

```
# Configures if shared calendar folders from the default account are considered for 
# synchronization by default or not. May still be set individually by the end user 
# unless also marked as protected.
com.openexchange.calendar.usedForSync.shared.default=true
```


```
# Configures if the default value that determines if shared calendar folders from the 
# default account are considered for synchronization may be overridden by the user or not.
com.openexchange.calendar.usedForSync.shared.protected=false
```


```
# Configures if public calendar folders from the default account are considered for 
# synchronization by default or not. May still be set individually by the end user 
# unless also marked as protected.
com.openexchange.calendar.usedForSync.public.default=true
```


```
# Configures if the default value that determines if public calendar folders from the 
# default account are considered for synchronization may be overridden by the user or not. 
com.openexchange.calendar.usedForSync.public.protected=false
```

All properties are reloadable and can be configured through the config cascade. With the implicit defaults, no existing semantics are changed, i.e. all shared/public folders of the default account continue to be used for sync by default, overridable by end users.

More details are available at [https://documentation.open-xchange.com/components/middleware/config/latest/#mode=search&term=com.openexchange.calendar.usedForSync] .

### SCR-1148
**Summary**: Allow using multiple services for password-change functionality

Since we now allow different `PasswordChangeServices` to be used in parallel, we must have some configuration that enables or disables certain services for certain context/users. Therefore, the following properties were introduced:



```
com.openexchange.passwordchange.script.enabled=false
com.openexchange.passwordchange.db.enabled=false
```


The database based password change is disabled by default, reflecting the status before the code changes. In older versions you had to actively install the packages.

### SCR-1142
**Summary**: Helm: Configuration of sensitive mandatory properties

With MW-1814 we removed the default values for some sensitive properties. 
 As some of those properties are still mandatory, we have updated the ox-common chart to generate secure random values, if no values have been specified (MW-1830). Those values are stored in a k8s secret called `<RELEASE>-common-env` and will be used by multiple charts/services (e.g. core-mw, core-imageconverter, ...).

The following properties are affected:

```
com.openexchange.cookie.hash.salt
com.openexchange.share.cryptKey
com.openexchange.sessiond.encryptionKey
```

From now on, administrators should set those properties in the global section of the deployment's `values.yaml` file.

Example:

```
global:
  core:
    cookieHashSalt: "KtLUTLKZrbXvCAOn"
    shareCryptKey: "lJZEFPzUYfapWbXL"
    sessiondEncryptionKey: "auw948cz,spdfgibcsp9e8ri+<#qawcghgifzign7c6gnrns9oysoeivn"
```

This will create the following k8s secret:

```
apiVersion: v1
kind: Secret
metadata:
  name: <RELEASE>-common-env
  namespace: <RELEASE>
  annotations:
    helm.sh/resource-policy: "keep"
  labels:
    helm.sh/chart: ox-common-1.0.22
data:
  COOKIE_HASH_SALT: cHlDN3p5RU1kZ0FmT3Znag==
  SHARE_CRYPT_KEY: Ujk5RFFVUGd4TWox
  SESSIOND_ENCRYPTION_KEY: eTY2cGk4azdXdFNpZ1BzTkJhVVIwWm9rN1lHM0M1YTZGVGZLenJkRWd5eVlwMGRuVjVtWjloSDFJUw==
```

Those environment variables will then be injected into the service containers and written into the relevant `.properties` files by the individual charts.



Packaging/Bundles
-----------------

### SCR-1182
**Summary**: Upgraded logback-extensions to 2.1.4

The logback-extensions library was upgraded to version 2.1.4 which includes some previously missing fields in the json logger.

### SCR-1171
**Summary**: Removed bundles com.openexchange.textxtraction and org.apache.tika

The use of Apache Tika within the Open-Xchange server was reduced to a possible minimum. As a result the bundles `org.apache.tika` and `com.openexchange.textxtraction` will be removed.

### SCR-1147
**Summary**: Allow multiple services for password-change functionality

With the new version 8.x of the Open Xchange App Suite we moved from package based installations to Docker/Kubernetes. For this, we need to be able to install all packages in parallel within the images we deliver. The different password change implementations however were conflicting. Therefore, we removed those packages and restructured the code.

Removed packages:

```
open-xchange-passwordchange-database
open-xchange-passwordchange-script
```


Removed bundles: 

```
com.openexchange.passwordchange.database
com.openexchange.passwordchange.script
```


Added bundles:


```
com.openexchange.passwordchange
com.openexchange.passwordchange.common
com.openexchange.passwordchange.impl
```


The added bundles are now delivered within the `open-xchange-core` package

The property files `change_pwd_script.properties` and `passwordchange.properties` were moved to the bundle `com.openexchange.passwordchange.impl` alongside the restructuring.


[8.10]: https://gitlab.open-xchange.com/middleware/core/-/compare/8.9.0...8.10.0
[8.11]: https://gitlab.open-xchange.com/middleware/core/-/compare/8.10.0...8.11.0
[8.12]: https://gitlab.open-xchange.com/middleware/core/-/compare/8.11.0...8.12.0
[8.13]: https://gitlab.open-xchange.com/middleware/core/-/compare/8.12.0...8.13.0
[8.14]: https://gitlab.open-xchange.com/middleware/core/-/compare/8.13.0...8.14.0
[8.15]: https://gitlab.open-xchange.com/middleware/core/-/compare/8.14.0...8.15.0
[8.16]: https://gitlab.open-xchange.com/middleware/core/-/compare/8.15.0...8.16.0
[8.17]: https://gitlab.open-xchange.com/middleware/core/-/compare/8.16.0...8.17.0
[8.18]: https://gitlab.open-xchange.com/middleware/core/-/compare/8.17.0...8.18.0
[8.19]: https://gitlab.open-xchange.com/middleware/core/-/compare/8.18.0...8.19.0
