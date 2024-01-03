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

package com.openexchange.contact.provider.ldap;

import static com.openexchange.contact.provider.ldap.SettingsHelper.initInternalConfig;
import static com.openexchange.contact.provider.ldap.config.ConfigUtils.PROPERTY_ACCOUNTS;
import static com.openexchange.contact.provider.ldap.config.ConfigUtils.PROVIDER_ID_PREFIX;
import java.util.EnumSet;
import java.util.Locale;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.contact.common.ContactsAccount;
import com.openexchange.contact.common.ContactsParameters;
import com.openexchange.contact.provider.AutoProvisioningContactsProvider;
import com.openexchange.contact.provider.ContactsAccessCapability;
import com.openexchange.contact.provider.ContactsProviderExceptionCodes;
import com.openexchange.contact.provider.folder.FolderContactsProvider;
import com.openexchange.contact.provider.ldap.config.ProviderConfig;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.java.Strings;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;

/**
 * {@link LdapContactsProvider}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class LdapContactsProvider implements FolderContactsProvider, AutoProvisioningContactsProvider {

    private static final Logger LOG = LoggerFactory.getLogger(LdapContactsProvider.class);

    private final ServiceLookup services;
    private final String id;
    private final ProviderConfig config;
    private final LocalCacheRegistry cacheRegistry;

    /**
     * Initializes a new {@link LdapContactsProvider}.
     *
     * @param id The identifier of the contact provider
     * @param services A service lookup reference
     * @param config The underlying provider config
     * @param cacheRegistry The {@link LocalCacheRegistry}
     */
    public LdapContactsProvider(ServiceLookup services, String id, ProviderConfig config, LocalCacheRegistry cacheRegistry) {
        super();
        this.services = services;
        this.id = id;
        this.config = config;
        this.cacheRegistry = cacheRegistry;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName(Locale locale) {
        return config.getName();
    }

    @Override
    public EnumSet<ContactsAccessCapability> getCapabilities() {
        return ContactsAccessCapability.getCapabilities(LdapContactsAccess.class);
    }

    @Override
    public boolean isAvailable(Session session) {
        try {
            LeanConfigurationService configService = services.getServiceSafe(LeanConfigurationService.class);
            String accounts = configService.getProperty(session.getUserId(), session.getContextId(), PROPERTY_ACCOUNTS);
            if (Strings.isNotEmpty(accounts)) {
                for (String splitted : Strings.splitByComma(accounts)) {
                    if (getId().equals(PROVIDER_ID_PREFIX + splitted)) {
                        return true;
                    }
                }
            }
        } catch (OXException e) {
            LOG.warn("Unexpected error checking if {} is available for {}: {}", getId(), session, e.getMessage(), e);
        }
        return false;
    }

    @Override
    public LdapContactsAccess connect(Session session, ContactsAccount account, ContactsParameters parameters) throws OXException {
        LOG.debug("Connecting LDAP contacts access for {}", account);
        if (config.optCacheConfig().isPresent() && config.optCacheConfig().get().useCache()) {
            return new CachingLdapContactsAccess(services, config, session, account, parameters, cacheRegistry, id);
        }
        return new LdapContactsAccess(services, config, session, account, parameters);
    }

    @Override
    public void onAccountDeleted(Session session, ContactsAccount account, ContactsParameters parameters) throws OXException {
        // nothing to do
    }

    @Override
    public void onAccountDeleted(Context context, ContactsAccount account, ContactsParameters parameters) throws OXException {
        // nothing to do
    }

    @Override
    public JSONObject autoConfigureAccount(Session session, JSONObject userConfig, ContactsParameters parameters) throws OXException {
        /*
         * prepare & return initial internal configuration
         */
        return initInternalConfig(config);
    }

    @Override
    public JSONObject configureAccount(Session session, JSONObject userConfig, ContactsParameters parameters) throws OXException {
        /*
         * no manual account creation allowed as accounts are provisioned automatically
         */
        throw ContactsProviderExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(getId());
    }

    @Override
    public JSONObject reconfigureAccount(Session session, ContactsAccount account, JSONObject userConfig, ContactsParameters parameters) throws OXException {
        /*
         * extract & return slipstreamed internal config if set to be saved along with the account, otherwise use existing config or initial default
         */
        Object internalConfig = userConfig.remove("internalConfig");
        if (null != internalConfig && (internalConfig instanceof JSONObject)) {
            return (JSONObject) internalConfig;
        }
        return null != account ? account.getInternalConfiguration() : initInternalConfig(config);
    }

    @Override
    public String toString() {
        return "LdapContactsProvider [id=" + getId() + "]";
    }

}
