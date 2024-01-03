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
import com.openexchange.ldap.common.config.auth.UserDNResolvedConfig;
import com.openexchange.session.Session;
import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPInterface;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.SimpleBindRequest;

/**
 * {@link UserDNResolvedBindRequestFactory} is a {@link BindRequestFactory} which provides bind requests for user dn resolved authentication
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.6
 */
public class UserDNResolvedBindRequestFactory extends AbstractUserDNBindRequestFactory {

    private final UserDNResolvedConfig config;
    private final LDAPInterface connection;

    /**
     * Initializes a new {@link UserDNResolvedBindRequestFactory}.
     *
     * @param config The {@link UserDNResolvedConfig}
     * @param connection The {@link LDAPInterface}
     * @throws OXException in case the configuration is invalid
     *
     */
    public UserDNResolvedBindRequestFactory(UserDNResolvedConfig config, LDAPInterface connection) throws OXException {
        super();
        if (!Objects.nonNull(config)) {
            throw LDAPCommonErrorCodes.INVALID_CONFIG.create();
        }
        this.config = config;
        this.connection = connection;
    }

    @Override
    public BindRequest createBindRequest(Session session) throws OXException {
        String name = getUserName(session, config.getNameSource());
        String searchBaseDN = config.getSearchBaseDN();
        String searchFilter = config.getSearchFilterTemplate().replace(VARIABLE_NAME, name);

        SearchScope searchScope = config.getSearchScope();
        SearchResult searchResult;
        try {
            searchResult = connection.search(searchBaseDN, searchScope, Filter.create(searchFilter));
        } catch (LDAPException e) {
            throw LDAPCommonErrorCodes.CONNECTION_ERROR.create(e, e.getMessage());
        }
        if (searchResult.getResultCode().equals(ResultCode.SUCCESS) == false || searchResult.getEntryCount() != 1) {
            throw LDAPCommonErrorCodes.CONNECTION_ERROR.create("User dn not found");
        }

        return new SimpleBindRequest(searchResult.getSearchEntries().get(0).getDN(), session.getPassword());
    }

    @Override
    public String getId() {
        return BuiltInFactory.USER_DN_RESOLVED.getId();
    }

}
