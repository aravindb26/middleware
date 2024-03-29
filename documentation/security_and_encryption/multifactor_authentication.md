---
title: Multifactor Authentication
icon: fa-link
tags: Security, Configuration
---

Appsuite provides methods for users to require secondary, additional methods of authentication before creating a valid Appsuite session. These methods may include SMS messages, Time based authenticator methods, U2F compatible devices/keyfobs, and other custom methods.

**Please Note:** When a user enables multifactor authentication for their account, their logins will be limited to the Appsuite UI only, as client applications (Mail app, Drive app, Exchange Active Sync) do not currently have the interface to perform the additional multifactor requirements.  As of 7.10.4, users can add Application Passwords to use in order to bypass multifactor authentication for their client applications.  See [Application Passwords]({{site.baseurl}}/middleware/login_and_sessions/application_passwords.html) for details.

# Enabling Multifactor

The `open-xchange-multifactor` package is required for the core of multifactor authentication. For some methods (such as SMS), additional packages will be required.

With SMS, for example, you must also install a provider, such as `open-xchange-sms-sipgate`.

Then, multifactor must be enabled as a capability.  This can be done in the multifactor.properties file, or as a cascade value

```properties
com.openexchange.capability.multifactor=true
```

# Enabling SMS

First, the SMS provider must be installed and configured.  Most will require a configured username and password, or AUTH_TOKEN.  Install the needed package and configure.

At that point, you should enable SMS in the multifactor.properties file

```properties
 com.openexchange.multifactor.sms.enabled=true
```

The following properties are also available

```properties
com.openexchange.multifactor.sms.tokenLength   (default is 8 characters)
com.openexchange.multifactor.sms.tokenLifetime (Number of minutes until challenge expires)
com.openexchange.multifactor.maxTokenAmount (Maximum number of challenges before locked out)
```

# Enabling TOTP

TOTP is Time-based One Time Password.  This works with several apps available in mobile stores, such as Google Authenticator.

To enable, just set in the multifactor.properties file

```properties
com.openexchange.multifactor.totp.enabled=true
```

## Enabling Backup String

This is a method to allow a user to log into their account if they lose their primary multifactor authentication device (say losing their phone or U2F token).  It is a long string that they can copy, download, or print to use to unlock the account in the event of loss

To enable, set in the multifactor.properties file

```properties
com.openexchange.multifactor.backupString.enabled=true
```

# Enabling U2F

U2F is supported in Google Chrome, as well as Firefox (though requires user changing advanced settings).

In multifactor.properties, enable U2F

```properties
com.openexchange.multifactor.u2f.enabled=true
```

Then, the domain that the user will be using must be specified.  This will be used with the requests to the U2F device, and must mach the website.  This configuration is config-cascade aware

```properties
 com.openexchange.multifactor.u2f.appId=https://yourdomain
```

# Enabling WebAuthn

WebAuthn allows authentication with public-key cryptography. The WebAuthn provider is backwards compatible with U2F, which means that authentication with already registered U2F devices is possible.

**Note that the WebAuthn provider is currently only able to perform authentication with registered U2F devices. Registrations of new devices via WebAuthn is currenlty not supported.**

In multifactor.properties, enable WebAuthn

   

```properties
com.openexchange.multifactor.webAuthn.enabled=true
```

Then, the name of the domain that the user will be using must be specified.  This will be used with the requests to the WebAuthn device, and must mach the website. This configuration is config-cascade aware.

```properties
 com.openexchange.multifactor.webAuthn.rpId=yourdomain
```

# Login Page

By default, the UI will change from the login page, draw the customized toolbar, then display a prompt for the multifactor authentication.

If you would prefer to have your login screen, or a different second factor screen used as the background, then you can configure in the as-config.yml

For example:

```yaml
 default:
     host: all
     signinTheme: default
     multifactorBackground: pages/secondFactor
```

