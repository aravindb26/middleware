# Swagger definition of OX APIs

## Overview

This respository contains the documentation of OX APIs (like OX HTTP API) in OpenAPI. OpenAPI enables the
description of a REST API in one YAML or JSON file using the [OpenAPI Specification](http://swagger.io/specification/).
The descriptions of the OX APIs take place in several YAML files that are translated into one JSON file.
This makes it possible to separate the whole documentation in smaller parts each representing one HTTP request.


## What's my job here?

As a developer you will typically change or add API docs during feature development. Details on what and how to change are described below in detail. 
After making your changes, you are supposed to perform a sanity check that your changes compile and that HTML and Java clients can be successfully generated and contain your changes. 
Last but not least, check in your changes. For example after changing HTTP API docs, execute:

```
./gradlew :http-api:clean :http-api:build :rest-api:clean :rest-api:build :drive-api:clean :drive-api:build
```

After editing and successful build, you'll find a few modified files in your workspace (in this example we have touched `http_api/components/schemas/userme/CurrentUserData.yaml`):

```
% git status
On branch develop
Your branch is up to date with 'origin/develop'.

Changes not staged for commit:
  (use "git add <file>..." to update what will be committed)
  (use "git checkout -- <file>..." to discard changes in working directory)

	modified:   http_api/components/schemas/userme/CurrentUserData.yaml
```

### File Structure

Instead of using one big file, each request and parts that belong to it, are placed in single YAML files. 
The YAML files that belong together can be grouped in folders. 

To sum up, we look at a folder example of the OX HTTP API. The `http_api` folder contains the OpenAPI definition of the requests:

```
http_api
|— paths
     |— login
          |— AutoLoginRequest.yaml
          |— ChangeIpRequest.yaml
          |— ...
          |— index.yaml 
     |— messaging
          |— account
               |— AllRequest.yaml
               |— ...
               |— index.yaml
          |— message
               |— AllRequest.yaml
               |— ...
               |— index.yaml
          |— ...
               |— ...
|— components   
     |— parameters
          |— ChronosFromTimeRange.yaml
          |— ChronosUntilTimeRange.yaml
          |— gblPathParamConfigNode.yaml
          |— ...
          |— index.yaml 
     |— requestBodies
          |— chronos
               |— EventData.yaml
               |— EventIdArray.yaml
               |— ...
          |— contacts
               |— ...
          |— index.yaml
     |— schemas
          |— ...
|— info
     |— index.yaml
```

#### Paths

Each folder represents a module with its requests and contains an index.yaml file, which lists all files in the folder. 
Furthermore it is also possible to divide the requests of a module in several subfolders of the module folder. 
In this case each subfolder must have its own index.yaml file. The content of the index.yaml of the config-module might look as follows:

```yaml
requests: 
  - configRequest.yaml
  - get_propertyRequest.yaml
  - set_propertyRequest.yaml
```

It describes what requests of the module are available and where they are found. 
Because all files (index.yaml and the YAML files of the requests) are stored in the same folder it simply contains the file names. 

#### Components

The components section holds a set of reusable objects for different aspects of the OpenAPI. 
All objects defined within the components tree will have no effect on the API unless they are explicitly referenced from properties outside the components object.
The schemas for those objects are defined under components/schemas and they are referenced within the parameters, requestBodies and responses folders.

##### Parameters

It is possible to outsource globale parameters. These parameters are not automatically added to all requests but the definition of those parameters must only be 
written once and can be referenced using the "$ref" tag. These parameters are defined within the index.yaml in the parameters folder.

##### RequestBodies

Similary to the parameters folder the RequestBodies folder contains definitions for request bodies.

##### Responses

Basically the same as RequestBodies but for responses.

##### Schemas

Schemas are one of the core features of OpenAPI 3.0. Schemas are reusable objects that can be found in almost any requestBody or responseBody of a request. 
The schema folder contains an overall index.yaml file which contains named references to schema defintions which can be found in subfolders of the schemas folder. It may look like this:

```yaml
# Common response containing the error fields and a timestamp
CommonResponse:
  type: object
  properties:
    $ref: ./common/CommonResponseData.yaml
# Module: login
LoginResponse:
  $ref:  ./login/LoginResponse.yaml
TokenLoginResponse:
  $ref: ./login/TokenLoginResponse.yaml
TokensData:
  $ref: ./login/TokensData.yaml
TokensResponse:
  $ref: ./login/TokensResponse.yaml
ChangeIPResponse:
  $ref: ./login/ChangeIPResponse.yaml
```

Snippet of the content of the LoginResponse.yaml:
```yaml
type: object
properties:
  session:
    type: string
    description: The session ID.
  user:
    type: string
    description: The username.
  user_id:
    type: integer
    description: The user ID.
  context_id:
    type: integer
    description: The context ID.
  locale:
    type: string
    description: The users locale (e.g. "en_US").
  ...
```

#### Info

The info-index.yaml file contains general information. E.g. an overview description like:

```json
"info": {
    "title": "OX REST API",
    "description": "Documentation of the Open-Xchange REST API.\n\n___\n",
    "contact": {
        "name": "Open-Xchange GmbH",
        "email": "info@open-xchange.com",
        "url": "https://www.open-xchange.com/"
    },
    "version": "7.8.4"
}
```

It is located at the base level of the file structure in a folder named info.

Finally all comes together in a last index.yaml file (the base index.yaml) that is stored at the base level of the file structure:

```yaml
openapi: 3.0.0
 
info:
  $ref: ./info/index.yaml
 
servers:
  - url: https://example.com/ajax
 
tags:
  - name: module name
    description: A short description of this module.
  
paths:
   source: ./paths/
 
components:
  schemas:
   $ref: ./components/schemas/index.yaml
 
  parameters:
   $ref: ./components/parameters/index.yaml
 
  requestBodies:
   $ref: ./components/requestBodies/index.yaml
 ```

The final structure of the folder looks as follows:

```
http_api
    |— paths
    |— components   
        |— parameters
        |— requestBodies
        |— responses
        |— schemas
    |— info
        |— index.yaml (info-index.yaml)
    |— index.yaml (base Index.yaml)
```

#### Short introduction to the specification

[This](https://confluence.open-xchange.com/x/e4ABE) article provides step-by-step instructions to extend/change the documentation of the HTTP API.


### Swagger Editor

The [Swagger Editor](http://editor.swagger.io) can be used to develop and validate a Swagger definition of an API. 
In the editor view (left) you can write YAML code that has to fit the Swagger specification and in the presentation
view (right) you see the final documentation (this presentation format may differ from visualization tool to visualization tool).
The generated _swagger.json_ from above can be imported over the Swagger Editor menu bar "File" > "Paste JSON...".
The generation of a client API is not possible from this editor due to template changes that must be necessary.

The advantage of the editor is that errors are visualized after loading a _openApi.json_. Next to this it is possible to jump
to the error location which makes it easier to identify positions that do not match the specification.

**NOTE: The current Swagger Editor may not support the syntax of our requests and can show errors.**


## Client API generation using Openapi-generator

### Codegen introduction

With [Openapi-generator](https://github.com/OpenAPITools/openapi-generator) it is possible to generate client APIs using a
previously created _openApi.json_ file. 

Example: Generation of a Java Client API from http-api directory

```
gradle :http-api:clean :http-api:build
```


### Configure Codegen

It is possible to configure the code generation with a few properties. These properties are
stored in a JSON file. Existing configuration files are for example `/http-api/client-gen/config/http_api.json ` and
`/http-api/client-gen/config/rest_api.json `. These configuration files must be stored individually for each API that
is described using OpenAPI. It follows a sample config file for the Java Client API generation of OX HTTP API:

```json
{
    "modelPackage" : "com.openexchange.testing.httpclient.models",
    "apiPackage"   : "com.openexchange.testing.httpclient.modules",
    "invokerPackage" : "com.openexchange.testing.httpclient.invoker",
    "groupId" : "com.openexchange.testing",
    "artifactId"  : "httpclient",
    "fullJavaUtil" : true,
    "dateLibrary" : "java8",
    "java8" : true
}
```

The properties are language specific and must not exist for each Codegen configuration. Here, the primary
options are the ones for the packages the java files are stored in. The package description uses the common package
structure. The package for the API classes is `modules` and the one for the `ApiClient` and related classes is `invoker`. Response
and request body models are placed in the `models` package.

### Specify operation identifiers and tags

For code generation it is important to specify `operationId`s in the definition of a request. The `operationId`
determines the name of the request's method in the client API. Otherwise the name might be illegible. The following example
shows the allocation of an `operationId`:

```yaml
/resource?action=get:
  get:
    operationId: getResource
    tags:
      - resources
    #...
```

Additionally a tag should be specified with the name of the related module. The Openapi-generator uses the tag
to name the API class of a certain module. Furthermore the tag is responsible for grouping of requests (e.g. when the _openApi.json_
is visualized as web documentation).

### Snippet: Send a mail

```java
package main;

import java.util.Arrays;

import com.openexchange.swagger.httpapi.invoker.ApiClient;
import com.openexchange.swagger.httpapi.invoker.ApiException;
import com.openexchange.swagger.httpapi.invoker.Configuration;
import com.openexchange.swagger.httpapi.models.LoginResponse;
import com.openexchange.swagger.httpapi.models.MailAttachment;
import com.openexchange.swagger.httpapi.models.SendMailData;
import com.openexchange.swagger.httpapi.modules.LoginApi;
import com.openexchange.swagger.httpapi.modules.MailApi;

public class OXHttpJavaClientApiTest {

	public static void main(String[] args) {
		final ApiClient apiClient = Configuration.getDefaultApiClient();
		apiClient.setBasePath("https://example.com/appsuite/api");
		
		final LoginApi loginApi = new LoginApi(apiClient);
		final MailApi mailApi = new MailApi(apiClient);
		
		try {
			// log into App Suite
			final LoginResponse loginResponse = loginApi.doLogin("username", "password", null, null, null, null, null);
			// successfully logged in?
			if(loginResponse.getSession() != null && !loginResponse.getSession().isEmpty()) {
				System.out.println("Successfully logged in! Your session: " + loginResponse.getSession());
				
				// build a mail
				final SendMailData mail = new SendMailData();
				mail.setFrom(Arrays.asList(Arrays.asList("Foo Bar", "foo.bar@example.com")));
				mail.setTo(Arrays.asList(Arrays.asList("Bar Foo", "bar.foo@example.com")));
				mail.setSubject("Hello World!");
				mail.setSendtype(0);
				// create the message
				final MailAttachment mailAttachment = new MailAttachment();
				mailAttachment.setContent("<p><b>Hello World!</b></p><p>This is Java Client API speaking.</p>");
				mailAttachment.setContentType("ALTERNATIVE");
				mailAttachment.setDisp("inline");
				mail.setAttachments(Arrays.asList(mailAttachment));
				
				// send the mail
				final String strResult = mailApi.sendMail(loginResponse.getSession(), mail.toJson(), null);
				System.out.println(strResult);
			}
			else {
				System.err.println("Login failed! " + loginResponse.toJson());
			}
		} catch (ApiException e) {
			System.err.println(e);
		}
	}

}
```

### Snippet: Usage of the multiple module

```java
package main;

import java.util.Arrays;
import java.util.List;

import com.openexchange.clientapi.http.invoker.ApiException;
import com.openexchange.clientapi.http.models.LoginResponse;
import com.openexchange.clientapi.http.models.ReminderUpdateBody;
import com.openexchange.clientapi.http.models.SingleRequest;
import com.openexchange.clientapi.http.models.SingleResponse;
import com.openexchange.clientapi.http.modules.LoginApi;
import com.openexchange.clientapi.http.modules.MultipleApi;

public class OXHttpJavaClientApiTest {

	public static void main(String[] args) {
		final ApiClient apiClient = Configuration.getDefaultApiClient();
		apiClient.setBasePath("https://example.com/appsuite/api");
		
		final LoginApi loginApi = new LoginApi(apiClient);
		final MultipleApi multipleApi = new MultipleApi(apiClient);
		
		try {
			// log into App Suite
			final LoginResponse loginResponse = loginApi.doLogin("username", "password", null, null, null, null, null);
			// successfully logged in?
			if(loginResponse.getSession() != null && !loginResponse.getSession().isEmpty()) {
				System.out.println("Successfully logged in! Your session: " + loginResponse.getSession());
			
				// process a reminder range and update request at once
				List<SingleResponse> responses = multipleApi.process(loginResponse.getSession(), Arrays.asList(
						new SingleRequest().module("reminder").action("range").end(1497461067180L),
						new SingleRequest().module("reminder").action("remindAgain").id("51").data(new ReminderUpdateBody().alarm(1429478800000L))
					), null);
				System.out.println(responses);
			}
			else {
				System.err.println("Login failed! " + loginResponse.toJson());
			}
		} catch (ApiException e) {
			System.err.println(e);
		}
	}

}
```