---
title: Export PDF
icon: fa-file-pdf
tags: Mail, Configuration, Installation
---
# Introduction

Efficient and flexible document handling is of utmost importance in the digital landscape. By providing the option to export emails as PDFs, we offer the possibility to transform mail into a universally accepted format that retains its accessibility across various devices and platforms. Exporting an email as a PDF offers several use cases and advantages over simply exporting it as .eml. While .eml allows storing and archiving emails, it is not a particularly convenient or accessible format for most users. By exporting an email as a PDF, users receive the original email in a format that comes with numerous benefits, such as platform and device independence, easy sharing and distribution, and enhanced security and privacy, to name just a few. Especially organizations which require official documentation for record-keeping, archiving, or legal purposes, are benefit from the PDF export feature.

# Components

This feature relies heavily on third-party components, two to be exact, [Collabora](https://sdk.collaboraonline.com/docs/installation/CODE_Docker_image.html) and [Gotenberg](https://gotenberg.dev/).

## Gotenberg

The Gotenberg component is used to primarily convert the HTML version of an e-mail to a PDF document.

## Collabora

The Collabora component is used to convert any supported type (see property `com.openexchange.mail.exportpdf.collabora.fileExtensions` for more information), including the converted HTML body from Gotenberg, to a valid PDF/A document. 

## MailExportService

The `MailExportService` is the central component of this feature. Initialises and coordinates the mail export process. Accepts the mail export requests from the clients, fetches the mail that is to be exported from the mail server, performs the conversion (both of the body and the attachments — depending upon the export options, see below) and stores the exported e-mail as a PDF/A to the user's drive account i.e. the Filestore.

# Export Options

There are different export options that can be used via the HTTP API to parametrise the export.

`folder_id`: Defines the drive folder in which the exported PDF/A document will be saved. This option is required.

`pageFormat`: Defines the page format of the export document. It can either be `a4` (which is the default behaviour) or `letter`. This option is not required. If absent, the page format will be derived from the user's locale setting (for `us` or `ca` the page format will be `letter` and for anything else `a4`).

`preferRichText`: If this option is enabled then, if an e-mail message contains both text and HTML versions of the body, then the latter is preferred and converted to a PDF/A document before it is appended to the exported PDF/A document. If only the text version is available, and the option is enabled, then the text version is converted to a PDF/A document and appended to the exported PDF/A document. This option is not required and by default is set to `true`.

`includeExternalImages`: If this option is enabled then, and the e-mail contains any external inline images, then those images will be fetched from their respective sources and included to the exported PDF/A document at their supposed positions. This option is not required and is by default `false`.

`appendAttachmentPreviews`: If this option is enabled, then any previewable attachment (i.e., documents and pictures) is converted from their original format, e.g., from docx or tiff, to a PDF/A document and is appended as one or more pages to the exported PDF/A document. This option is not required and is `false` by default.

`embedAttachmentPreviews`: If this option is enabled, then any previewable attachment is converted from their original format to a PDF/A document and is embedded as an attachment to the exported PDF/A document. This option is not required and is `false` by default.

`embedRawAttachments`: If this option is enabled, then all attachments are embedded without further processing to the exported PDF/A document as attachments. This option is not required and is `false` by default.

`embedNonConvertibleAttachments`: If this option is enabled, then all attachments (previewable and non-previewable, i.e., zips, mp4s, etc.) are embedded without further processing to the exported PDF/A document as attachments. This option is not required and is `false` by default.

# Setup

As mentioned above, alongside this feature, there are two third-party applications that are required in order for this to work properly. Both are available as Docker containers, but they can also be VM instances or bare metal installations, which-ever you prefer. Refer to their appropriate guides on how to install them separately. If you choose the container variant, then there are already helm charts provided to ease the deployment.

# Helm Charts

The `core-mw` helm chart provides charts for [Gotenberg](https://github.com/MaikuMori/helm-charts/blob/master/charts/gotenberg/README.md) and [Collabora](https://github.com/CollaboraOnline/online/blob/master/kubernetes/helm/README.md) as dependencies. The services can be enabled via `core-mw.gotenberg.enabled` and `core-mw.collabora-online.enabled`. Please see the `core-mw` [chart documentation](https://gitlab.open-xchange.com/middleware/core/-/blob/main/helm/core-mw/README.md) for detailed service configuration options.

# Configuration

There is a bunch of properties with which this feature can be configured and tailored to fit the needs of its users. However, in this article we will only discuss the most prominent and must-have in order to enable this feature.

To activate the feature, you need to configure the following capability:

```properties
com.openexchange.capability.mail_export_pdf=true
```
The capability is reloadable and config-cascade aware. Furthermore, in order to work properly, two properties must be enabled, namely those of the previous mentioned converters: Collabora and Gotenberg.

```properties
com.openexchange.mail.exportpdf.gotenberg.enabled=true
com.openexchange.mail.exportpdf.collabora.enabled=true
```
The end-point URLs for both services must also be configured, namely:

```properties
com.openexchange.mail.exportpdf.collabora.url=http://<COLLABORA_HOST>:<COLLABORA_PORT>
com.openexchange.mail.exportpdf.gotenberg.url=http://<GOTENBERG_HOST>:<GOTENBERG_PORT>
```

Both accept a full url with protocol and port and default to localhost. Adjust at your leisure.

More configuration options can be found [here](https://documentation.open-xchange.com/components/middleware/config{{ site.baseurl }}/#mode=tags&tag=Mail%20Export).
