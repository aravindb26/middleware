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

package com.openexchange.oauth.json.proxy;

import com.openexchange.ajax.requesthandler.AJAXActionService;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.exception.OXException;
import com.openexchange.oauth.OAuthService;
import com.openexchange.oauth.http.OAuthProxyHttpHelper;
import com.openexchange.oauth.http.ProxyRequest;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 *
 * {@link OAuthProxyAction}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public class OAuthProxyAction implements AJAXActionService {

    private final OAuthService oauthService;
    private final OAuthProxyHttpHelper helper;

    public OAuthProxyAction(OAuthService service, OAuthProxyHttpHelper helper) {
        oauthService = service;
        this.helper = helper;
    }

    @Override
    public AJAXRequestResult perform(AJAXRequestData requestData, ServerSession session) throws OXException {
        OAuthProxyRequest proxy = new OAuthProxyRequest(requestData, session, oauthService);
        // @formatter:off
        ProxyRequest req = new ProxyRequest(proxy.getAccount(),
                                            proxy.getMethod(),
                                            proxy.getParameters(),
                                            proxy.getHeaders(),
                                            proxy.getUrl(),
                                            proxy.getBody());
        // @formatter:on
        try {
            String result = helper.execute(session, req);
            return new AJAXRequestResult(result, "string");
        } catch(@SuppressWarnings("unused") IllegalArgumentException e) {
            throw AjaxExceptionCodes.UNKNOWN_ACTION.create(proxy.getMethod().toString());
        }
    }

}
