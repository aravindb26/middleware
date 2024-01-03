---
title: Drive Client Windows Setup
icon: fa fa-cloud
tags: Drive Client Windows, Kubernetes, Drive Client, Cloud
---

# Overview

Starting with version v8.0.0 of the Open Xchange Server windows drive clients need to be fetched from dedicated services which host the binary files. 
This article shows how you need to configure the Open Xchange Server in order to communicate with the drive client services, retrieve the manifests and download links. The download itself needs to be triggered from client side using the delivered download link.  


# Cluster Configuration

In general Open Xchange Server knows two basic cluster mode configuration settings: 
* *KUBERNETES* (which is the default)
* *EXTERNAL* 

The main difference is the way a running middleware pod is searching for available drive client services. 

Via property `com.openexchange.drive.client.windows.mode` it is possible to control which one will be used.

Some of the later described properties are type specific which means they only affect one or the other mode whereas other properties are cluster mode independent.      

## Kubernetes Mode

With this mode in place drive client services will be fetched by contacting a kubernetes API and search for services with a specific label in a configurable namespace. Said this it might also be necessary to setup a proper RBAC configuration in case the drive service`s namespace differ from the one the middleware pod is running in. This could be achieved by e.g. setting up a cluster role:

```yml
kind: ClusterRole
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  namespace: <the-namespace>
  name: service-reader
rules:
- apiGroups: [""] # "" indicates the core API group
  resources: ["services"]
  verbs: ["get", "list"]
```

By default Open Xchange Server retrieve drive client services from the same namespace the middleware pods are running in and there is no need to take care about access control.  


In general you only activate the bundle ``open-xchange-drive-client-windows`` and keep all properties with their defaults.
This will result in:
* Find services labeled with ``drive-client`` running in the middleware pod`s namespace
* Fetch the branding from each of those services
* Configure an endpoint for each of those services which looks like ``https://{servicenname}.{namespace}.svc.cluster.local/manifest.json``


## External Mode

With this mode a generic drive client service per requested brand will be created. The services are some kind of virtual services with a configurable URL where the branding becomes part of the manifest URI.    

Activate the bundle ``open-xchange-drive-client-windows`` and adjust some properties to setup the service.

Sample configuration for a request to brand ``foo``:

```
com.openexchange.drive.client.windows.mode=EXTERNAL
com.openexchange.drive.client.windows.external.manifestUrl=https://drive.example.com/appsuite/[branding]/drive/client/windows/manifest.json
```

The resulting virtual service is branded to ``foo`` and use the following URL to retrieve the manifest 
*https://drive.example.com/appsuite/foo/drive/client/windows/manifest.json*


