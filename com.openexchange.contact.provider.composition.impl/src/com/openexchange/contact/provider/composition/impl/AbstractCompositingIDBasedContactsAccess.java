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

package com.openexchange.contact.provider.composition.impl;

import static com.openexchange.java.Autoboxing.I;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.capabilities.CapabilityService;
import com.openexchange.contact.DefaultContactsParameters;
import com.openexchange.contact.common.ContactsAccount;
import com.openexchange.contact.common.ContactsParameters;
import com.openexchange.contact.provider.AutoProvisioningContactsProvider;
import com.openexchange.contact.provider.CommonContactsConfigurationFields;
import com.openexchange.contact.provider.ContactsAccess;
import com.openexchange.contact.provider.ContactsAccessCapability;
import com.openexchange.contact.provider.ContactsAccountService;
import com.openexchange.contact.provider.ContactsProvider;
import com.openexchange.contact.provider.ContactsProviderExceptionCodes;
import com.openexchange.contact.provider.ContactsProviderRegistry;
import com.openexchange.contact.provider.ContactsProviders;
import com.openexchange.contact.provider.FallbackAwareContactsProvider;
import com.openexchange.contact.provider.basic.BasicContactsAccess;
import com.openexchange.contact.provider.extensions.WarningsAware;
import com.openexchange.contact.provider.folder.FolderContactsAccess;
import com.openexchange.contact.provider.folder.FolderContactsProvider;
import com.openexchange.contact.provider.groupware.GroupwareContactsAccess;
import com.openexchange.exception.OXException;
import com.openexchange.java.CallerRunsCompletionService;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.threadpool.ThreadPoolCompletionService;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.threadpool.ThreadPools;
import com.openexchange.tools.session.ServerSessionAdapter;
import com.openexchange.tx.TransactionAware;

/**
 * {@link AbstractCompositingIDBasedContactsAccess}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.5
 */
abstract class AbstractCompositingIDBasedContactsAccess implements TransactionAware, ContactsParameters {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractCompositingIDBasedContactsAccess.class);

    protected final ServiceLookup services;
    protected final Session session;
    protected final List<OXException> warnings;
    protected final ContactsParameters parameters;

    private final ContactsProviderRegistry providerRegistry;
    private final ConcurrentMap<Integer, ContactsAccess> connectedAccesses;

    /**
     * Initializes a new {@link AbstractCompositingIDBasedContactsAccess}.
     *
     * @param session The session to create the ID-based access for
     * @param providerRegistry A reference to the contacts provider registry
     * @param services A service lookup reference
     */
    protected AbstractCompositingIDBasedContactsAccess(Session session, ContactsProviderRegistry providerRegistry, ServiceLookup services) {
        super();
        this.services = services;
        this.providerRegistry = providerRegistry;
        this.session = session;
        this.parameters = new DefaultContactsParameters();
        this.connectedAccesses = new ConcurrentHashMap<>();
        this.warnings = new ArrayList<>();
    }

    @Override
    public <T> ContactsParameters set(String parameter, T value) {
        return parameters.set(parameter, value);
    }

    @Override
    public <T> T get(String parameter, Class<T> clazz) {
        return parameters.get(parameter, clazz);
    }

    @Override
    public <T> T get(String parameter, Class<T> clazz, T defaultValue) {
        return parameters.get(parameter, clazz, defaultValue);
    }

    @Override
    public boolean contains(String parameter) {
        return parameters.contains(parameter);
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return parameters.entrySet();
    }

    @Override
    public void startTransaction() throws OXException {
        ConcurrentMap<Integer, ContactsAccess> connectedAccesses = this.connectedAccesses;
        if (false == connectedAccesses.isEmpty()) {
            for (ContactsAccess access : connectedAccesses.values()) {
                LOG.warn("Access already connected: {}", access);
            }
        }
        connectedAccesses.clear();
        warnings.clear();
    }

    @Override
    public void finish() throws OXException {
        /*
         * close any connected calendar accesses
         */
        ConcurrentMap<Integer, ContactsAccess> connectedAccesses = this.connectedAccesses;
        for (Iterator<Entry<Integer, ContactsAccess>> iterator = connectedAccesses.entrySet().iterator(); iterator.hasNext();) {
            Entry<Integer, ContactsAccess> entry = iterator.next();
            ContactsAccess access = entry.getValue();
            LOG.debug("Closing contacts access {} for account {}.", access, entry.getKey());
            if ((access instanceof WarningsAware)) {
                List<OXException> warnings = ((WarningsAware) access).getWarnings();
                if (null != warnings) {
                    this.warnings.addAll(warnings);
                }
            }
            access.close();
            iterator.remove();
        }
    }

    @Override
    public void commit() throws OXException {
        //no-op
    }

    @Override
    public void rollback() throws OXException {
        //no-op
    }

    @Override
    public void setTransactional(boolean transactional) {
        //no-op
    }

    @Override
    public void setRequestTransactional(boolean transactional) {
        //no-op
    }

    @Override
    public void setCommitsTransaction(boolean commits) {
        //no-op
    }

    ///////////////////////////////////////// HELPERS ////////////////////////////////////////

    /**
     * Gets the basic contacts access for a specific account.
     *
     * @param account The account identifier to get the contacts access for
     * @return The basic contacts access for the specified account
     */
    protected BasicContactsAccess getBasicAccess(ContactsAccount account) throws OXException {
        return getAccess(account, BasicContactsAccess.class);
    }

    /**
     * Gets the contacts folder access for a specific account.
     *
     * @param account The account identifier to get the contacts access for
     * @return The contacts folder access for the specified account
     */
    protected FolderContactsAccess getFolderAccess(int accountId) throws OXException {
        return getAccess(accountId, FolderContactsAccess.class);
    }

    /**
     * Gets the contacts folder access for a specific account.
     *
     * @param account The account identifier to get the contacts access for
     * @return The contacts folder access for the specified account
     */
    protected FolderContactsAccess getFolderAccess(ContactsAccount account) throws OXException {
        return getAccess(account, FolderContactsAccess.class);
    }

    /**
     * Gets the groupware contacts access for a specific account.
     *
     * @param account The account identifier to get the contacts access for
     * @return The groupware contacts access for the specified account
     */
    protected GroupwareContactsAccess getGroupwareAccess(int accountId) throws OXException {
        return getAccess(accountId, GroupwareContactsAccess.class);
    }

    /**
     * Gets the groupware contacts access for a specific account. The account is connected implicitly and remembered to be closed during
     * {@link #finish()} implicitly, if not already done.
     *
     * @param account The account to get the contacts access for
     * @return The groupware contacts access for the specified account
     */
    protected GroupwareContactsAccess getGroupwareAccess(ContactsAccount account) throws OXException {
        return getAccess(account, GroupwareContactsAccess.class);
    }

    /**
     * Gets the contacts access for a specific account. The account is connected implicitly and remembered to be closed during
     * {@link #finish()} implicitly, if not already done.
     *
     * @param accountId The identifier to get the contacts access for
     * @return The contacts access for the specified account
     */
    protected ContactsAccess getAccess(int accountId) throws OXException {
        ContactsAccess access = connectedAccesses.get(I(accountId));
        return null != access ? access : getAccess(getAccount(accountId));
    }

    /**
     * Gets the contacts access implementing a specific extension for a specific account. The account is connected implicitly and
     * remembered to be closed during {@link #finish()} implicitly, if not already done.
     * <p/>
     * If the provider in not available or the extension is not provided by the account's contacts access, an appropriate exception is thrown.
     *
     * @param accountId The identifier to get the contacts access for
     * @param extensionClass The targeted extension class
     * @return The contacts access for the specified account
     */
    protected <T extends ContactsAccess> T getAccess(int accountId, Class<T> extensionClass) throws OXException {
        ContactsAccount account = getAccount(accountId);
        String providerId = account.getProviderId();
        Optional<ContactsProvider> contactsProvider = providerRegistry.getContactProvider(providerId);
        if (contactsProvider.isEmpty()) {
            throw ContactsProviderExceptionCodes.PROVIDER_NOT_AVAILABLE.create(providerId);
        }
        ContactsAccess access = getAccess(accountId);
        try {
            return extensionClass.cast(access);
        } catch (ClassCastException e) {
            throw ContactsProviderExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(e, getProviderName(account));
        }
    }

    /**
     * Gets the contacts access for a specific account. The account is connected implicitly and remembered to be closed during
     * {@link #finish()} implicitly, if not already done.
     *
     * @param account The account to get the contacts access for
     * @return The contacts access for the specified account
     */
    protected ContactsAccess getAccess(ContactsAccount account) {
        return getAccess(account, parameters);
    }

    /**
     * Gets the contacts access for a specific account. The account is connected implicitly and remembered to be closed during
     * {@link #finish()} implicitly, if not already done.
     *
     * @param account The account to get the contacts access for
     * @param parameters The contacts parameters to use when initializing the access
     * @return The contacts access for the specified account
     */
    protected ContactsAccess getAccess(ContactsAccount account, ContactsParameters parameters) {
        ConcurrentMap<Integer, ContactsAccess> connectedAccesses = this.connectedAccesses;
        ContactsAccess access = connectedAccesses.get(I(account.getAccountId()));
        if (null == access) {
            access = initAccess(account, parameters);
            ContactsAccess existingAccess = connectedAccesses.put(I(account.getAccountId()), access);
            if (null != existingAccess) {
                access.close();
                access = existingAccess;
            }
        }
        return access;
    }

    /**
     * Gets the contacts access implementing a specific extension for a specific account. The account is connected implicitly and
     * remembered to be closed during {@link #finish()} implicitly, if not already done.
     * <p/>
     * If the extension is not provided by the account's contacts access, an appropriate exception is thrown.
     *
     * @param account The account to get the contacts access for
     * @param extensionClass The targeted extension class
     * @return The contacts access for the specified account
     */
    protected <T extends ContactsAccess> T getAccess(ContactsAccount account, Class<T> extensionClass) throws OXException {
        ContactsAccess access = getAccess(account);
        try {
            return extensionClass.cast(access);
        } catch (ClassCastException e) {
            throw ContactsProviderExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(e, getProviderName(account));
        }
    }

    /**
     * Gets a specific contacts account.
     *
     * @param accountId The identifier of the account to get
     * @return The contacts account
     */
    protected ContactsAccount getAccount(int accountId) throws OXException {
        return getAccount(accountId, false);
    }

    /**
     * Gets a specific contacts account.
     *
     * @param accountId The identifier of the account to get
     * @param checkCapbilities <code>true</code> to check the current session user's capabilities for the underlying contacts provider,
     *            <code>false</code>, otherwise
     * @return The contacts account
     */
    protected ContactsAccount getAccount(int accountId, boolean checkCapbilities) throws OXException {
        ContactsAccount account = optAccount(accountId);
        if (null == account) {
            throw ContactsProviderExceptionCodes.ACCOUNT_NOT_FOUND.create(I(accountId));
        }
        if (checkCapbilities) {
            requireCapability(account.getProviderId());
        }
        return account;
    }

    /**
     * Gets a (fallback) for the account's display name, in case the corresponding settings are not available.
     *
     * @param account The account to get the name for
     * @return The account name
     */
    protected String getAccountName(ContactsAccount account) {
        String fallbackName = "Account " + account.getAccountId();
        try {
            JSONObject internalConfig = account.getInternalConfiguration();
            if (null != internalConfig) {
                return internalConfig.optString(CommonContactsConfigurationFields.NAME, fallbackName);
            }
        } catch (Exception e) {
            LOG.debug("Error getting display name for contacts account \"{}\": {}", account.getProviderId(), e.getMessage());
        }
        return fallbackName;
    }

    /**
     * Gets all <i>enabled</i> contacts accounts of the current session's user.
     *
     * @return The contacts accounts, or an empty list if there are none
     */
    protected List<ContactsAccount> getAccounts() throws OXException {
        return this.services.getServiceSafe(ContactsAccountService.class).getAccounts(session, parameters);
    }

    /**
     * Gets all <i>enabled</i> contacts accounts of the current session's user supporting a specific contacts capability..
     *
     * @param capability The targeted capability
     * @return The contacts accounts supporting the capability, or an empty list if there are none
     */
    protected List<ContactsAccount> getAccounts(ContactsAccessCapability capability) throws OXException {
        List<ContactsAccount> accounts = new LinkedList<>();
        for (ContactsAccount account : getAccounts()) {
            if (supports(account, capability)) {
                accounts.add(account);
            }
        }
        return accounts;
    }

    /**
     * Gets all <i>enabled</i> contacts accounts of the current session's user implementing a specific extension.
     *
     * @param extensionClass The targeted extension class
     * @return The contacts accounts supporting the extension, or an empty list if there are none
     */
    protected <T extends ContactsAccess> List<ContactsAccount> getAccounts(Class<T> extensionClass) throws OXException {
        List<ContactsAccount> accounts = new LinkedList<>();
        for (ContactsAccount account : getAccounts()) {
            if (supports(account, extensionClass)) {
                accounts.add(account);
            }
        }
        return accounts;
    }

    /**
     * Checks whether the provider of the specified account supports the specified extension
     *
     * @param <T> The extension type
     * @param account The account
     * @param extensionClass The extension class
     * @return <code>true</code> if the provider of the specified account supports the specified extension;
     *         <code>false</code> otherwise.
     */
    protected <T extends ContactsAccess> boolean supports(ContactsAccount account, Class<T> extensionClass) {
        Optional<ContactsProvider> provider = providerRegistry.getContactProvider(account.getProviderId());
        if (false == provider.isPresent()) {
            LOG.warn("Contacts provider '{}' for account {} not found; skipping.", account.getProviderId(), I(account.getAccountId()));
            return false;
        }
        for (ContactsAccessCapability capability : provider.get().getCapabilities()) {
            if (capability.getAccessInterface().isAssignableFrom(extensionClass)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the provider of the specified account supports the specified
     * {@link ContactsAccessCapability}.
     *
     * @param account The account
     * @param capability the capability
     * @return <code>true</code> if the provider of the specified account supports the specified capability;
     *         <code>false</code> otherwise.
     *
     */
    protected boolean supports(ContactsAccount account, ContactsAccessCapability capability) {
        Optional<ContactsProvider> provider = providerRegistry.getContactProvider(account.getProviderId());
        if (false == provider.isPresent()) {
            LOG.warn("Contacts provider '{}' for account {} not found; skipping.", account.getProviderId(), I(account.getAccountId()));
            return false;
        }
        return provider.get().getCapabilities().contains(capability);
    }

    /**
     * Optionally gets a specific contacts account.
     *
     * @param accountId The identifier of the account to get
     * @return The contacts account, or <code>null</code> if not found
     */
    protected ContactsAccount optAccount(int accountId) throws OXException {
        return this.services.getServiceSafe(ContactsAccountService.class).getAccount(session, accountId, parameters);
    }

    /**
     * Gets a value indicating whether a specific contacts account is auto-provisioned, i.e. it was created automatically and the user
     * cannot delete it.
     *
     * @param account The account to check
     * @return <code>true</code> if the account is auto-provisioned, <code>false</code>, otherwise
     */
    protected boolean isAutoProvisioned(ContactsAccount account) {
        Optional<ContactsProvider> provider = providerRegistry.getContactProvider(account.getProviderId());
        return provider.isPresent() && (provider.get() instanceof AutoProvisioningContactsProvider);
    }

    /**
     * Checks that the current session's user has the required capability for a specific contacts provider.
     *
     * @param providerId The identifier of the contacts provider to check the capabilities for
     * @throws OXException {@link ContactsProviderExceptionCodes#MISSING_CAPABILITY}
     */
    protected void requireCapability(String providerId) throws OXException {
        if (false == hasCapability(providerId)) {
            throw ContactsProviderExceptionCodes.MISSING_CAPABILITY.create(ContactsProviders.getCapabilityName(providerId));
        }
    }

    /**
     * Gets a value indicating whether the current session's user has the required capability for a specific contacts provider or not.
     *
     * @param providerId The identifier of the contacts provider to check the capabilities for
     * @return <code>true</code> if the user has the required capability, <code>false</code>, otherwise
     */
    protected boolean hasCapability(String providerId) throws OXException {
        String capabilityName = ContactsProviders.getCapabilityName(providerId);
        return this.services.getServiceSafe(CapabilityService.class).getCapabilities(session).contains(capabilityName);
    }

    /**
     * Gets the account's contacts provider's display name, localised in the current session user's language.
     *
     * @param account The account to get the provider name for
     * @return The provider name
     */
    protected String getProviderName(ContactsAccount account) {
        try {
            Optional<ContactsProvider> provider = providerRegistry.getContactProvider(account.getProviderId());
            if (provider.isPresent()) {
                return provider.get().getDisplayName(ServerSessionAdapter.valueOf(session).getUser().getLocale());
            }
        } catch (Exception e) {
            LOG.debug("Error getting display name for contacts provider '{}': {}", account.getProviderId(), e.getMessage());
        }
        return account.getProviderId();
    }

    /**
     * Checks if the {@link ContactsProvider} for the given account is a {@link FolderContactsProvider}.
     *
     * @param accountId the account identifier
     * @return <code>true</code> if the specified account is a {@link FolderContactsProvider}
     * @throws OXException if an error is occurred
     */
    protected boolean isContactsFolder1Provider(int accountId) throws OXException {
        Optional<ContactsProvider> provider = providerRegistry.getContactProvider(getAccount(accountId).getProviderId());
        return provider.isPresent() ? (provider.get() instanceof FolderContactsProvider) : false;
    }

    /**
     * Checks if the provider with the specified identifier is of the specified type.
     *
     * @param <T> The type of the contacts provider
     * @param providerId the provider's identifier
     * @param clazz the type of the contacts provider
     * @return <code>true</code> if the provider with the specified identifier is of the specified
     *         type; <code>false</code> otherwise
     */
    protected <T> boolean isTypedProvider(String providerId, Class<T> clazz) {
        Optional<ContactsProvider> provider = providerRegistry.getContactProvider(providerId);
        return provider.isPresent() ? clazz.isAssignableFrom(provider.get().getClass()) : false;
    }

    /**
     * Gets the completion service to manage asynchronous operations on multiple contacts accounts.
     *
     * @return The completion service
     */
    protected <V> CompletionService<V> getCompletionService() {
        ThreadPoolService threadPool = ThreadPools.getThreadPool();
        if (null == threadPool) {
            return new CallerRunsCompletionService<>();
        }
        return new ThreadPoolCompletionService<>(threadPool);
    }

    private ContactsAccess initAccess(ContactsAccount account, ContactsParameters parameters) {
        Optional<ContactsProvider> optionalProvider = providerRegistry.getContactProvider(account.getProviderId());
        try {
            if (false == optionalProvider.isPresent()) {
                throw ContactsProviderExceptionCodes.PROVIDER_NOT_AVAILABLE.create(account.getProviderId());
            }
            ContactsProvider provider = optionalProvider.get();
            if (false == hasCapability(provider.getId())) {
                throw ContactsProviderExceptionCodes.MISSING_CAPABILITY.create(ContactsProviders.getCapabilityName(provider.getId()));
            }
            return provider.connect(session, account, parameters);
        } catch (OXException e) {
            if (optionalProvider.isPresent() && (optionalProvider.get() instanceof FallbackAwareContactsProvider)) {
                return ((FallbackAwareContactsProvider) optionalProvider.get()).connectFallback(session, account, parameters, e);
            }
            if (optionalProvider.isPresent() && (optionalProvider.get() instanceof AutoProvisioningContactsProvider)) {
                return new FallbackEmptyContactsAccess(account, e);
            }
            return new FallbackUnknownContactsAccess(account, e);
        }
    }

}
