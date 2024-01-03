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

package com.openexchange.dav;

import java.net.URLDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.dav.osgi.Services;
import com.openexchange.java.Charsets;
import com.openexchange.login.DelegatingLoginRequest;
import com.openexchange.login.LoginRequest;
import com.openexchange.tools.webdav.LoginCustomizer;

/**
 * {@link URLDecodingLoginCustomizer}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v8.0.0
 */
public class URLDecodingLoginCustomizer implements LoginCustomizer {

    private static final Logger LOG = LoggerFactory.getLogger(URLDecodingLoginCustomizer.class);

    /**
     * Initializes a new {@link URLDecodingLoginCustomizer}.
     */
    public URLDecodingLoginCustomizer() {
        super();
    }

    @Override
    public LoginRequest modifyLogin(LoginRequest loginRequest) {
        if (null != loginRequest) {
            String login = loginRequest.getLogin();
            DAVUserAgentParser parser = Services.getDAVUserAgentParser();
            if (null != parser && null != login && DAVUserAgent.MAC_CONTACTS.equals(parser.parse(loginRequest.getUserAgent())) && login.contains("%40")) {
                try {
                    String decodedLogin = URLDecoder.decode(login, Charsets.UTF_8);
                    LOG.trace("Using decoded login name \"{}\" for \"{}\" passed from macOS Contacts client.", decodedLogin, login);
                    return new DelegatingLoginRequest(loginRequest) {

                        @Override
                        public String getLogin() {
                            return decodedLogin;
                        }
                    };
                } catch (IllegalArgumentException e) {
                    LOG.warn("Unexpected error decoding \"{}\", unable to customize login.", login, e);
                }

            }
        }
        return loginRequest;
    }

}
