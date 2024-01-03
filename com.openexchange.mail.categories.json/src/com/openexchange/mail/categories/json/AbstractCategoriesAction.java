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

package com.openexchange.mail.categories.json;

import org.json.JSONException;
import com.openexchange.ajax.requesthandler.AJAXActionService;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.capabilities.CapabilityService;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link AbstractCategoriesAction}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.8.2
 */
public abstract class AbstractCategoriesAction implements AJAXActionService {

    private static final String CAPABILITY_MAIL_CATEGORIES = "mail_categories";
    /** The OSGi service look-up */
    protected final ServiceLookup services;

    /**
     * Initializes a new {@link SwitchAction}.
     */
    protected AbstractCategoriesAction(ServiceLookup services) {
        super();
        this.services = services;
    }

    @Override
    public final AJAXRequestResult perform(AJAXRequestData requestData, ServerSession session) throws OXException {
        try {
            // Check required "mail_categories" capability
            {
                CapabilityService capabilityService = services.getService(CapabilityService.class);
                if (null != capabilityService && !capabilityService.getCapabilities(requestData.getSession()).contains(CAPABILITY_MAIL_CATEGORIES)) {
                    throw AjaxExceptionCodes.NO_PERMISSION_FOR_MODULE.create(CAPABILITY_MAIL_CATEGORIES);
                }
            }
            return doPerform(requestData, session);
        } catch (JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Performs given request.
     *
     * @param requestData The request data
     * @param session The associated session
     * @return The result
     * @throws OXException If an Open-Xchange error occurs
     * @throws JSONException If a JSON error occurs
     */
    protected abstract AJAXRequestResult doPerform(AJAXRequestData requestData, ServerSession session) throws OXException, JSONException;

}
