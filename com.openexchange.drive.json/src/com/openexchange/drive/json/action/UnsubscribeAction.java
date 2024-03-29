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

import java.util.List;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.drive.events.subscribe.DriveSubscriptionStore;
import com.openexchange.drive.json.internal.DefaultDriveSession;
import com.openexchange.drive.json.internal.Services;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.tools.servlet.AjaxExceptionCodes;


/**
 * {@link UnsubscribeAction}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class UnsubscribeAction extends AbstractDriveWriteAction {

    @Override
    protected boolean requiresRootFolderID() {
        return false;
    }

    @Override
    public AJAXRequestResult doPerform(AJAXRequestData requestData, DefaultDriveSession session) throws OXException {
        /*
         * get parameters
         */
        String token = requestData.getParameter("token");
        if (Strings.isEmpty(token)) {
            throw AjaxExceptionCodes.MISSING_PARAMETER.create("token");
        }
        String serviceID = requestData.getParameter("service");
        if (Strings.isEmpty(serviceID)) {
            throw AjaxExceptionCodes.MISSING_PARAMETER.create("service");
        }
        /*
         * extract root folder identifier(s), either directly, or based on representative domains; if not set, all subscriptions are purged
         */
        List<String> rootFolderIDs = extractPushRootFolderIDs(requestData);
        /*
         * remove subscription(s)
         */
        DriveSubscriptionStore subscriptionStore = Services.getService(DriveSubscriptionStore.class, true);
        subscriptionStore.unsubscribe(session.getServerSession(), serviceID, token, rootFolderIDs);
        /*
         * return empty json object to indicate success
         */
        return new AJAXRequestResult(JSONObject.EMPTY_OBJECT, "json");
    }

}
