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

package com.openexchange.chronos.provider.ical.auth;

import java.util.HashSet;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.auth.info.AuthInfo;
import com.openexchange.auth.info.AuthInfo.Builder;
import com.openexchange.auth.info.AuthType;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.provider.CalendarAccountAttribute;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;

/**
 * 
 * {@link ICalAuthParser}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 */
public class ICalAuthParser {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ICalAuthParser.class);

    private static final ICalAuthParser INSTANCE = new ICalAuthParser();

    /**
     * Gets the instance.
     *
     * @return The instance
     */
    public static ICalAuthParser getInstance() {
        return INSTANCE;
    }

    private ICalAuthParser() {
        super();
    }

    public AuthInfo getAuthInfo(JSONObject config) throws OXException {
        JSONObject userConfiguration = new JSONObject(config);
        try {
            Set<CalendarAccountAttribute> availableAttributes = parse(userConfiguration);
            Set<CalendarAccountAttribute> authAttributes = validate(availableAttributes);
            return generateAuthInfo(userConfiguration, authAttributes);
        } catch (JSONException e) {
            LOG.error("Unable to recognize auth information. Will try with no auth.", e);
        }
        return AuthInfo.NONE;
    }

    /**
     * Parses the attributes from the configuration and adapts them to be valid for further processing
     *
     * @param configuration
     * @return Set of {@link CalendarAccountAttribute}s contained within the configuration map
     */
    private Set<CalendarAccountAttribute> parse(final JSONObject configuration) throws JSONException {
        final Set<CalendarAccountAttribute> attributes = new HashSet<CalendarAccountAttribute>();

        String login = CalendarAccountAttribute.LOGIN_LITERAL.getName();
        if (configuration.has(login)) {
            attributes.add(CalendarAccountAttribute.LOGIN_LITERAL);
        }

        String id = CalendarAccountAttribute.ID_LITERAL.getName();
        if (configuration.has(id)) {
            attributes.add(CalendarAccountAttribute.ID_LITERAL);
        }

        String password = CalendarAccountAttribute.PASSWORD_LITERAL.getName();
        if (configuration.has(password)) {
            configuration.put(password, parseString(configuration, password));
            attributes.add(CalendarAccountAttribute.PASSWORD_LITERAL);
        }

        String token = CalendarAccountAttribute.TOKEN_LITERAL.getName();
        if (configuration.has(token)) {
            attributes.add(CalendarAccountAttribute.TOKEN_LITERAL);
        }
        return attributes;
    }

    private static String parseString(final JSONObject configuration, final String name) throws JSONException {
        String retval = null;
        if (configuration.has(name)) {
            final String test = configuration.getString(name);
            if (0 != test.length()) {
                retval = test;
            }
        }
        return retval;
    }

    private Set<CalendarAccountAttribute> validate(Set<CalendarAccountAttribute> attributes) throws OXException {
        Set<CalendarAccountAttribute> copy = new HashSet<>(attributes);
        copy.remove(CalendarAccountAttribute.LOGIN_LITERAL);
        copy.remove(CalendarAccountAttribute.ID_LITERAL);

        if (copy.size() > 1) {
            throw CalendarExceptionCodes.BAD_AUTH_CONFIGURATION.create(copy.toString());
        }
        return copy;
    }

    private AuthInfo generateAuthInfo(JSONObject configuration, Set<CalendarAccountAttribute> authAttributes) throws JSONException {
        if (authAttributes.isEmpty()) {
            return AuthInfo.NONE;
        }

        AuthInfo authInfo = null;
        Builder builder = AuthInfo.builder();
        if (authAttributes.contains(CalendarAccountAttribute.TOKEN_LITERAL)) {
            authInfo = builder.setAuthType(AuthType.TOKEN).setToken((String) configuration.get(CalendarAccountAttribute.TOKEN_LITERAL.getName())).build();
        } else if (authAttributes.contains(CalendarAccountAttribute.LOGIN_LITERAL) || authAttributes.contains(CalendarAccountAttribute.PASSWORD_LITERAL)) {
            builder.setAuthType(AuthType.BASIC);
            String login = configuration.optString(CalendarAccountAttribute.LOGIN_LITERAL.getName());
            if (Strings.isNotEmpty(login)) {
                builder.setLogin(login);
            }
            String feedPassword = configuration.optString(CalendarAccountAttribute.PASSWORD_LITERAL.getName());
            if (Strings.isNotEmpty(feedPassword)) {
                builder.setPassword(feedPassword);
            }
            authInfo = builder.build();
        } else {
            authInfo = AuthInfo.NONE;
        }
        return authInfo;
    }
}
