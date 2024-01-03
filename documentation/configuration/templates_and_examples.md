---
title: Templates and Examples
icon: fa-file-code
tags: Installation, Configuration, Templates
---

The Open-Xchange Middleware needs several configuration files to run properly. Below you can find a series of template and example files which help with configuration. For more information on the respective files, either navigate to the corresponding part of the documentation using the link if available, or open the file for further information. 

*Note: Not every file is treated in the documentation. File names are clickable.*

# Client On-boarding

*   [`client-onboarding-scenarios-template.yml`](https://gitlab.open-xchange.com/middleware/core/-/blob{{ site.branchName }}/com.openexchange.client.onboarding/doc/examples/client-onboarding-scenarios-template.yml)

    The YAML configuration file for on-boarding scenarios.

# Database

*   [`dbconnector-template.yaml`](https://gitlab.open-xchange.com/middleware/core/-/blob{{ site.branchName }}/com.openexchange.database/doc/examples/dbconnector-template.yaml)

    Configuration file for the Java database connector.

*   [`globaldb-template.yml`](https://gitlab.open-xchange.com/middleware/core/-/blob{{ site.branchName }}/com.openexchange.database/doc/examples/globaldb-template.yml)

    Configuration file for global, cross-context databases.

# LDAP

*   [`contacts-provider-ldap-mappings-template.yml`](https://gitlab.open-xchange.com/middleware/core/-/blob{{ site.branchName }}/com.openexchange.contact.provider.ldap/doc/examples/contacts-provider-ldap-mappings-template.yml)

    Example definitions of contact property &#10231; LDAP attribute mappings.

    Please see [Contacts/Contacts Provider LDAP](https://documentation.open-xchange.com{{ site.baseurl }}/middleware/contacts/contacts_provider_ldap.html#configure-mappings) for further information.

*   [`contacts-provider-ldap-template.yml`](https://gitlab.open-xchange.com/middleware/core/-/blob{{ site.branchName }}/com.openexchange.contact.provider.ldap/doc/examples/contacts-provider-ldap-template.yml)

    Example definitions of available LDAP contact providers, together with their corresponding configuration, referenced LDAP client connection settings and attribute mappings.

    Please see [Contacts/Contacts Provider LDAP](https://documentation.open-xchange.com{{ site.baseurl }}/middleware/contacts/contacts_provider_ldap.html#define-contacts-provider) for further information.

*   [`ldap-client-config-template.yml`] (https://gitlab.open-xchange.com/middleware/core/-/blob{{ site.branchName }}/com.openexchange.ldap.common/doc/examples/ldap-client-config-template.yml)

    A template which provides some basic LDAP client configuration as a reference.

    Please see [Administration/LDAP Client Configuration](https://documentation.open-xchange.com{{ site.baseurl }}/middleware/administration/ldap_client_configuration.html) for further information.

# Login and Sessions

*   [`app-password-apps-template.yml`](https://gitlab.open-xchange.com/middleware/core/-/blob{{ site.branchName }}/com.openexchange.authentication.application.impl/doc/examples/app-password-apps-template.yml)

    Example definitions of available applications, together with their corresponding scope-definitions, that can be used with app-specific passwords.

    Please see [Login and Sessions/Application Details](https://documentation.open-xchange.com{{ site.baseurl }}/middleware/login_and_sessions/application_passwords.html#application-details) for further information.

# Webhooks

*   [`webhooks-template.yml`](https://gitlab.open-xchange.com/middleware/core/-/blob{{ site.branchName }}/com.openexchange.webhooks/doc/examples/webhooks-template.yml)

    Example definitions of available Webhooks that can be subscribed by clients.

    Please see [Config/Wehooks](https://documentation.open-xchange.com{{ site.baseurl }}/middleware/config/webhooks.html) for further information.
