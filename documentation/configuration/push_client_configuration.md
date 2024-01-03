---
title: Push client configuration
icon: fa fa-bell
tags: Configuration, Push, APN, APNS, GCM, FCM
---


# How to configure push transport clients

In order to send push messages to a client you need to configure the necessary push transport client. Previously this was done via different yml files and properties. Now this has been unified and if you still use the old way you need to migrate to this approach. For this please consider the migration guide further down in this article. 

The transport clients are now configured via yml files in the '/opt/open-xchange/etc/pushClientConfig' folder. All yml files in this folder are considered, however, the used client identifiers needs to be unique across all files. You can also configure multiple transport clients in one file. 

The yml files need to follow the following structure:

The top elements are unique names which define the transport client ids. Then each client contains a '\_type' field which contains the type of the transport client. Currently only 'apn' and 'fcm' are supported.

The other fields of a config vary depending on the type.

## fcm

This is the configuration for android based clients which use the firebase cloud messaging service. The configuration for this type contains the following fields:

| Name | Type   | required | description |
|:---- | :----- | :------: | :---------- |
| key  | String | true     | The fcm key |

### Example config

```
my-fcm-transport-client:
  _type: fcm
  key: ABCDEFG12345
```

## apn

The configuration for ios/macos based clients which use the apple push notification service. This type supports two configuration modes. 

### Certificate

A configuration based on a certificate. There are actually two ways to provide the keystore which contains the certificate.
On one hand you can mount it as a file into the container and configure the path via the 'keystore' field. On the other hand and also the preferred way is to deploy it as a secret in the k8s cluster in the same namespace, label it with 'OX_KEYSTORE=YourKeystoreId' and configure this id via the keystoreId field. 

Here is an overview over all fields for this auth type:

| Name        | Type    | required | description                                                                    |
|:----------- | :------ | :------: | :----------------------------------------------------------------------------- |
| authtype    | String  | false    | The config type. If configured must be set to 'certificate'                    |
| keystoreId  | String  | false    | The id of the keystore. Must be mapped to the OX_KEYSTORE label of the secret. |
| keystore    | String  | false    | The path to the local keystore file.                                           |
| password    | String  | false    | The password of the keystore.                                                  |
| topic       | String  | true     | The topic of your application. Typically the bundle ID of the app.             |
| production  | boolean | false    | Indicates which apn service to use.                                            |

#### Example apn certificate config

```
my-apn-certificate-based-transport-client:
  _type: apn
  authtype: certificate
  topic: com.openxchange.mobile.drive2.pushkit.fileprovider
  production: true
  keystoreId: my-secret-label-value
  password: myKeystorePassword
```

### JWT

A configuration based on a json web token. Again there are two ways to provide the jwt.
On one hand you can mount it as a file into the container and configure the path via the 'privatekey' field. On the other hand and also the preferred way is to deploy it as a secret in the k8s cluster in the same namespace, label it with 'OX_KEYSTORE=YourKeystoreId' and configure this id via the privatekeyId field. 

Here is an overview over all fields for this auth type:

| Name         | Type    | required | description                                                                    |
|:------------ | :------ | :------: | :----------------------------------------------------------------------------- |
| authtype     | String  | true     | The config type. Must be set to 'jwt'.                                         |
| privatekeyId | String  | false    | The id of the jwt. Must be mapped to the OX_KEYSTORE label of the secret.      |
| privatekey   | String  | false    | The path to the local jwt file.                                                |
| keyid        | String  | true     | The key identifier of this jwt.                                                |
| teamid       | String  | true     | The team identifier of this jwt.                                               |
| topic        | String  | true     | The topic of your application. Typically the bundle ID of the app.             |
| production   | boolean | false    | Indicates which apn service to use.                                            |

#### Example apn jwt config

```
my-apn-jwt-based-transport-client:
  _type: apn
  authtype: jwt
  topic: com.openxchange.mobile.drive2.pushkit.fileprovider
  production: true
  privatekeyId: my-secret-label-value
  keyid: MyKeyId
  teamid: MyTeamId
```

# How to map push subscriptions to transport clients

Once you configured all your transport for every client you want to support you need to map the subscriptions to the respective transport. 
There are actually two different systems in place.

## Drive

For push messages to drive clients you need to enabled the push transport and configure the client id for every transport type. The properties can be defined individually per user through the config cascade.

```
com.openexchange.drive.events.apn2.ios.enabled=true
com.openexchange.drive.events.apn2.ios.clientId=my-apn-certificate-based-transport-client

com.openexchange.drive.events.apn.ios.enabled=true
com.openexchange.drive.events.apn.ios.clientId=my-apn-jwt-based-transport-client

com.openexchange.drive.events.gcm.enabled=true
com.openexchange.drive.events.gcm.clientId=my-fcm-transport-client
```

## Mobile Api Facade

For clients which use the mobile api facade it is a bit different. Here the mobile client id must match the transport client id.
So you need to configure the transport client with the mobile client id as a prefix followed by the operating system of the mobile client.

So for an android client with the client id 'my-mobile-client' you need to configure a transport client with the id 'my-mobile-client-android'.
And for an ios client with the client id 'my-other-client' you need to configure a transport client with the id 'my-other-client-ios'.


# Migration

If you want to migrate from your current configuration to the new one please follow the following steps

## Drive

* Remove all com.openexchange.drive.events.* properties with the exception of the com.openexchange.drive.events.\*.enabled properties
* Optionally create a k8s secret from the used keystore files and label them with the OX_KEYSTORE key and a unique value of your choosing
* Create the yml configuration
  * Create a yml file and mount it into the /opt/open-xchange/etc/pushClientConfig folder
  * Add a transport configuration to it for each different transport client you want to support
    * Choose a unique name
    * Choose the respective type (apn or fcm)
    * Take over the configuration of the existing
    * Optionally set the keystore id to the value of your secret label
* For each transport configure the respective com.openexchange.drive.events.\*.clientId

## Mobile Api Facade

### APN

* Optionally create a k8s secret from the used keystore files and label them with the OX_KEYSTORE as a key and a unique value of your choosing
* Move your /opt/open-xchange/etc/pns-apns-options.yml into the /opt/open-xchange/etc/pushClientConfig folder
* Adjust the yml
  * Remove the enabled field of this config
  * Adjust the name to match the id + the operating system of your mobile client (see above)
  * Replace keystore with keystoreId and set it to the respective secret label value

### GCM

Basically do the same as for apn for the pns-gcm-options.yml file with the exception that the keystore changes are not necessary.