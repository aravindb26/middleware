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
package com.openexchange.mailaccount.internal;

import static com.openexchange.java.Autoboxing.I;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.mail.MailProperty;
import com.openexchange.mailaccount.Account;
import com.openexchange.mailaccount.Attribute;
import com.openexchange.mailaccount.Event;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.MailAccountDescription;
import com.openexchange.mailaccount.MailAccountExceptionCodes;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.mailaccount.TransportAccount;
import com.openexchange.mailaccount.TransportAccountDescription;
import com.openexchange.mailaccount.UpdateProperties;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;

/**
 * {@link TransportFilteringMailAccountStorage}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8
 */
public class TransportFilteringMailAccountStorage implements MailAccountStorageService {

    private final MailAccountStorageService delegate;

    /**
     * Initializes a new {@link TransportFilteringMailAccountStorage}.
     *
     * @param delegate
     */
    public TransportFilteringMailAccountStorage(MailAccountStorageService delegate) {
        super();
        this.delegate = delegate;
    }

    @Override
    public void invalidateMailAccount(int id, int userId, int contextId) throws OXException {
        delegate.invalidateMailAccount(id, userId, contextId);
    }

    @Override
    public void invalidateMailAccounts(int userId, int contextId) throws OXException {
        delegate.invalidateMailAccounts(userId, contextId);
    }

    @Override
    public void clearFullNamesForMailAccount(int id, int userId, int contextId) throws OXException {
        delegate.clearFullNamesForMailAccount(id, userId, contextId);
    }

    @Override
    public void clearFullNamesForMailAccount(int id, int[] indexes, int userId, int contextId) throws OXException {
        delegate.clearFullNamesForMailAccount(id, indexes, userId, contextId);
    }

    @Override
    public boolean setFullNamesForMailAccount(int id, int[] indexes, String[] fullNames, int userId, int contextId) throws OXException {
        return delegate.setFullNamesForMailAccount(id, indexes, fullNames, userId, contextId);
    }

    @Override
    public boolean setNamesForMailAccount(int id, int[] indexes, String[] names, int userId, int contextId) throws OXException {
        return delegate.setNamesForMailAccount(id, indexes, names, userId, contextId);
    }

    @Override
    public void propagateEvent(Event event, int id, Map<String, Object> eventProps, int userId, int contextId) throws OXException {
        delegate.propagateEvent(event, id, eventProps, userId, contextId);
    }

    @Override
    public boolean isSecondaryMailAccount(int id, int userId, int contextId) throws OXException {
        return delegate.isSecondaryMailAccount(id, userId, contextId);
    }

    @Override
    public boolean isDeactivatedMailAccount(int id, int userId, int contextId) throws OXException {
        return delegate.isDeactivatedMailAccount(id, userId, contextId);
    }

    @Override
    public boolean existsMailAccount(int id, int userId, int contextId) throws OXException {
        return delegate.existsMailAccount(id, userId, contextId);
    }

    @Override
    public MailAccount getMailAccount(int id, int userId, int contextId) throws OXException {
        MailAccount result = delegate.getMailAccount(id, userId, contextId);
        return filterTransportFromMailAccount(result, userId, contextId);
    }

    @Override
    public MailAccount getRawMailAccount(int id, int userId, int contextId) throws OXException {
        MailAccount result = delegate.getRawMailAccount(id, userId, contextId);
        return filterTransportFromMailAccount(result, userId, contextId);
    }

    @Override
    public MailAccount getMailAccount(int id, int userId, int contextId, Connection con) throws OXException {
        MailAccount result = delegate.getMailAccount(id, userId, contextId, con);
        return filterTransportFromMailAccount(result, userId, contextId);
    }

    @Override
    public TransportAccount getTransportAccount(int accountId, int userId, int contextId, Connection con) throws OXException {
        checkTransportAccount(accountId, userId, contextId);
        return delegate.getTransportAccount(accountId, userId, contextId, con);
    }

    @Override
    public TransportAccount getTransportAccount(int accountId, int userId, int contextId) throws OXException {
        checkTransportAccount(accountId, userId, contextId);
        return delegate.getTransportAccount(accountId, userId, contextId);
    }

    @Override
    public TransportAccount[] getUserTransportAccounts(int userId, int contextId) throws OXException {
        return filterTransportAccounts(delegate.getUserTransportAccounts(userId, contextId), userId, contextId);
    }

    @Override
    public TransportAccount[] getUserTransportAccounts(int userId, int contextId, Connection con) throws OXException {
        return filterTransportAccounts(delegate.getUserTransportAccounts(userId, contextId, con), userId, contextId);
    }

    @Override
    public MailAccount[] getUserMailAccounts(int userId, int contextId) throws OXException {
        List<MailAccount> result = Arrays.asList(delegate.getUserMailAccounts(userId, contextId));
        return filterTransportFromMailAccounts(result, userId, contextId).toArray(new MailAccount[result.size()]);
    }

    @Override
    public MailAccount[] getUserDefaultAndSecondaryMailAccounts(int userId, int contextId) throws OXException {
        // Primary and secondary transport accounts are always available. No filtering necessary
        return delegate.getUserDefaultAndSecondaryMailAccounts(userId, contextId);
    }

    @Override
    public List<MailAccount> getUserMailAccounts(int contextId) throws OXException {
        return delegate.getUserMailAccounts(contextId);
    }

    @Override
    public MailAccount[] getUserMailAccounts(int userId, int contextId, Connection con) throws OXException {
        List<MailAccount> result = Arrays.asList(delegate.getUserMailAccounts(userId, contextId, con));
        return filterTransportFromMailAccounts(result, userId, contextId).toArray(new MailAccount[result.size()]);
    }

    @Override
    public MailAccount[] getUserDefaultAndSecondaryMailAccounts(int userId, int contextId, Connection con) throws OXException {
        List<MailAccount> result = Arrays.asList(delegate.getUserDefaultAndSecondaryMailAccounts(userId, contextId, con));
        return filterTransportFromMailAccounts(result, userId, contextId).toArray(new MailAccount[result.size()]);
    }

    @Override
    public MailAccount getDefaultMailAccount(int userId, int contextId) throws OXException {
        // Transport for primary account is always available
        return delegate.getDefaultMailAccount(userId, contextId);
    }

    @Override
    public String getDefaultFolderPrefix(Session session) throws OXException {
        return delegate.getDefaultFolderPrefix(session);
    }

    @Override
    public char getDefaultSeparator(Session session) throws OXException {
        return delegate.getDefaultSeparator(session);
    }

    @Override
    public void enableMailAccount(int accountId, int userId, int contextId) throws OXException {
        delegate.enableMailAccount(accountId, userId, contextId);
    }

    @Override
    public void enableMailAccount(int accountId, int userId, int contextId, Connection con) throws OXException {
        delegate.enableMailAccount(accountId, userId, contextId, con);
    }

    @Override
    public void updateMailAccount(MailAccountDescription mailAccount, Set<Attribute> attributes, int userId, int contextId, Session session) throws OXException {
        checkMailAccountDescription(mailAccount, userId, contextId);
        delegate.updateMailAccount(mailAccount, attributes, userId, contextId, session);
    }

    @Override
    public void updateMailAccount(MailAccountDescription mailAccount, Set<Attribute> attributes, int userId, int contextId, Session session, Connection con, boolean changePrimary) throws OXException {
        checkMailAccountDescription(mailAccount, userId, contextId);
        delegate.updateMailAccount(mailAccount, attributes, userId, contextId, session, con, changePrimary);
    }

    @Override
    public void updateMailAccount(MailAccountDescription mailAccount, Set<Attribute> attributes, int userId, int contextId, UpdateProperties updateProperties) throws OXException {
        checkMailAccountDescription(mailAccount, userId, contextId);
        delegate.updateMailAccount(mailAccount, attributes, userId, contextId, updateProperties);
    }

    @Override
    public void updateMailAccount(MailAccountDescription mailAccount, int userId, int contextId, Session session) throws OXException {
        checkMailAccountDescription(mailAccount, userId, contextId);
        delegate.updateMailAccount(mailAccount, userId, contextId, session);
    }

    @Override
    public void updateTransportAccount(TransportAccountDescription transportAccount, int userId, int contextId, Session session) throws OXException {
        checkTransportAccount(transportAccount.getId(), userId, contextId);
        delegate.updateTransportAccount(transportAccount, userId, contextId, session);
    }

    @Override
    public void updateTransportAccount(TransportAccountDescription transportAccount, Set<Attribute> attributes, int userId, int contextId, Session session) throws OXException {
        checkTransportAccount(transportAccount.getId(), userId, contextId);
        delegate.updateTransportAccount(transportAccount, attributes, userId, contextId, session);
    }

    @Override
    public void updateTransportAccount(TransportAccountDescription transportAccount, Set<Attribute> attributes, int userId, int contextId, UpdateProperties updateProperties) throws OXException {
        checkTransportAccount(transportAccount.getId(), userId, contextId);
        delegate.updateTransportAccount(transportAccount, attributes, userId, contextId, updateProperties);
    }

    @Override
    public int acquireId(int userId, Context ctx) throws OXException {
        return delegate.acquireId(userId, ctx);
    }

    @Override
    public int insertMailAccount(MailAccountDescription mailAccount, int userId, Context ctx, Session session) throws OXException {
        checkMailAccountDescription(mailAccount, userId, ctx.getContextId());
        return delegate.insertMailAccount(mailAccount, userId, ctx, session);
    }

    @Override
    public int insertMailAccount(MailAccountDescription mailAccount, int userId, Context ctx, Session session, Connection con) throws OXException {
        checkMailAccountDescription(mailAccount, userId, ctx.getContextId());
        return delegate.insertMailAccount(mailAccount, userId, ctx, session, con);
    }

    @Override
    public int insertTransportAccount(TransportAccountDescription transportAccount, int userId, Context ctx, Session session) throws OXException {
        checkTransportAccount(transportAccount.getId(), userId, ctx.getContextId());
        return delegate.insertTransportAccount(transportAccount, userId, ctx, session);
    }

    @Override
    public boolean deleteMailAccount(int id, Map<String, Object> properties, int userId, int contextId) throws OXException {
        return delegate.deleteMailAccount(id, properties, userId, contextId);
    }

    @Override
    public void deleteAllMailAccounts(int userId, int contextId, Connection connection) throws OXException {
        delegate.deleteAllMailAccounts(userId, contextId, connection);
    }

    @Override
    public void deleteTransportAccount(int id, int userId, int contextId) throws OXException {
        delegate.deleteTransportAccount(id, userId, contextId);
    }

    @Override
    public void deleteTransportAccount(int id, int userId, int contextId, Connection con) throws OXException {
        delegate.deleteTransportAccount(id, userId, contextId, con);
    }

    @Override
    public boolean deleteMailAccount(int id, Map<String, Object> properties, int userId, int contextId, boolean deletePrimaryOrSecondary) throws OXException {
        return delegate.deleteMailAccount(id, properties, userId, contextId, deletePrimaryOrSecondary);
    }

    @Override
    public boolean deleteMailAccount(int id, Map<String, Object> properties, int userId, int contextId, boolean deletePrimaryOrSecondary, Connection con) throws OXException {
        return delegate.deleteMailAccount(id, properties, userId, contextId, deletePrimaryOrSecondary, con);
    }

    @Override
    public MailAccount[] resolveLogin(String login, int contextId) throws OXException {
        return delegate.resolveLogin(login, contextId);
    }

    @Override
    public MailAccount[] resolveLogin(String login, String serverUrl, int contextId) throws OXException {
        return delegate.resolveLogin(login, serverUrl, contextId);
    }

    @Override
    public MailAccount[] resolvePrimaryAddr(String primaryAddress, int contextId) throws OXException {
        return delegate.resolvePrimaryAddr(primaryAddress, contextId);
    }

    @Override
    public int getByPrimaryAddress(String primaryAddress, int userId, int contextId) throws OXException {
        // Transport for primary account is always available
        return delegate.getByPrimaryAddress(primaryAddress, userId, contextId);
    }

    @Override
    public int getTransportByPrimaryAddress(String primaryAddress, int userId, int contextId) throws OXException {
        // Transport for primary account is always available
        return delegate.getTransportByPrimaryAddress(primaryAddress, userId, contextId);
    }

    @Override
    public TransportAccount getTransportByReference(String reference, int userId, int contextId) throws OXException {
        TransportAccount result = delegate.getTransportByReference(reference, userId, contextId);
        checkTransportAccount(result.getId(), userId, contextId);
        return result;
    }

    @Override
    public boolean incrementFailedMailAuthCount(int accountId, int userId, int contextId, Exception optReason) throws OXException {
        return delegate.incrementFailedMailAuthCount(accountId, userId, contextId, optReason);
    }

    @Override
    public boolean incrementFailedTransportAuthCount(int accountId, int userId, int contextId, Exception optReason) throws OXException {
        return delegate.incrementFailedTransportAuthCount(accountId, userId, contextId, optReason);
    }

    @Override
    public int[] getByHostNames(Collection<String> hostNames, int userId, int contextId) throws OXException {
        return delegate.getByHostNames(hostNames, userId, contextId);
    }

    @Override
    public void migratePasswords(String oldSecret, String newSecret, Session session) throws OXException {
        delegate.migratePasswords(oldSecret, newSecret, session);

    }

    @Override
    public boolean hasAccounts(Session session) throws OXException {
        return delegate.hasAccounts(session);
    }

    @Override
    public void cleanUp(String secret, Session session) throws OXException {
        delegate.cleanUp(secret, session);

    }

    @Override
    public void removeUnrecoverableItems(String secret, Session session) throws OXException {
        delegate.removeUnrecoverableItems(secret, session);
    }

    // ----------------------------------------- private methods -------------------------------

    /**
     * Checks if the transport account is available for the given user
     *
     * @param accountId The account id
     * @param userId The user id
     * @param contextId The context id
     * @throws OXException if the account is not available or if the check fails
     */
    private void checkTransportAccount(int accountId, int userId, int contextId) throws OXException {
        if (isPrimaryOrSecondaryAccount(accountId, userId, contextId)) {
            // Primary/Secondary account is always available
            return;
        }
        checkExternalAccountsEnabled(userId, contextId);
    }

    /**
     * Checks if the given account is either a primary or secondary account
     *
     * @param accountId The account id to check
     * @param userId The user id
     * @param contextId The context id
     * @return <code>true</code> if the account id belongs to either a primary or secondary account, <code>false</code> otherwise
     * @throws OXException
     */
    private boolean isPrimaryOrSecondaryAccount(int accountId, int userId, int contextId) throws OXException {
        return Account.DEFAULT_ID == accountId || isSecondaryMailAccount(accountId, userId, contextId);
    }

    /**
     * Checks if the given {@link MailAccountDescription} is valid.
     *
     * @param mailAccount The mail account to check
     * @param userId The user id
     * @param contextId The context id
     * @throws OXException if the {@link LeanConfigurationService} is missing or if the account is invalid
     */
    private void checkMailAccountDescription(MailAccountDescription mailAccount, int userId, int contextId) throws OXException {
        if (isExternalAccountsEnabled(userId, contextId)) {
            return;
        }
        if (mailAccount.isDefaultFlag() // @formatter:off
            || mailAccount.isSecondaryAccount()
            || isPrimaryOrSecondaryAccount(mailAccount.getId(), userId, contextId)) { // @formatter:on
            return;
        }

        // Throw error in case transport fields are set
        if (mailAccount.getTransportAuthType() != null || // @formatter:off
            mailAccount.getTransportLogin() != null ||
            !mailAccount.getTransportProperties().isEmpty() ||
            mailAccount.getTransportPassword() != null ||
            mailAccount.getTransportServer() != null  ||
            mailAccount.getTransportOAuthId() > -1) { // @formatter:on
            throw MailAccountExceptionCodes.EXTERNAL_ACCOUNTS_DISABLED.create(I(userId), I(contextId));
        }
    }

    /**
     * Checks if the external accounts are enabled for the specified user in the specified context
     *
     * @param userId the user id
     * @param contextId The context id
     * @throws OXException if the {@link LeanConfigurationService} is absent, or if external accounts are disabled for the given user
     */
    private void checkExternalAccountsEnabled(int userId, int contextId) throws OXException {
        if (false == isExternalAccountsEnabled(userId, contextId)) {
            throw MailAccountExceptionCodes.EXTERNAL_ACCOUNTS_DISABLED.create(I(userId), I(contextId));
        }
    }

    /**
     * Filters out the transport information from the specified mail account
     *
     * @param mailAccount The mail account
     * @param userId The user id
     * @param contextId The context id
     * @return The filtered mail account
     * @throws OXException
     */
    private MailAccount filterTransportFromMailAccount(MailAccount mailAccount, int userId, int contextId) throws OXException {
        if (Account.DEFAULT_ID == mailAccount.getId()) {
            return mailAccount;
        }
        return isExternalAccountsEnabled(userId, contextId) ? mailAccount : new NoTransportMailAccountImpl(mailAccount);
    }

    /**
     * Filters out the transport information from the specified mail accounts
     *
     * @param mailAccounts The mail accounts
     * @param userId The user id
     * @param contextId The context id
     * @return The filtered mail accounts
     * @throws OXException
     */
    private List<MailAccount> filterTransportFromMailAccounts(List<MailAccount> mailAccounts, int userId, int contextId) throws OXException {
        boolean externalAccountsEnabled = isExternalAccountsEnabled(userId, contextId);
        if (externalAccountsEnabled) {
            return mailAccounts;
        }

        // @formatter:off
        return mailAccounts.stream()
                           .map(acc -> Account.DEFAULT_ID == acc.getId() ? acc : new NoTransportMailAccountImpl(acc))
                           .collect(Collectors.toList());
        // @formatter:on
    }

    /**
     * Checks if the external accounts are enabled for the specified user in the specified context
     *
     * @param userId the user id
     * @param contextId The context id
     * @param throwEx Whether to throw exception if the accounts are disabled
     * @return <code>true</code> if the external accounts are enabled; <code>false</code> otherwise
     * @throws OXException if the {@link LeanConfigurationService} is absent
     */
    private boolean isExternalAccountsEnabled(int userId, int contextId) throws OXException {
        LeanConfigurationService configService = ServerServiceRegistry.getInstance().getService(LeanConfigurationService.class, true);
        return configService.getBooleanProperty(userId, contextId, MailProperty.SMTP_ALLOW_EXTERNAL);
    }

    /**
     * In case {@link #isExternalAccountsEnabled(int, int, boolean)} is <code>true</code> this methods filters all external accounts.
     *
     * @param accounts The accounts to filter
     * @param userId The user id
     * @param contextId The context id
     * @return The filtered {@link TransportAccount} array
     * @throws OXException
     */
    private TransportAccount[] filterTransportAccounts(TransportAccount[] accounts, int userId, int contextId) throws OXException {
        if (isExternalAccountsEnabled(userId, contextId)) {
            return accounts;
        }
        List<TransportAccount> result = new ArrayList<TransportAccount>(accounts.length);
        for (TransportAccount acc : accounts) {
            if (isPrimaryOrSecondaryAccount(acc.getId(), userId, contextId)) {
                result.add(acc);
            }
        }
        return result.toArray(new TransportAccount[result.size()]);
    }

}
