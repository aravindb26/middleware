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

package com.openexchange.oauth.impl.proxy;

import static com.openexchange.java.Autoboxing.I;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.Api;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.exception.OXException;
import com.openexchange.oauth.API;
import com.openexchange.oauth.KnownApi;
import com.openexchange.oauth.OAuthAccount;
import com.openexchange.oauth.OAuthExceptionCodes;
import com.openexchange.oauth.http.OAuthProxyHttpHelper;
import com.openexchange.oauth.http.ProxyRequest;
import com.openexchange.session.Session;

/**
 * {@link OAuthProxyHttpHelperImpl}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.6
 */
public class OAuthProxyHttpHelperImpl implements OAuthProxyHttpHelper {
    
    private static final Logger LOG = LoggerFactory.getLogger(OAuthProxyHttpHelperImpl.class);
    
    @Override
    public String execute(Session session, ProxyRequest req) throws OXException, IllegalArgumentException {
        
        final OAuthRequest httpRequest;
        switch (req.getMethod()) {
            case GET:
                httpRequest = buildGet(req);
                break;
            case DELETE:
                httpRequest = buildDelete(req);
                break;
            case PUT:
                httpRequest = buildPut(req);
                break;
            case POST:
                httpRequest = buildPost(req);
                break;
            default:
                throw new IllegalArgumentException("HTTP method not supported");
        }
        OAuthAccount account = req.getAccount();
        // @formatter:off
        OAuthService scribeOAuthService = new ServiceBuilder().provider(getProvider(account.getAPI()))
                                                              .apiKey(account.getMetaData().getAPIKey(session))
                                                              .apiSecret(account.getMetaData().getAPISecret(session))
                                                              .build();
        // @formatter:on
        
        scribeOAuthService.signRequest(getToken(account), httpRequest);
        Response resp = httpRequest.send();
        return resp.getBody();
    }
    
    /**
     * Gets the Scribe provider for given API.
     *
     * @param api The API
     * @return The associated Scribe provider
     * @throws IllegalStateException If given API cannot be mapped to a Scribe provider
     */
    protected static Class<? extends Api> getProvider(final API api) {
        KnownApi stdApi = KnownApi.getApiByServiceId(api.getServiceId());
        if (stdApi == null) {
            throw new IllegalStateException("Unsupported API type: " + api);
        }
        return stdApi.getApiClass();
    }
    
    /**
     * Gets the Scribe token for associated OAuth account.
     *
     * @return The Scribe token
     * @throws OXException If operation fails due to an invalid account
     */
    private static Token getToken(OAuthAccount account) throws OXException {
        try {
            return new Token(account.getToken(), account.getSecret());
        } catch (IllegalArgumentException e) {
            LOG.warn("Associated OAuth \"{} ({})\" account misses token information.", account.getDisplayName(), I(account.getId()));
            throw OAuthExceptionCodes.INVALID_ACCOUNT_EXTENDED.create(e, account.getDisplayName(), I(account.getId()));
        }
    }

    private static OAuthRequest buildPost(ProxyRequest proxyRequest) {
        OAuthRequest result = new OAuthRequest(Verb.POST, proxyRequest.getUrl());
        proxyRequest.getHeaders().forEach((k,v) -> result.addHeader(k,v));
        proxyRequest.getParameters().forEach((k,v) ->  result.addBodyParameter(k,v));
        return result;
    }

    private static OAuthRequest buildPut(ProxyRequest proxyRequest) {
        OAuthRequest result = buildCommon(Verb.GET, proxyRequest);
        result.addPayload(proxyRequest.getBody());
        return result;
    }

    private static OAuthRequest buildGet(ProxyRequest proxyRequest) {
        return buildCommon(Verb.GET, proxyRequest);
    }

    private static OAuthRequest buildDelete(ProxyRequest proxyRequest) {
        return buildCommon(Verb.DELETE, proxyRequest);
    }

    private static OAuthRequest buildCommon(Verb verb, ProxyRequest proxyRequest) {
        OAuthRequest result = new OAuthRequest(verb, proxyRequest.getUrl());
        proxyRequest.getHeaders().forEach((k,v) -> result.addHeader(k,v));
        proxyRequest.getParameters().forEach((k,v) ->  result.addQuerystringParameter(k,v));
        return result;
    }

}
