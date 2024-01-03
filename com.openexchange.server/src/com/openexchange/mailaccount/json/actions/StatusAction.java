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

package com.openexchange.mailaccount.json.actions;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.mail.api.MailConfig.determinePasswordAndAuthType;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.json.JSONObject;
import org.json.JSONValue;
import org.slf4j.Logger;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction;
import com.openexchange.crypto.CryptoErrorMessage;
import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.mail.api.AuthInfo;
import com.openexchange.mailaccount.Account;
import com.openexchange.mailaccount.KnownStatus;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.MailAccountDescription;
import com.openexchange.mailaccount.MailAccountExceptionCodes;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.mailaccount.MailAccounts;
import com.openexchange.mailaccount.Status;
import com.openexchange.mailaccount.TransportAuth;
import com.openexchange.mailaccount.UnifiedInboxManagement;
import com.openexchange.mailaccount.json.ActiveProviderDetector;
import com.openexchange.mailaccount.json.MailAccountFields;
import com.openexchange.mailaccount.utils.MailAccountUtils;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link StatusAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
@RestrictedAction(module = AbstractMailAccountAction.MODULE, type = RestrictedAction.Type.READ)
public final class StatusAction extends AbstractValidateMailAccountAction implements MailAccountFields {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(StatusAction.class);
    }

    public static final String ACTION = "status";

    /**
     * Initializes a new {@link StatusAction}.
     */
    public StatusAction(ActiveProviderDetector activeProviderDetector) {
        super(activeProviderDetector);
    }

    @Override
    protected AJAXRequestResult innerPerform(final AJAXRequestData requestData, final ServerSession session, final JSONValue jVoid) throws OXException {
        MailAccountStorageService storageService = ServerServiceRegistry.getInstance().getService(MailAccountStorageService.class, true);
        List<OXException> warnings = new LinkedList<>();

        {
            int id = optionalIntParameter(AJAXServlet.PARAMETER_ID, -1, requestData);
            if (id >= 0) {
                if (isDefaultOrSecondaryMailAccount(id, session)) {
                    // Primary/secondary is always allowed
                    return getStatusFor(id, session, storageService, warnings);
                }
                if (!session.getUserPermissionBits().isMultipleMailAccounts()) {
                    throw MailAccountExceptionCodes.NOT_ENABLED.create(Integer.valueOf(session.getUserId()), Integer.valueOf(session.getContextId()));
                }
                return getStatusFor(id, session, storageService, warnings);
            }
        }

        // Get status for all mail accounts
        MailAccount[] accounts;
        if (session.getUserPermissionBits().isMultipleMailAccounts()) {
            accounts = storageService.getUserMailAccounts(session.getUserId(), session.getContextId());
        } else {
            accounts = storageService.getUserDefaultAndSecondaryMailAccounts(session.getUserId(), session.getContextId());
        }

        JSONObject jStatuses = new JSONObject(accounts.length);

        Locale locale = session.getUser().getLocale();
        for (MailAccount account : accounts) {
            Status status = determineAccountStatus(account, false, warnings, session);
            jStatuses.putSafe(String.valueOf(account.getId()), serialize(status, locale));
        }
        return new AJAXRequestResult(jStatuses, "json").addWarnings(warnings);
    }

    private static AJAXRequestResult getStatusFor(int id, final ServerSession session, MailAccountStorageService storageService, List<OXException> warnings) throws OXException {
        if (Account.DEFAULT_ID != id && !MailAccounts.isSecondaryAccount(id, session) && !session.getUserPermissionBits().isMultipleMailAccounts()) {
            UnifiedInboxManagement unifiedInboxManagement = ServerServiceRegistry.getInstance().getService(UnifiedInboxManagement.class);
            if ((null == unifiedInboxManagement) || (id != unifiedInboxManagement.getUnifiedINBOXAccountID(session))) {
                throw MailAccountExceptionCodes.NOT_ENABLED.create(Integer.valueOf(session.getUserId()), Integer.valueOf(session.getContextId()));
            }
        }

        Status status = determineAccountStatus(id, true, storageService, warnings, session);
        JSONObject jStatus = serialize(status, session.getUser().getLocale());
        return new AJAXRequestResult(new JSONObject(2).putSafe(Integer.toString(id), jStatus), "json").addWarnings(warnings);
    }

    private static Status determineAccountStatus(int id, boolean singleRequested, MailAccountStorageService storageService, List<OXException> warnings, ServerSession session) throws OXException {
        MailAccount mailAccount = storageService.getMailAccount(id, session.getUserId(), session.getContextId());
        return determineAccountStatus(mailAccount, singleRequested, warnings, session);
    }

    private static Status determineAccountStatus(MailAccount mailAccount, boolean singleRequested, List<OXException> warnings, ServerSession session) {
        try {
            if (MailAccountUtils.isUnifiedINBOXAccount(mailAccount)) {
                // Treat as no hit
                if (singleRequested) {
                    throw MailAccountExceptionCodes.NOT_FOUND.create(Integer.valueOf(mailAccount.getId()), Integer.valueOf(session.getUserId()), Integer.valueOf(session.getContextId()));
                }
                return null;
            }

            if (mailAccount.isMailDisabled()) {
                return KnownStatus.DISABLED;
            }

            if (mailAccount.isDeactivated()) {
                return KnownStatus.DEACTIVATED;
            }

            return checkStatus(mailAccount, session, false, warnings);
        } catch (OXException e) {
            return getStatusFor(mailAccount, e);
        }
    }

    /**
     * Validates specified account description.
     *
     * @param account The account to check
     * @param session The associated session
     * @param ignoreInvalidTransport
     * @param warnings The warnings list
     * @return <code>true</code> for successful validation; otherwise <code>false</code>
     * @throws OXException If an severe error occurs
     */
    public static KnownStatus checkStatus(MailAccount account, ServerSession session, boolean ignoreInvalidTransport, List<OXException> warnings) throws OXException {
        // Check for primary account
        if (Account.DEFAULT_ID == account.getId() || account.isDefaultOrSecondaryAccount()) {
            return KnownStatus.OK;
        }

        boolean ignoreTransport = ignoreInvalidTransport;

        MailAccountDescription accountDescription = new MailAccountDescription();
        accountDescription.setId(account.getId());
        accountDescription.setMailServer(account.getMailServer());
        accountDescription.setMailPort(account.getMailPort());
        accountDescription.setMailOAuthId(account.getMailOAuthId());
        accountDescription.setMailSecure(account.isMailSecure());
        accountDescription.setMailProtocol(account.getMailProtocol());
        accountDescription.setMailStartTls(account.isMailStartTls());
        accountDescription.setLogin(account.getLogin());
        accountDescription.setPrimaryAddress(account.getPrimaryAddress());
        try {
            AuthInfo authInfo = determinePasswordAndAuthType(account.getLogin(), session, account, true);
            accountDescription.setPassword(authInfo.getPassword());
            accountDescription.setAuthType(authInfo.getAuthType());
        } catch (OXException e) {
            if (!CryptoErrorMessage.BadPassword.equals(e)) {
                throw e;
            }
            return KnownStatus.INVALID_CREDENTIALS;
        }

        if (Strings.isNotEmpty(account.getTransportServer())) {
            if (TransportAuth.NONE == account.getTransportAuth()) {
                if (ignoreTransport) {
                    return checkStatusWithoutTransport(accountDescription, session, warnings);
                }
                return checkStatusWithTransport(accountDescription, session, warnings);
            }

            accountDescription.setTransportServer(account.getTransportServer());
            accountDescription.setTransportPort(account.getTransportPort());
            accountDescription.setTransportOAuthId(account.getTransportOAuthId());
            accountDescription.setTransportSecure(account.isTransportSecure());
            accountDescription.setTransportProtocol(account.getTransportProtocol());
            accountDescription.setTransportStartTls(account.isTransportStartTls());

            if (TransportAuth.MAIL == account.getTransportAuth()) {
                accountDescription.setTransportLogin(accountDescription.getLogin());
                accountDescription.setTransportPassword(accountDescription.getPassword());
                accountDescription.setTransportAuthType(accountDescription.getAuthType());
                ignoreTransport = true;
            } else {
                String transportLogin = account.getTransportLogin();
                accountDescription.setTransportLogin(transportLogin);
                try {
                    AuthInfo authInfo = determinePasswordAndAuthType(transportLogin, session, account, false);
                    accountDescription.setTransportPassword(authInfo.getPassword());
                    accountDescription.setTransportAuthType(authInfo.getAuthType());
                } catch (OXException e) {
                    if (!CryptoErrorMessage.BadPassword.equals(e)) {
                        throw e;
                    }
                    return KnownStatus.INVALID_CREDENTIALS;
                }
            }
        }

        if (ignoreTransport) {
            return checkStatusWithoutTransport(accountDescription, session, warnings);
        }
        return checkStatusWithTransport(accountDescription, session, warnings);
    }

    /**
     * Validates specified account description.
     *
     * @param accountDescription The account description
     * @param session The associated session
     * @param warnings The warnings list
     * @return <code>true</code> for successful validation; otherwise <code>false</code>
     * @throws OXException If an severe error occurs
     */
    public static KnownStatus checkStatusWithoutTransport(MailAccountDescription accountDescription, ServerSession session, List<OXException> warnings) throws OXException {
        // Check for primary account
        if (Account.DEFAULT_ID == accountDescription.getId()) {
            return KnownStatus.OK;
        }
        // Validate mail server
        boolean validated = checkMailServerURL(accountDescription, session, warnings, false, false);
        // Failed?
        if (!validated) {
            Optional<KnownStatus> optStatus = testForCommunicationProblem(warnings, false, accountDescription);
            return optStatus.orElse(KnownStatus.INVALID_CREDENTIALS);
        }
        // No need to check transport settings here
        return KnownStatus.OK;
    }

    /**
     * Validates specified account description.
     *
     * @param accountDescription The account description
     * @param session The associated session
     * @param ignoreInvalidTransport
     * @param warnings The warnings list
     * @return <code>true</code> for successful validation; otherwise <code>false</code>
     * @throws OXException If an severe error occurs
     */
    public static KnownStatus checkStatusWithTransport(MailAccountDescription accountDescription, ServerSession session, List<OXException> warnings) throws OXException {
        KnownStatus knownStatus = checkStatusWithoutTransport(accountDescription, session, warnings);
        if (KnownStatus.OK != knownStatus) {
            return knownStatus;
        }
        // Now check transport server URL, if a transport server is present
        boolean validated;
        if (Strings.isNotEmpty(accountDescription.getTransportServer())) {
            validated = checkTransportServerURL(accountDescription, session, warnings, false, false);
            if (!validated) {
                Optional<KnownStatus> optStatus = testForCommunicationProblem(warnings, true, accountDescription);
                return optStatus.orElse(KnownStatus.INVALID_CREDENTIALS);
            }
        }
        return KnownStatus.OK ;
    }

    private static Optional<KnownStatus> testForCommunicationProblem(List<OXException> warnings, boolean transport, MailAccountDescription accountDescription) {
        if (null != warnings && !warnings.isEmpty()) {
            OXException warning = warnings.get(0);
            LoggerHolder.LOG.debug("Encountered a warning while checking status for mail account {} ({})", accountDescription.getPrimaryAddress(), I(accountDescription.getId()), warning);
            if (indicatesCommunicationProblem(warning.getCause())) {
                OXException newWarning;
                if (transport) {
                    String login = accountDescription.getTransportLogin();
                    if (!seemsValid(login)) {
                        login = accountDescription.getLogin();
                    }
                    newWarning = MailAccountExceptionCodes.VALIDATE_FAILED_TRANSPORT.create(accountDescription.getTransportServer(), login);
                } else {
                    newWarning = MailAccountExceptionCodes.VALIDATE_FAILED_MAIL.create(accountDescription.getMailServer(), accountDescription.getLogin());
                }
                newWarning.setCategory(Category.CATEGORY_WARNING);
                warnings.clear();
                warnings.add(newWarning);
                return Optional.of(KnownStatus.COMMUNICATION_PROBLEM);
            } else if (indicatesSSLProblem(warning)) {
                return Optional.of(KnownStatus.INVALID_SSL_CERTIFICATE);
            }
        }
        return Optional.empty();
    }

    /**
     * Serializes an account status to JSON.
     *
     * @param status The status to serialize
     * @param locale The locale to use for translations
     * @return The serialized status as JSON object
     */
    private static JSONObject serialize(Status status, Locale locale) {
        if (null == status) {
            return null;
        }
        JSONObject jsonObject = new JSONObject(4);
        jsonObject.putSafe("status", status.getId());
        jsonObject.putSafe("message", status.getMessage(locale));
        return jsonObject;
    }

    /**
     * Optionally gets an error status for an encountered exception when trying to access the mail account.
     *
     * @param account The mail account being accessed
     * @param error The error that occurred
     * @return An appropriate error status
     */
    private static Status getStatusFor(MailAccount account, OXException error) {
        switch (error.getErrorCode()) {
            case "CRP-0001":
                // Wrong Password
                LoggerHolder.LOG.debug("Invalid credentials while checking status for mail account {} ({})", account.getPrimaryAddress(), I(account.getId()), error);
                return KnownStatus.INVALID_CREDENTIALS;
            case "OAUTH-0042":
                // Required scopes are not authorized by user
                LoggerHolder.LOG.debug("Required scopes are not authorized by user and therefore checking status for mail account {} ({}) failed", account.getPrimaryAddress(), I(account.getId()), error);
                return KnownStatus.INACCESSIBLE(error);
            case "OAUTH-0043":
                // Required scopes are not available. Either not offered by service or explicitly disabled
                // (e.g. com.openexchange.oauth.modules.enabled.google=)
                LoggerHolder.LOG.debug("Required scopes are not available and therefore checking status for mail account {} ({}) failed", account.getPrimaryAddress(), I(account.getId()), error);
                return KnownStatus.UNSUPPORTED(error);
            case "OAUTH-0044":
                // OAuth provider has been disabled (e.g. com.openexchange.oauth.google=false)
                LoggerHolder.LOG.debug("OAuth provider has been disabled and therefore checking status for mail account {} ({}) failed", account.getPrimaryAddress(), I(account.getId()), error);
                return KnownStatus.UNSUPPORTED(error);
            default:
                LoggerHolder.LOG.warn("Unexpected error while checking status for mail account {} ({})", account.getPrimaryAddress(), I(account.getId()), error);
                return KnownStatus.UNKNOWN(error);
        }
    }

}
