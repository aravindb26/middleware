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

package com.openexchange.ajax.request;

import static com.openexchange.resource.json.ResourceWriter.writeResource;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONValue;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.fields.DataFields;
import com.openexchange.ajax.fields.SearchFields;
import com.openexchange.ajax.parser.DataParser;
import com.openexchange.ajax.requesthandler.AJAXRequestHandler;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.resource.Resource;
import com.openexchange.resource.ResourceService;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.services.ServerRequestHandlerRegistry;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.User;

/**
 * {@link ResourceRequest} - Executes a resource request.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class ResourceRequest {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ResourceRequest.class);

    private final ServerSession session;

    private Date timestamp;

    /**
     * Initializes a new {@link ResourceRequest}.
     *
     * @param session
     *            The session providing needed user data
     */
    public ResourceRequest(final ServerSession session) {
        super();
        this.session = session;
    }

    private static final String MODULE_RESOURCE = "resource";

    public Object action(final String action, final JSONObject jsonObject) throws OXException,
            JSONException {
        if (action.equalsIgnoreCase(AJAXServlet.ACTION_LIST)) {
            return actionList(jsonObject);
        } else if (action.equalsIgnoreCase(AJAXServlet.ACTION_GET)) {
            return actionGet(jsonObject);
        } else if (action.equalsIgnoreCase(AJAXServlet.ACTION_SEARCH)) {
            return actionSearch(jsonObject);
        } else if (action.equalsIgnoreCase(AJAXServlet.ACTION_ALL)) {
            return actionAll();
        } else if (action.equalsIgnoreCase(AJAXServlet.ACTION_UPDATES)) {
            return actionUpdates(jsonObject);
        } else {
            /*
             * Look-up manage request
             */
            final AJAXRequestHandler handler = ServerRequestHandlerRegistry.getInstance().getHandler(MODULE_RESOURCE,
                    action);
            if (null == handler) {
                /*
                 * No appropriate handler
                 */
                throw AjaxExceptionCodes.UNKNOWN_ACTION.create( action);
            }
            /*
             * ... and delegate to manage request
             */
            final AJAXRequestResult result = handler.performAction(action, jsonObject, session, session.getContext());
            timestamp = result.getTimestamp();
            return result.getResultObject();
        }
    }

    private JSONValue actionUpdates(final JSONObject jsonObj)  throws OXException, JSONException {
        final Date lastModified = DataParser.checkDate(jsonObj, AJAXServlet.PARAMETER_TIMESTAMP);
        Resource[] updatedResources = null;
        Resource[] deletedResources = null;
        try {
            final ResourceService resService = ServerServiceRegistry.getServize(ResourceService.class, true);
            updatedResources = resService .listModified(lastModified, session.getContext());
            deletedResources = resService.listDeleted(lastModified, session.getContext());
        } catch (OXException exc) {
            LOG.debug("Tried to find resources that were modified since {}", lastModified, exc);
        }

        final JSONArray modified = new JSONArray();
        long lm = 0;
        if (updatedResources != null){
            for(final Resource res: updatedResources){
                if (res.getLastModified().getTime() > lm) {
                    lm = res.getLastModified().getTime();
                }
                modified.put(writeResource(session, res));
            }
        }

        final JSONArray deleted = new JSONArray();
        if (deletedResources != null){
            for(final Resource res: deletedResources){
                if (res.getLastModified().getTime() > lm) {
                    lm = res.getLastModified().getTime();
                }

                deleted.put(writeResource(session, res));
            }
        }
        timestamp = new Date(lm);

        final JSONObject retVal = new JSONObject();
        retVal.put("modified", modified);
        retVal.put("new", modified);
        retVal.put("deleted", deleted);

        return retVal;
    }

    private JSONArray actionList(final JSONObject jsonObj) throws OXException, JSONException {
        final JSONArray jsonResponseArray = new JSONArray();

        UserStorage userStorage = null;

        final JSONArray jsonArray = DataParser.checkJSONArray(jsonObj, AJAXServlet.PARAMETER_DATA);
        final int len = jsonArray.length();
        if (len > 0) {
            long lastModified = Long.MIN_VALUE;
            for (int a = 0; a < len; a++) {
                final JSONObject jData = jsonArray.getJSONObject(a);
                final int id = DataParser.checkInt(jData, DataFields.ID);
                com.openexchange.resource.Resource r = null;

                try {
                    r = ServerServiceRegistry.getServize(ResourceService.class, true).getResource(id, session.getContext());
                } catch (OXException exc) {
                    LOG.debug("resource not found try to find id in user table", exc);
                }

                if (r == null) {
                    if (userStorage == null) {
                        userStorage = UserStorage.getInstance();
                    }

                    final User u = userStorage.getUser(id, session.getContext());

                    r = new com.openexchange.resource.Resource();
                    r.setIdentifier(u.getId());
                    r.setDisplayName(u.getDisplayName());
                    r.setLastModified(new Date(0));
                }

                if (lastModified < r.getLastModified().getTime()) {
                    lastModified = r.getLastModified().getTime();
                }

                jsonResponseArray.put(writeResource(session, r));
            }
            timestamp = new Date(lastModified);
        } else {
            timestamp = new Date(0);
        }

        return jsonResponseArray;
    }

    private JSONObject actionGet(final JSONObject jsonObj) throws OXException, JSONException {
        final int id = DataParser.checkInt(jsonObj, AJAXServlet.PARAMETER_ID);
        com.openexchange.resource.Resource r = null;
        try {
            r = ServerServiceRegistry.getServize(ResourceService.class, true).getResource(id, session.getContext());
        } catch (OXException exc) {
            LOG.debug("resource not found try to find id in user table", exc);
        }

        if (r == null) {
            final User u = UserStorage.getInstance().getUser(id, session.getContext());

            r = new com.openexchange.resource.Resource();
            r.setIdentifier(u.getId());
            r.setDisplayName(u.getDisplayName());
            r.setLastModified(new Date(0));
        }
        timestamp = r.getLastModified();

        return writeResource(session, r);
    }

    private JSONArray actionSearch(final JSONObject jsonObj) throws OXException,
            JSONException {
        final ResourceService resourceService = ServerServiceRegistry.getServize(ResourceService.class, true);
        if (null == resourceService) {
            throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create( ResourceService.class.getName());
        }

        final JSONArray jsonResponseArray = new JSONArray();

        final String searchpattern;
        final JSONObject jData = DataParser.checkJSONObject(jsonObj, AJAXServlet.PARAMETER_DATA);
        if (jData.has(SearchFields.PATTERN) && !jData.isNull(SearchFields.PATTERN)) {
            searchpattern = jData.getString(SearchFields.PATTERN);
        } else {
            LOG.warn("Missing field \"{}\" in JSON data. Searching for all as fallback", SearchFields.PATTERN);
            return actionAll();
        }

        final com.openexchange.resource.Resource[] resources = resourceService.searchResources(searchpattern, session.getContext());
        if (resources.length > 0) {
            long lastModified = Long.MIN_VALUE;
            for (final com.openexchange.resource.Resource resource : resources) {
                if (lastModified < resource.getLastModified().getTime()) {
                    lastModified = resource.getLastModified().getTime();
                }
                jsonResponseArray.put(writeResource(session, resource));
            }
            timestamp = new Date(lastModified);
        } else {
            timestamp = new Date(0);
        }

        return jsonResponseArray;
    }

    private static final String STR_ALL = "*";

    /**
     * Performs an all request
     *
     * @return A JSON array of all available resources' IDs
     * @throws OXException
     *             If all resources cannot be retrieved from resource storage
     */
    private JSONArray actionAll() throws OXException {
        final JSONArray jsonResponseArray = new JSONArray();

        final com.openexchange.resource.Resource[] resources = ServerServiceRegistry.getServize(ResourceService.class, true).searchResources(
                STR_ALL, session.getContext());
        if (resources.length > 0) {
            long lastModified = Long.MIN_VALUE;
            for (final com.openexchange.resource.Resource resource : resources) {
                if (lastModified < resource.getLastModified().getTime()) {
                    lastModified = resource.getLastModified().getTime();
                }
                jsonResponseArray.put(resource.getIdentifier());
            }
            timestamp = new Date(lastModified);
        } else {
            timestamp = new Date(0);
        }

        return jsonResponseArray;
    }

    /**
     * Gets the last-modified time stamp
     *
     * @return The last-modified time stamp
     */
    public Date getTimestamp() {
        return timestamp;
    }

}
