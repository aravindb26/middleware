/*
 * @copyright Copyright (c) OX Software GmbH, Germany <info@open-xchange.com>
 * @license AGPL-3.0
 *
 * This code is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OX App Suite.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>.
 *
 * Any use of the work other than as authorized under this license or copyright law is prohibited.
 *
 */

package com.openexchange.ajax.chronos;

import java.io.File;
import java.lang.reflect.Type;
import java.util.List;
import com.google.gson.reflect.TypeToken;
import com.openexchange.testing.httpclient.invoker.ApiCallback;
import com.openexchange.testing.httpclient.invoker.ApiClient;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.invoker.ApiResponse;
import com.openexchange.testing.httpclient.invoker.Pair;
import com.openexchange.testing.httpclient.modules.ChronosApi;
import okhttp3.Call;

/**
 * {@link EnhancedChronosApi}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.4
 */
public class EnhancedChronosApi extends ChronosApi {

    private final ApiClient localClient;

    /**
     * Initializes a new {@link EnhancedChronosApi}.
     */
    public EnhancedChronosApi(ApiClient client) {
        super(client);
        this.localClient = client;
    }
    
    /**
     * Creates an event and attaches drive referenced files.
     * 
     * #### Note It is possible to create multiple attachments at once. Therefore add additional form fields and replace \&quot;[index]\&quot; in &#x60;file_[index]&#x60;  with the appropriate index, like &#x60;file_1&#x60;. The index always starts with 0 (mandatory attachment object). There can only be one json payload describing the EventData, the rest json payloads (if present) will simply be ignored. 
     * @param session The groupware session
     * @param folder ID of the folder who contains the events. (required)
     * @param json0 A JSON object containing the event&#39;s data as described in [EventData](#/definitions/EventData). (required)
     * @param checkConflicts Whether to check for conflicts or not. (optional, default to false)
     * @param scheduling Controls the generation of scheduling messages and notification mails for  the current operation. (optional, default to all)
     * @param extendedEntities If set to &#39;true&#39; attendees of internal users will be extended by a &#39;contact&#39; field, which contains some of the contact fields of this user.  (optional, default to false)
     * @param usedGroups In case the client resolves groups into single attendees the client can provide the ids  of the groups he used (resolved) as a comma separated list. This way the usecount of those groups will be increased.  (optional)
     * @return String
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> A HTML page containing the newly created event or in case of errors an error object (see [File uploads](https://documentation.open-xchange.com/latest/middleware/http_api/1_introduction.html#file-uploads) as an example). If the event could not be created due to conflicts, the HTML Page contains an object with the field &#x60;conflicts&#x60;, which holds informations about the conflict.  </td><td>  -  </td></tr>
        <tr><td> 401 </td><td> In case the oauth token is missing or is invalid </td><td>  * WWW-Authenticate - Contains the required scheme (\&quot;Bearer\&quot;) and in case the token is invalid also the error. E.g.: Bearer,error&#x3D;\&quot;invalid token\&quot;,error_description&#x3D;\&quot;The token has expired\&quot; <br>  </td></tr>
        <tr><td> 403 </td><td> In case the scope of the oauth token is insufficient </td><td>  -  </td></tr>
     </table>
     */
    public String createEventWithAttachments(String session, String folder, String json0, Boolean checkConflicts, String scheduling, Boolean extendedEntities, String usedGroups) throws ApiException {
        ApiResponse<String> localVarResp = createEventWithAttachmentsWithHttpInfo(session, folder, json0, checkConflicts, scheduling, extendedEntities, usedGroups);
        return localVarResp.getData();
    }
    
    /**
     * Creates an event and attaches files.
     * 
     * #### Note It is possible to create multiple attachments at once. Therefore add additional form fields and replace \&quot;[index]\&quot; in &#x60;file_[index]&#x60;  with the appropriate index, like &#x60;file_1&#x60;. The index always starts with 0 (mandatory attachment object). There can only be one json payload describing the EventData, the rest json payloads (if present) will simply be ignored. 
     * @param session The groupware session
     * @param folder ID of the folder who contains the events. (required)
     * @param json0 A JSON object containing the event&#39;s data as described in [EventData](#/definitions/EventData). (required)
     * @param checkConflicts Whether to check for conflicts or not. (optional, default to false)
     * @param scheduling Controls the generation of scheduling messages and notification mails for  the current operation. (optional, default to all)
     * @param extendedEntities If set to &#39;true&#39; attendees of internal users will be extended by a &#39;contact&#39; field, which contains some of the contact fields of this user.  (optional, default to false)
     * @param usedGroups In case the client resolves groups into single attendees the client can provide the ids  of the groups he used (resolved) as a comma separated list. This way the usecount of those groups will be increased.  (optional)
     * @return ApiResponse&lt;String&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> A HTML page containing the newly created event or in case of errors an error object (see [File uploads](https://documentation.open-xchange.com/latest/middleware/http_api/1_introduction.html#file-uploads) as an example). If the event could not be created due to conflicts, the HTML Page contains an object with the field &#x60;conflicts&#x60;, which holds informations about the conflict.  </td><td>  -  </td></tr>
        <tr><td> 401 </td><td> In case the oauth token is missing or is invalid </td><td>  * WWW-Authenticate - Contains the required scheme (\&quot;Bearer\&quot;) and in case the token is invalid also the error. E.g.: Bearer,error&#x3D;\&quot;invalid token\&quot;,error_description&#x3D;\&quot;The token has expired\&quot; <br>  </td></tr>
        <tr><td> 403 </td><td> In case the scope of the oauth token is insufficient </td><td>  -  </td></tr>
     </table>
     */
    public ApiResponse<String> createEventWithAttachmentsWithHttpInfo(String session, String folder, String json0, Boolean checkConflicts, String scheduling, Boolean extendedEntities, String usedGroups) throws ApiException {
        okhttp3.Call localVarCall = createEventWithAttachmentsValidateBeforeCall(session, folder, json0, checkConflicts, scheduling, extendedEntities, usedGroups, null);
        Type localVarReturnType = new TypeToken<String>() {}.getType();
        return localClient.execute(localVarCall, localVarReturnType);
    }
    
    private okhttp3.Call createEventWithAttachmentsValidateBeforeCall(String session, String folder, String json0, Boolean checkConflicts, String scheduling, Boolean extendedEntities, String usedGroups, final ApiCallback<?> _callback) throws ApiException {

        // verify the required parameter 'session' is set
        if (session == null) {
            throw new ApiException("Missing the required parameter 'session' when calling createEventWithAttachments(Async)");
        }

        // verify the required parameter 'folder' is set
        if (folder == null) {
            throw new ApiException("Missing the required parameter 'folder' when calling createEventWithAttachments(Async)");
        }

        // verify the required parameter 'json0' is set
        if (json0 == null) {
            throw new ApiException("Missing the required parameter 'json0' when calling createEventWithAttachments(Async)");
        }

        okhttp3.Call localVarCall = createEventWithAttachmentsCall(session, folder, json0, checkConflicts, scheduling, extendedEntities, usedGroups, _callback);
        return localVarCall;

    }
    

    /**
     * Build call for createEventWithAttachments
     * 
     * @param session The groupware session
     * @param folder ID of the folder who contains the events. (required)
     * @param json0 A JSON object containing the event&#39;s data as described in [EventData](#/definitions/EventData). (required)
     * @param checkConflicts Whether to check for conflicts or not. (optional, default to false)
     * @param scheduling Controls the generation of scheduling messages and notification mails for  the current operation. (optional, default to all)
     * @param extendedEntities If set to &#39;true&#39; attendees of internal users will be extended by a &#39;contact&#39; field, which contains some of the contact fields of this user.  (optional, default to false)
     * @param usedGroups In case the client resolves groups into single attendees the client can provide the ids  of the groups he used (resolved) as a comma separated list. This way the usecount of those groups will be increased.  (optional)
     * @param _callback Callback for upload/download progress
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> A HTML page containing the newly created event or in case of errors an error object (see [File uploads](https://documentation.open-xchange.com/latest/middleware/http_api/1_introduction.html#file-uploads) as an example). If the event could not be created due to conflicts, the HTML Page contains an object with the field &#x60;conflicts&#x60;, which holds informations about the conflict.  </td><td>  -  </td></tr>
        <tr><td> 401 </td><td> In case the oauth token is missing or is invalid </td><td>  * WWW-Authenticate - Contains the required scheme (\&quot;Bearer\&quot;) and in case the token is invalid also the error. E.g.: Bearer,error&#x3D;\&quot;invalid token\&quot;,error_description&#x3D;\&quot;The token has expired\&quot; <br>  </td></tr>
        <tr><td> 403 </td><td> In case the scope of the oauth token is insufficient </td><td>  -  </td></tr>
     </table>
     */
    public Call createEventWithAttachmentsCall(String session, String folder, String json0, Boolean checkConflicts, String scheduling, Boolean extendedEntities, String usedGroups, ApiCallback<?> _callback) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/chronos?action=new";

        java.util.List<Pair> localVarQueryParams = new java.util.ArrayList<Pair>();
        java.util.List<Pair> localVarCollectionQueryParams = new java.util.ArrayList<Pair>();
        if (session != null) {
            localVarQueryParams.addAll(localClient.parameterToPair("session", session));
        }

        if (folder != null) {
            localVarQueryParams.addAll(localClient.parameterToPair("folder", folder));
        }

        if (checkConflicts != null) {
            localVarQueryParams.addAll(localClient.parameterToPair("checkConflicts", checkConflicts));
        }

        if (scheduling != null) {
            localVarQueryParams.addAll(localClient.parameterToPair("scheduling", scheduling));
        }

        if (extendedEntities != null) {
            localVarQueryParams.addAll(localClient.parameterToPair("extendedEntities", extendedEntities));
        }

        if (usedGroups != null) {
            localVarQueryParams.addAll(localClient.parameterToPair("usedGroups", usedGroups));
        }

        java.util.Map<String, String> localVarHeaderParams = new java.util.HashMap<String, String>();
        java.util.Map<String, String> localVarCookieParams = new java.util.HashMap<String, String>();
        java.util.Map<String, Object> localVarFormParams = new java.util.HashMap<String, Object>();
        if (json0 != null) {
            localVarFormParams.put("json_0", json0);
        }

        final String[] localVarAccepts = { "text/html" };
        final String localVarAccept = localClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = { "multipart/form-data" };
        final String localVarContentType = localClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        String[] localVarAuthNames = new String[] {};
        return localClient.buildCall(localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    /**
     * Creates an event and attaches files.
     * 
     * #### Note It is possible to create multiple attachments at once. Therefore add additional form fields and replace \&quot;[index]\&quot; in &#x60;file_[index]&#x60;  with the appropriate index, like &#x60;file_1&#x60;. The index always starts with 0 (mandatory attachment object). There can only be one json payload describing the EventData, the rest json payloads (if present) will simply be ignored. 
     * @param session The groupware session
     * @param folder ID of the folder who contains the events. (required)
     * @param json0 A JSON object containing the event&#39;s data as described in [EventData](#/definitions/EventData). (required)
     * @param files A list with attachment files as per &#x60;&lt;input type&#x3D;\\\&quot;file\\\&quot; /&gt;&#x60;. (required)
     * @param checkConflicts Whether to check for conflicts or not. (optional, default to false)
     * @param scheduling Controls the generation of scheduling messages and notification mails for  the current operation. (optional, default to all)
     * @param extendedEntities If set to &#39;true&#39; attendees of internal users will be extended by a &#39;contact&#39; field, which contains some of the contact fields of this user.  (optional, default to false)
     * @param usedGroups In case the client resolves groups into single attendees the client can provide the ids  of the groups he used (resolved) as a comma separated list. This way the usecount of those groups will be increased.  (optional)
     * @return String
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> A HTML page containing the newly created event or in case of errors an error object (see [File uploads](https://documentation.open-xchange.com/latest/middleware/http_api/1_introduction.html#file-uploads) as an example). If the event could not be created due to conflicts, the HTML Page contains an object with the field &#x60;conflicts&#x60;, which holds informations about the conflict.  </td><td>  -  </td></tr>
        <tr><td> 401 </td><td> In case the oauth token is missing or is invalid </td><td>  * WWW-Authenticate - Contains the required scheme (\&quot;Bearer\&quot;) and in case the token is invalid also the error. E.g.: Bearer,error&#x3D;\&quot;invalid token\&quot;,error_description&#x3D;\&quot;The token has expired\&quot; <br>  </td></tr>
        <tr><td> 403 </td><td> In case the scope of the oauth token is insufficient </td><td>  -  </td></tr>
     </table>
     */
    public String createEventWithAttachments(String session, String folder, String json0, List<File> files, Boolean checkConflicts, String scheduling, Boolean extendedEntities, String usedGroups) throws ApiException {
        ApiResponse<String> localVarResp = createEventWithAttachmentsWithHttpInfo(session, folder, json0, files, checkConflicts, scheduling, extendedEntities, usedGroups);
        return localVarResp.getData();
    }

    /**
     * Build call for createEventWithAttachments
     * 
     * @param session The groupware session
     * @param folder ID of the folder who contains the events. (required)
     * @param json0 A JSON object containing the event&#39;s data as described in [EventData](#/definitions/EventData). (required)
     * @param files A list with attachment files as per &#x60;&lt;input type&#x3D;\\\&quot;file\\\&quot; /&gt;&#x60;. (required)
     * @param checkConflicts Whether to check for conflicts or not. (optional, default to false)
     * @param scheduling Controls the generation of scheduling messages and notification mails for  the current operation. (optional, default to all)
     * @param extendedEntities If set to &#39;true&#39; attendees of internal users will be extended by a &#39;contact&#39; field, which contains some of the contact fields of this user.  (optional, default to false)
     * @param usedGroups In case the client resolves groups into single attendees the client can provide the ids  of the groups he used (resolved) as a comma separated list. This way the usecount of those groups will be increased.  (optional)
     * @param _callback Callback for upload/download progress
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> A HTML page containing the newly created event or in case of errors an error object (see [File uploads](https://documentation.open-xchange.com/latest/middleware/http_api/1_introduction.html#file-uploads) as an example). If the event could not be created due to conflicts, the HTML Page contains an object with the field &#x60;conflicts&#x60;, which holds informations about the conflict.  </td><td>  -  </td></tr>
        <tr><td> 401 </td><td> In case the oauth token is missing or is invalid </td><td>  * WWW-Authenticate - Contains the required scheme (\&quot;Bearer\&quot;) and in case the token is invalid also the error. E.g.: Bearer,error&#x3D;\&quot;invalid token\&quot;,error_description&#x3D;\&quot;The token has expired\&quot; <br>  </td></tr>
        <tr><td> 403 </td><td> In case the scope of the oauth token is insufficient </td><td>  -  </td></tr>
     </table>
     */
    public Call createEventWithAttachmentsCall(String session, String folder, String json0, List<File> files, Boolean checkConflicts, String scheduling, Boolean extendedEntities, String usedGroups, ApiCallback<?> _callback) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/chronos?action=new";

        java.util.List<Pair> localVarQueryParams = new java.util.ArrayList<Pair>();
        java.util.List<Pair> localVarCollectionQueryParams = new java.util.ArrayList<Pair>();
        if (session != null) {
            localVarQueryParams.addAll(localClient.parameterToPair("session", session));
        }

        if (folder != null) {
            localVarQueryParams.addAll(localClient.parameterToPair("folder", folder));
        }

        if (checkConflicts != null) {
            localVarQueryParams.addAll(localClient.parameterToPair("checkConflicts", checkConflicts));
        }

        if (scheduling != null) {
            localVarQueryParams.addAll(localClient.parameterToPair("scheduling", scheduling));
        }

        if (extendedEntities != null) {
            localVarQueryParams.addAll(localClient.parameterToPair("extendedEntities", extendedEntities));
        }

        if (usedGroups != null) {
            localVarQueryParams.addAll(localClient.parameterToPair("usedGroups", usedGroups));
        }

        java.util.Map<String, String> localVarHeaderParams = new java.util.HashMap<String, String>();
        java.util.Map<String, String> localVarCookieParams = new java.util.HashMap<String, String>();
        java.util.Map<String, Object> localVarFormParams = new java.util.HashMap<String, Object>();
        if (json0 != null) {
            localVarFormParams.put("json_0", json0);
        }

        if (files != null) {
            int index = 0;
            for (File f : files) {
                localVarFormParams.put("file_" + index++, f);
            }
        }

        final String[] localVarAccepts = { "text/html" };
        final String localVarAccept = localClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = { "multipart/form-data" };
        final String localVarContentType = localClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        String[] localVarAuthNames = new String[] {};
        return localClient.buildCall(localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    /**
     * Creates an event and attaches files.
     * 
     * #### Note It is possible to create multiple attachments at once. Therefore add additional form fields and replace \&quot;[index]\&quot; in &#x60;file_[index]&#x60;  with the appropriate index, like &#x60;file_1&#x60;. The index always starts with 0 (mandatory attachment object). There can only be one json payload describing the EventData, the rest json payloads (if present) will simply be ignored. 
     * @param session The groupware session
     * @param folder ID of the folder who contains the events. (required)
     * @param json0 A JSON object containing the event&#39;s data as described in [EventData](#/definitions/EventData). (required)
     * @param files A list with attachment files as per &#x60;&lt;input type&#x3D;\\\&quot;file\\\&quot; /&gt;&#x60;. (required)
     * @param checkConflicts Whether to check for conflicts or not. (optional, default to false)
     * @param scheduling Controls the generation of scheduling messages and notification mails for  the current operation. (optional, default to all)
     * @param extendedEntities If set to &#39;true&#39; attendees of internal users will be extended by a &#39;contact&#39; field, which contains some of the contact fields of this user.  (optional, default to false)
     * @param usedGroups In case the client resolves groups into single attendees the client can provide the ids  of the groups he used (resolved) as a comma separated list. This way the usecount of those groups will be increased.  (optional)
     * @return ApiResponse&lt;String&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> A HTML page containing the newly created event or in case of errors an error object (see [File uploads](https://documentation.open-xchange.com/latest/middleware/http_api/1_introduction.html#file-uploads) as an example). If the event could not be created due to conflicts, the HTML Page contains an object with the field &#x60;conflicts&#x60;, which holds informations about the conflict.  </td><td>  -  </td></tr>
        <tr><td> 401 </td><td> In case the oauth token is missing or is invalid </td><td>  * WWW-Authenticate - Contains the required scheme (\&quot;Bearer\&quot;) and in case the token is invalid also the error. E.g.: Bearer,error&#x3D;\&quot;invalid token\&quot;,error_description&#x3D;\&quot;The token has expired\&quot; <br>  </td></tr>
        <tr><td> 403 </td><td> In case the scope of the oauth token is insufficient </td><td>  -  </td></tr>
     </table>
     */
    public ApiResponse<String> createEventWithAttachmentsWithHttpInfo(String session, String folder, String json0, List<File> files, Boolean checkConflicts, String scheduling, Boolean extendedEntities, String usedGroups) throws ApiException {
        okhttp3.Call localVarCall = createEventWithAttachmentsValidateBeforeCall(session, folder, json0, files, checkConflicts, scheduling, extendedEntities, usedGroups, null);
        Type localVarReturnType = new TypeToken<String>() {}.getType();
        return localClient.execute(localVarCall, localVarReturnType);
    }

    private okhttp3.Call createEventWithAttachmentsValidateBeforeCall(String session, String folder, String json0, List<File> files, Boolean checkConflicts, String scheduling, Boolean extendedEntities, String usedGroups, final ApiCallback<?> _callback) throws ApiException {

        // verify the required parameter 'session' is set
        if (session == null) {
            throw new ApiException("Missing the required parameter 'session' when calling createEventWithAttachments(Async)");
        }

        // verify the required parameter 'folder' is set
        if (folder == null) {
            throw new ApiException("Missing the required parameter 'folder' when calling createEventWithAttachments(Async)");
        }

        // verify the required parameter 'json0' is set
        if (json0 == null) {
            throw new ApiException("Missing the required parameter 'json0' when calling createEventWithAttachments(Async)");
        }

        // verify the required parameter 'file0' is set
        if (files == null || files.isEmpty()) {
            throw new ApiException("Missing the required parameter 'file0' when calling createEventWithAttachments(Async)");
        }

        okhttp3.Call localVarCall = createEventWithAttachmentsCall(session, folder, json0, files, checkConflicts, scheduling, extendedEntities, usedGroups, _callback);
        return localVarCall;

    }

    /**
     * Creates an event and attaches files. (asynchronously)
     * #### Note It is possible to create multiple attachments at once. Therefore add additional form fields and replace \&quot;[index]\&quot; in &#x60;file_[index]&#x60;  with the appropriate index, like &#x60;file_1&#x60;. The index always starts with 0 (mandatory attachment object). There can only be one json payload describing the EventData, the rest json payloads (if present) will simply be ignored. 
     * @param folder ID of the folder who contains the events. (required)
     * @param json0 A JSON object containing the event&#39;s data as described in [EventData](#/definitions/EventData). (required)
     * @param file0 The attachment file as per &#x60;&lt;input type&#x3D;\\\&quot;file\\\&quot; /&gt;&#x60;. (required)
     * @param checkConflicts Whether to check for conflicts or not. (optional, default to false)
     * @param scheduling Controls the generation of scheduling messages and notification mails for  the current operation. (optional, default to all)
     * @param extendedEntities If set to &#39;true&#39; attendees of internal users will be extended by a &#39;contact&#39; field, which contains some of the contact fields of this user.  (optional, default to false)
     * @param usedGroups In case the client resolves groups into single attendees the client can provide the ids  of the groups he used (resolved) as a comma separated list. This way the usecount of those groups will be increased.  (optional)
     * @param _callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> A HTML page containing the newly created event or in case of errors an error object (see [File uploads](https://documentation.open-xchange.com/latest/middleware/http_api/1_introduction.html#file-uploads) as an example). If the event could not be created due to conflicts, the HTML Page contains an object with the field &#x60;conflicts&#x60;, which holds informations about the conflict.  </td><td>  -  </td></tr>
        <tr><td> 401 </td><td> In case the oauth token is missing or is invalid </td><td>  * WWW-Authenticate - Contains the required scheme (\&quot;Bearer\&quot;) and in case the token is invalid also the error. E.g.: Bearer,error&#x3D;\&quot;invalid token\&quot;,error_description&#x3D;\&quot;The token has expired\&quot; <br>  </td></tr>
        <tr><td> 403 </td><td> In case the scope of the oauth token is insufficient </td><td>  -  </td></tr>
     </table>
     */
    @Override
    public Call createEventWithAttachmentsAsync(String folder, String json0, File file0, Boolean checkConflicts, String scheduling, Boolean extendedEntities, String usedGroups, ApiCallback<String> _callback) throws ApiException {
        return super.createEventWithAttachmentsAsync(folder, json0, file0, checkConflicts, scheduling, extendedEntities, usedGroups, _callback);
    }

}
