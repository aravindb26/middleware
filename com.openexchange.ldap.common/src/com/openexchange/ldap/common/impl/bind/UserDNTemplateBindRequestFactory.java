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

package com.openexchange.ldap.common.impl.bind;

import java.util.Objects;
import com.openexchange.exception.OXException;
import com.openexchange.ldap.common.BindRequestFactory;
import com.openexchange.ldap.common.LDAPCommonErrorCodes;
import com.openexchange.ldap.common.config.auth.UserDNTemplateConfig;
import com.openexchange.session.Session;
import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.ldap.sdk.unboundidds.controls.GetAuthorizationEntryRequestControl;
import com.unboundid.ldap.sdk.unboundidds.controls.GetUserResourceLimitsRequestControl;

/**
 * {@link UserDNTemplateBindRequestFactory} is a {@link BindRequestFactory} which provides bind requests for user dn template authentication
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.6
 */
public class UserDNTemplateBindRequestFactory extends AbstractUserDNBindRequestFactory {

    private final UserDNTemplateConfig config;

    /**
     * Initializes a new {@link UserDNTemplateBindRequestFactory}.
     *
     * @param config The {@link UserDNTemplateConfig}
     * @throws OXException in case the configuration is invalid
     */
    public UserDNTemplateBindRequestFactory(UserDNTemplateConfig config) throws OXException {
        super();
        if (!Objects.nonNull(config)) {
            throw LDAPCommonErrorCodes.INVALID_CONFIG.create();
        }
        this.config = config;
        if (config.getDNTemplate().contains(VARIABLE_NAME) == false) {
            throw LDAPCommonErrorCodes.INVALID_CONFIG.create();
        }
    }

    @Override
    public BindRequest createBindRequest(Session session) throws OXException {
        String template = config.getDNTemplate();
        String name = getUserName(session, config.getNameSource());

        // @formatter:off
        return new SimpleBindRequest(template.replace(VARIABLE_NAME, name),
                                     session.getPassword(),
                                     new GetAuthorizationEntryRequestControl(),
                                     new GetUserResourceLimitsRequestControl());
        // @formatter:on
    }

    @Override
    public String getId() {
        return BuiltInFactory.USER_DN_TEMPLATE.getId();
    }

}
