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

package com.openexchange.drive.json.action;

import static com.openexchange.osgi.Tools.requireService;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXActionService;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction;
import com.openexchange.capabilities.CapabilityService;
import com.openexchange.config.ConfigurationService;
import com.openexchange.dispatcher.DispatcherPrefixService;
import com.openexchange.drive.DriveClientVersion;
import com.openexchange.drive.DriveExceptionCodes;
import com.openexchange.drive.DriveService;
import com.openexchange.drive.DriveSession;
import com.openexchange.drive.events.subscribe.DriveSubscriptionStore;
import com.openexchange.drive.events.subscribe.Subscription;
import com.openexchange.drive.events.subscribe.SubscriptionMode;
import com.openexchange.drive.json.DriveShareJSONParser;
import com.openexchange.drive.json.internal.DefaultDriveSession;
import com.openexchange.drive.json.internal.Services;
import com.openexchange.drive.json.json.DriveFieldMapper;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.FolderService;
import com.openexchange.folderstorage.FolderStorage;
import com.openexchange.folderstorage.database.contentType.InfostoreContentType;
import com.openexchange.folderstorage.type.PrivateType;
import com.openexchange.framework.request.RequestContextHolder;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.notify.hostname.HostData;
import com.openexchange.groupware.notify.hostname.HostnameService;
import com.openexchange.i18n.LocaleTools;
import com.openexchange.java.Enums;
import com.openexchange.java.Strings;
import com.openexchange.share.ShareService;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.servlet.CountingHttpServletRequest;
import com.openexchange.tools.servlet.http.Tools;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link AbstractDriveAction}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
@RestrictedAction(module = AbstractDriveAction.MODULE, type = RestrictedAction.Type.READ)
public abstract class AbstractDriveAction implements AJAXActionService {

    protected static final String MODULE = "drive";

    private final DriveShareJSONParser parser;

    /**
     * Initializes a new {@link AbstractDriveAction}.
     */
    protected AbstractDriveAction() {
        super();
        this.parser = new DriveShareJSONParser();
    }

    /**
     * Gets the drive share parser.
     *
     * @return The parser
     */
    protected DriveShareJSONParser getShareParser() {
        return parser;
    }

    /**
     * Gets the drive service.
     *
     * @return The drive service
     * @throws OXException if the service is unavailable
     */
    protected DriveService getDriveService() throws OXException {
        return requireService(DriveService.class, Services.get());
    }

    /**
     * Gets the default share service.
     *
     * @return The share service
     * @throws OXException if the service is unavailable
     */
    protected ShareService getShareService() throws OXException {
        return requireService(ShareService.class, Services.get());
    }

    protected DriveSubscriptionStore getSubscriptionStore() throws OXException {
        return Services.getService(DriveSubscriptionStore.class, true);
    }

    protected ConfigurationService getConfigService() throws OXException {
        return Services.getService(ConfigurationService.class, true);
    }

    protected abstract AJAXRequestResult doPerform(AJAXRequestData requestData, DefaultDriveSession session) throws OXException;

    protected boolean requiresRootFolderID() {
        return true;
    }

    @Override
    public AJAXRequestResult perform(AJAXRequestData requestData, ServerSession session) throws OXException {
        /*
         * check module permissions
         */
        {
            CapabilityService capabilityService = Services.getService(CapabilityService.class, true);
            if (false == capabilityService.getCapabilities(session).contains(DriveService.CAPABILITY_DRIVE)) {
                throw AjaxExceptionCodes.NO_PERMISSION_FOR_MODULE.create("drive");
            }
        }
        /*
         * Create drive session
         */
        String rootFolderID = requestData.getParameter("root");
        if (requiresRootFolderID() && Strings.isEmpty(rootFolderID)) {
            throw AjaxExceptionCodes.MISSING_PARAMETER.create("root");
        }
        int apiVersion = getAPIVersion(requestData);
        DefaultDriveSession driveSession = new DefaultDriveSession(session, rootFolderID, extractHostData(requestData, session), apiVersion, extractClientVersion(requestData), extractLocale(requestData));
        /*
         * extract device name information if present
         */
        String device = requestData.getParameter("device");
        if (Strings.isNotEmpty(device)) {
            driveSession.setDeviceName(device);
        }
        /*
         * extract meta override parameter if present
         */
        if (requestData.containsParameter("driveMeta")) {
            driveSession.setDriveMeta(requestData.getParameter("driveMeta"));
        }
        /*
         * extract push token if present
         */
        String pushToken = requestData.getParameter("pushToken");
        if (Strings.isNotEmpty(pushToken)) {
            session.setParameter(DriveSession.PARAMETER_PUSH_TOKEN, pushToken);
        }
        /*
         * extract diagnostics parameter if present
         */
        String diagnostics = requestData.getParameter("diagnostics");
        if (Strings.isNotEmpty(diagnostics)) {
            driveSession.setDiagnostics(Boolean.valueOf(diagnostics));
        }
        /*
         * extract quota parameter if present
         */
        String quota = requestData.getParameter("quota");
        if (Strings.isNotEmpty(quota)) {
            driveSession.setIncludeQuota(Boolean.parseBoolean(quota));
        }
        /*
         * extract columns parameter to fields if present
         */
        String columnsValue = requestData.getParameter("columns");
        if (Strings.isNotEmpty(columnsValue)) {
            String[] splitted = Strings.splitByComma(columnsValue);
            int[] columnIDs = new int[splitted.length];
            for (int i = 0; i < splitted.length; i++) {
                try {
                    columnIDs[i] = Integer.parseInt(splitted[i]);
                } catch (NumberFormatException e) {
                    throw AjaxExceptionCodes.INVALID_PARAMETER_VALUE.create("columns");
                }
            }
            driveSession.setFields(Arrays.asList(DriveFieldMapper.getInstance().getFields(columnIDs)));
        }
        /*
         * perform
         */
        return doPerform(requestData, driveSession);
    }

    /**
     * Extracts the value of the 'apiVersion' URL parameter if present.
     *
     * @param requestData The {@link AJAXRequestData}
     * @return The value of the 'apiVersion' URL parameter. If the parameter is absent 0 is returned.
     * @throws OXException if value coercion fails or if the parameter contains an invalid value
     */
    private int getAPIVersion(AJAXRequestData requestData) throws OXException {
        if (false == requestData.containsParameter("apiVersion")) {
            return 0;
        }
        String apiVersion = requestData.getParameter("apiVersion");
        if (null == apiVersion) {
            return 0;
        }
        try {
            return Integer.parseInt(apiVersion);
        } catch (NumberFormatException e) {
            throw AjaxExceptionCodes.INVALID_PARAMETER_VALUE.create("apiVersion", "\"" + apiVersion + "\"");
        }
    }

    private static DriveClientVersion extractClientVersion(AJAXRequestData requestData) throws OXException {
        String version = requestData.containsParameter("version") ? requestData.getParameter("version") : null;
        if (Strings.isEmpty(version)) {
            return DriveClientVersion.VERSION_0;
        }
        try {
            return new DriveClientVersion(version);
        } catch (IllegalArgumentException e) {
            throw AjaxExceptionCodes.INVALID_PARAMETER_VALUE.create(e, "version", version);
        }
    }

    /**
     * Extracts a possible set locale override from the supplied request data.
     *
     * @param requestData The request data
     * @return The locale, or <code>null</code> if not set
     */
    private static Locale extractLocale(AJAXRequestData requestData) {
        Locale localeOverride = null;
        if (requestData.containsParameter("locale")) {
            localeOverride = LocaleTools.getLocale(requestData.getParameter("locale"));
        }
        if (null == localeOverride && requestData.containsParameter("language")) {
            localeOverride = LocaleTools.getLocale(requestData.getParameter("language"));
        }
        return localeOverride;
    }

    /**
     * Extracts and parses a possible subscription mode from the supplied request data.
     *
     * @param requestData The request data
     * @return The subscription mode, or <code>null</code> if not set
     */
    protected static SubscriptionMode extractSubscriptionMode(AJAXRequestData requestData) throws OXException {
        String mode = requestData.getParameter("mode");
        if (Strings.isNotEmpty(mode)) {
            try {
                return Enums.parse(SubscriptionMode.class, mode);
            } catch (IllegalArgumentException e) {
                throw AjaxExceptionCodes.INVALID_PARAMETER_VALUE.create(e, "mode", mode);
            }
        }
        return null;
    }

    /**
     * Extracts host data from the supplied request and session.
     *
     * @param requestData The AJAX request data
     * @param session The session
     * @return The extracted host data
     * @throws OXException
     */
    private static HostData extractHostData(AJAXRequestData requestData, ServerSession session) throws OXException {
        /*
         * get host data from reuest context or session parameter
         */
        com.openexchange.framework.request.RequestContext requestContext = RequestContextHolder.get();
        if (null != requestContext) {
            return requestContext.getHostData();
        }
        HostData hostData = (HostData) session.getParameter(HostnameService.PARAM_HOST_DATA);
        if (null != hostData) {
            return hostData;
        }
        /*
         * build up hostdata from request as fallback
         */
        final boolean secure = requestData.isSecure();
        final String httpSessionID = requestData.getRoute();
        final String route = Tools.extractRoute(httpSessionID);
        HttpServletRequest servletRequest = requestData.optHttpServletRequest();
        final int port = null != servletRequest ? servletRequest.getServerPort() : -1;
        final String host = determineHost(requestData, session);
        final String prefix = Services.getService(DispatcherPrefixService.class, true).getPrefix();
        return new HostData() {

            @Override
            public String getHTTPSession() {
                return httpSessionID;
            }

            @Override
            public boolean isSecure() {
                return secure;
            }

            @Override
            public String getRoute() {
                return route;
            }

            @Override
            public int getPort() {
                return port;
            }

            @Override
            public String getHost() {
                return host;
            }

            @Override
            public String getDispatcherPrefix() {
                return prefix;
            }
        };
    }

    private static String determineHost(AJAXRequestData requestData, ServerSession session) {
        String hostName = null;
        /*
         * Ask hostname service if available
         */
        HostnameService hostnameService = Services.getOptionalService(HostnameService.class);
        if (null != hostnameService) {
            if (session.getUser().isGuest()) {
                hostName = hostnameService.getGuestHostname(session.getUserId(), session.getContextId());
            } else {
                hostName = hostnameService.getHostname(session.getUserId(), session.getContextId());
            }
        }
        /*
         * Get hostname from request
         */
        if (Strings.isEmpty(hostName)) {
            hostName = requestData.getHostname();
        }
        /*
         * Get hostname from java
         */
        if (Strings.isEmpty(hostName)) {
            try {
                hostName = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (UnknownHostException e) {
                // ignore
            }
        }
        /*
         * Fall back to localhost as last resort
         */
        if (Strings.isEmpty(hostName)) {
            hostName = "localhost";
        }
        return hostName;
    }

    /**
     * Enables an unlimited body size by setting the maximum body size in the underlying {@link CountingHttpServletRequest} to
     * <code>-1</code>.
     *
     * @param requestData The AJAX request data
     */
    protected void enableUnlimitedBodySize(AJAXRequestData requestData) {
        HttpServletRequest servletRequest = requestData.optHttpServletRequest();
        if (servletRequest instanceof CountingHttpServletRequest) {
            ((CountingHttpServletRequest) servletRequest).setMax(-1);
        }
    }

    /**
     * Actively prevents additional image transformations by setting the <code>transformationNeeded</code> parameter to
     * <code>false</code> in the supplied request data reference.
     *
     * @param requestData The request data
     */
    protected void preventTransformations(AJAXRequestData requestData) {
        requestData.putParameter("transformationNeeded", String.valueOf(false));
    }

    /**
     * Extracts the root folder identifier(s) to use for the (un)subscription of push notifications, either the explicitly passed folder
     * ids, or based on the static push domain names.
     * 
     * @param requestData The request data
     * @return The root folder identifiers, or <code>null</code> if none were supplied
     */
    protected List<String> extractPushRootFolderIDs(AJAXRequestData requestData) throws OXException {
        /*
         * probe for "root" array in body
         */
        Object data = requestData.getData();
        if (null != data) {
            if (false == (data instanceof JSONObject)) {
                throw AjaxExceptionCodes.MISSING_REQUEST_BODY.create();
            }
            JSONObject dataObject = (JSONObject) data;
            JSONArray rootArray = dataObject.optJSONArray("root");
            if (null != rootArray && 0 < rootArray.length()) {
                List<String> rootFolderIDs = new ArrayList<String>(rootArray.length());
                try {
                    for (int i = 0; i < rootArray.length(); i++) {
                        rootFolderIDs.add(rootArray.getString(i));
                    }
                } catch (JSONException e) {
                    throw AjaxExceptionCodes.JSON_ERROR.create(e, e.getMessage());
                }
                return rootFolderIDs;
            }
        }
        /*
         * probe for "root" parameter
         */
        String root = requestData.getParameter("root");
        if (Strings.isNotEmpty(root)) {
            return Collections.singletonList(root);
        }
        /*
         * probe for "domain" array in body
         */
        if (null != data) {
            JSONObject dataObject = (JSONObject) data;
            JSONArray domainArray = dataObject.optJSONArray("domain");
            if (null != domainArray && 0 < domainArray.length()) {
                List<String> rootFolderIDs = new ArrayList<String>(domainArray.length());
                try {
                    for (int i = 0; i < domainArray.length(); i++) {
                        rootFolderIDs.add(getRootFromDomain(requestData.getSession(), domainArray.getString(i)));
                    }
                } catch (JSONException e) {
                    throw AjaxExceptionCodes.JSON_ERROR.create(e, e.getMessage());
                }
                return rootFolderIDs;
            }
        }
        /*
         * probe for "domain" parameter
         */
        String domain = requestData.getParameter("domain");
        if (Strings.isNotEmpty(domain)) {
            return Collections.singletonList(getRootFromDomain(requestData.getSession(), domain));
        }
        return null;
    }

    /**
     * Derives the root folder identifier from the supplied push domain value.
     * 
     * @param session The user's session
     * @param pushDomain The push domain
     * @return The root folder id
     */
    protected String getRootFromDomain(ServerSession session, String pushDomain) throws OXException {
        if (Subscription.DOMAIN_MY_FILES.equals(pushDomain)) {
            FolderService folderService = Services.getService(FolderService.class, true);
            return folderService.getDefaultFolder(session.getUser(), FolderStorage.REAL_TREE_ID, 
                InfostoreContentType.getInstance(), PrivateType.getInstance(), session, null).getID();
        }
        if (Subscription.DOMAIN_SHARED_FILES.equals(pushDomain)) {
            return String.valueOf(FolderObject.SYSTEM_USER_INFOSTORE_FOLDER_ID);
        }
        if (Subscription.DOMAIN_PUBLIC_FILES.equals(pushDomain)) {
            return String.valueOf(FolderObject.SYSTEM_PUBLIC_INFOSTORE_FOLDER_ID);
        }
        throw DriveExceptionCodes.UNEXPECTED_ERROR.create("Unknown push domain " + pushDomain);
    }

}
