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

import static com.openexchange.ajax.requesthandler.AJAXRequestDataBuilder.request;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.mail.internet.idn.IDNA;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONValue;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.requesthandler.AJAXActionService;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestDataTools;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.crypto.CryptoErrorMessage;
import com.openexchange.exception.OXException;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.utils.MailPasswordUtil;
import com.openexchange.mailaccount.Account;
import com.openexchange.mailaccount.Attribute;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.MailAccountDescription;
import com.openexchange.mailaccount.MailAccountExceptionCodes;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.mailaccount.Password;
import com.openexchange.mailaccount.TransportAccount;
import com.openexchange.mailaccount.TransportAuth;
import com.openexchange.mailaccount.json.ActiveProviderDetector;
import com.openexchange.mailaccount.json.MailAccountActionProvider;
import com.openexchange.mailaccount.json.MailAccountFields;
import com.openexchange.mailaccount.json.parser.DefaultMailAccountParser;
import com.openexchange.mailaccount.utils.MailAccountUtils;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.tools.net.URIDefaults;
import com.openexchange.tools.net.URITools;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link ValidateAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ValidateAction extends AbstractMailAccountTreeAction {

    public static final String ACTION = AJAXServlet.ACTION_VALIDATE;

    /**
     * Initializes a new {@link ValidateAction}.
     */
    public ValidateAction(ActiveProviderDetector activeProviderDetector) {
        super(activeProviderDetector);
    }

    @Override
    protected AJAXRequestResult innerPerform(final AJAXRequestData requestData, final ServerSession session, final JSONValue jData) throws OXException, JSONException {
        if (null == jData) {
            throw AjaxExceptionCodes.MISSING_REQUEST_BODY.create();
        }

        MailAccountDescription accountDescription = new MailAccountDescription();
        List<OXException> warnings = new LinkedList<OXException>();
        Set<Attribute> availableAttributes = DefaultMailAccountParser.getInstance().parse(accountDescription, jData.toObject(), warnings);

        if (isDefaultOrSecondaryMailAccount(accountDescription, session)) {
            return new AJAXRequestResult(Boolean.TRUE);
        }

        if (!session.getUserPermissionBits().isMultipleMailAccounts()) {
            throw MailAccountExceptionCodes.NOT_ENABLED.create(Integer.valueOf(session.getUserId()), Integer.valueOf(session.getContextId()));
        }

        if (!availableAttributes.contains(Attribute.TRANSPORT_AUTH_LITERAL)) {
            accountDescription.setTransportAuth(TransportAuth.MAIL);
            availableAttributes.add(Attribute.TRANSPORT_AUTH_LITERAL);
        }

        // Check for tree parameter
        boolean tree;
        {
            String tmp = requestData.getParameter("tree");
            tree = AJAXRequestDataTools.parseBoolParameter(tmp);
        }

        Password password = null;
        try {
            MailAccountStorageService storageService = ServerServiceRegistry.getInstance().getService(MailAccountStorageService.class);

            if (accountDescription.getId() >= 0) {
                MailAccountActionProvider activeProvider = Account.DEFAULT_ID == accountDescription.getId() ? null : optActiveProvider(session);
                if (null == activeProvider) {
                    MailAccount storageMailAccount = storageService.getMailAccount(accountDescription.getId(), session.getUserId(), session.getContextId());

                    boolean checkPassword = true;
                    if (null == accountDescription.getPassword()) {
                        checkPassword = false;
                        // Identifier is given, but password not set. Thus load from storage version.
                        try {
                            String passwd = storageMailAccount.getPassword();
                            if (null != passwd) {
                                String decryptedPassword = MailPasswordUtil.decrypt(passwd, session, accountDescription.getId(), accountDescription.getLogin(), accountDescription.getMailServer());
                                accountDescription.setPassword(decryptedPassword);
                            }
                        } catch (OXException e) {
                            if (!CryptoErrorMessage.BadPassword.equals(e)) {
                                throw e;
                            }
                            storageService.invalidateMailAccounts(session.getUserId(), session.getContextId());
                            storageMailAccount = storageService.getMailAccount(accountDescription.getId(), session.getUserId(), session.getContextId());
                            String decryptedPassword = MailPasswordUtil.decrypt(storageMailAccount.getPassword(), session, accountDescription.getId(), accountDescription.getLogin(), accountDescription.getMailServer());
                            accountDescription.setPassword(decryptedPassword);
                        }
                    }

                    // Check for any modifications that would justify validation
                    if (!tree && !hasValidationReason(accountDescription, storageMailAccount, checkPassword, session)) {
                        return new AJAXRequestResult(Boolean.TRUE);
                    }
                } else {
                    password = activeProvider.getPassword(Integer.toString(accountDescription.getId()), session);

                    String passwd;
                    if (Password.Type.ENCRYPTED == password.getType()) {
                        passwd = MailPasswordUtil.decrypt(new String(password.getPassword()), session, accountDescription.getId(), accountDescription.getLogin(), accountDescription.getMailServer());
                    } else {
                        passwd = new String(password.getPassword());
                    }

                    JSONObject jAccount;
                    {
                        AJAXActionService getAction = activeProvider.getAction(GetAction.ACTION);
                        AJAXRequestData getActionRequestData = request(GetAction.ACTION, "account", session).format("json").params(AJAXServlet.PARAMETER_ID, Integer.toString(accountDescription.getId())).build(requestData);
                        jAccount = (JSONObject) getAction.perform(getActionRequestData, session).getResultObject();
                    }

                    boolean checkPassword = true;
                    if (null == accountDescription.getPassword()) {
                        checkPassword = false;
                        accountDescription.setPassword(passwd);
                    }

                    // Check for any modifications that would justify validation
                    if (!tree && !hasValidationReason(accountDescription, jAccount, checkPassword, passwd, storageService, session)) {
                        return new AJAXRequestResult(Boolean.TRUE);
                    }

                    accountDescription.setId(-1);
                }
            }

            checkNeededFields(accountDescription, false);

            if (MailAccountUtils.isUnifiedINBOXAccount(accountDescription.getMailProtocol())) {
                // Deny validation of Unified Mail account
                throw MailAccountExceptionCodes.UNIFIED_INBOX_ACCOUNT_VALIDATION_FAILED.create();
            }

            boolean ignoreInvalidTransport;
            {
                String tmp = requestData.getParameter("ignoreInvalidTransport");
                ignoreInvalidTransport = AJAXRequestDataTools.parseBoolParameter(tmp);
            }

            if (tree) {
                return new AJAXRequestResult(actionValidateTree(accountDescription, session, ignoreInvalidTransport, warnings)).addWarnings(warnings);
            }
            return new AJAXRequestResult(actionValidateBoolean(accountDescription, session, ignoreInvalidTransport, warnings, false)).addWarnings(warnings);
        } finally {
            Streams.close(password);
        }
    }

    private static boolean hasValidationReason(MailAccountDescription accountDescription, JSONObject jAccount, boolean checkPassword, String passwd, MailAccountStorageService storageService, ServerSession session) throws OXException {
        String s1 = generateMailServerURL(jAccount);
        String s2 = accountDescription.generateMailServerURL();
        if (null == s1) {
            if (null != s2) {
                return true;
            }
        } else if (!s1.equals(s2)) {
            return true;
        }

        s1 = generateTransportServerURL(jAccount);
        s2 = accountDescription.generateTransportServerURL();
        if (null == s1) {
            if (null != s2) {
                return true;
            }
        } else if (!s1.equals(s2)) {
            return true;
        }

        s1 = jAccount.optString(MailAccountFields.LOGIN, null);
        s2 = accountDescription.getLogin();
        if (null == s1) {
            if (null != s2) {
                return true;
            }
        } else if (!s1.equals(s2)) {
            return true;
        }

        if (checkPassword) {
            s1 = passwd;
            s2 = accountDescription.getPassword();
            if (null == s1) {
                if (null != s2) {
                    return true;
                }
            } else if (!s1.equals(s2)) {
                return true;
            }
        }

        s2 = accountDescription.getTransportLogin();
        if (null != s2) {
            s1 = jAccount.optString(MailAccountFields.TRANSPORT_LOGIN, null);
            if (!s2.equals(s1)) {
                return true;
            }
        }

        s2 = accountDescription.getTransportPassword();
        if (null != s2) {
            TransportAccount transportAccount = storageService.getTransportAccount(accountDescription.getId(), session.getUserId(), session.getContextId());
            s1 = transportAccount.getTransportPassword();
            if (null != s1) {
                try {
                    s1 = MailPasswordUtil.decrypt(s1, session, accountDescription.getId(), accountDescription.getLogin(), accountDescription.getMailServer());
                } catch (OXException e) {
                    if (!CryptoErrorMessage.BadPassword.equals(e)) {
                        throw e;
                    }
                    // Password cannot be decrypted
                    s1 = null;
                }
                if (!s2.equals(s1)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean hasValidationReason(MailAccountDescription accountDescription, MailAccount storageMailAccount, boolean checkPassword, ServerSession session) throws OXException {
        String s1 = storageMailAccount.generateMailServerURL();
        String s2 = accountDescription.generateMailServerURL();
        if (null == s1) {
            if (null != s2) {
                return true;
            }
        } else if (!s1.equals(s2)) {
            return true;
        }

        s1 = storageMailAccount.generateTransportServerURL();
        s2 = accountDescription.generateTransportServerURL();
        if (null == s1) {
            if (null != s2) {
                return true;
            }
        } else if (!s1.equals(s2)) {
            return true;
        }

        s1 = storageMailAccount.getLogin();
        s2 = accountDescription.getLogin();
        if (null == s1) {
            if (null != s2) {
                return true;
            }
        } else if (!s1.equals(s2)) {
            return true;
        }

        if (checkPassword) {
            try {
                s1 = MailPasswordUtil.decrypt(storageMailAccount.getPassword(), session, accountDescription.getId(), accountDescription.getLogin(), accountDescription.getMailServer());
            } catch (OXException e) {
                if (!CryptoErrorMessage.BadPassword.equals(e)) {
                    throw e;
                }
                // Password cannot be decrypted
                s1 = null;
            }
            s2 = accountDescription.getPassword();
            if (null == s1) {
                if (null != s2) {
                    return true;
                }
            } else if (!s1.equals(s2)) {
                return true;
            }
        }

        s2 = accountDescription.getTransportLogin();
        if (null != s2) {
            s1 = storageMailAccount.getTransportLogin();
            if (!s2.equals(s1)) {
                return true;
            }
        }

        s2 = accountDescription.getTransportPassword();
        if (null != s2) {
            s1 = storageMailAccount.getTransportPassword();
            if (null != s1) {
                try {
                    s1 = MailPasswordUtil.decrypt(s1, session, accountDescription.getId(), accountDescription.getLogin(), accountDescription.getMailServer());
                } catch (OXException e) {
                    if (!CryptoErrorMessage.BadPassword.equals(e)) {
                        throw e;
                    }
                    // Password cannot be decrypted
                    s1 = null;
                }
                if (!s2.equals(s1)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static Object actionValidateTree(final MailAccountDescription accountDescription, final ServerSession session, final boolean ignoreInvalidTransport, final List<OXException> warnings) throws JSONException, OXException {
        if (!actionValidateBoolean(accountDescription, session, ignoreInvalidTransport, warnings, false).booleanValue()) {
            // TODO: How to indicate error if folder tree requested?
            return null;
        }
        // Create a mail access instance
        final MailAccess<?, ?> mailAccess = getMailAccess(accountDescription, session, warnings);
        if (null == mailAccess) {
            return JSONObject.NULL;
        }
        return actionValidateTree0(mailAccess, session);
    }

    /**
     * Validates specified account description.
     *
     * @param accountDescription The account description
     * @param session The associated session
     * @param ignoreInvalidTransport
     * @param warnings The warnings list
     * @param errorOnDenied <code>true</code> to throw an error in case account description is denied (either by host or port); otherwise <code>false</code>
     * @return <code>true</code> for successful validation; otherwise <code>false</code>
     * @throws OXException If an severe error occurs
     */
    public static Boolean actionValidateBoolean(MailAccountDescription accountDescription, ServerSession session, boolean ignoreInvalidTransport, List<OXException> warnings, boolean errorOnDenied) throws OXException {
        // Check for primary account
        if (Account.DEFAULT_ID == accountDescription.getId()) {
            return Boolean.TRUE;
        }
        // Validate mail server
        boolean validated = checkMailServerURL(accountDescription, session, warnings, errorOnDenied);
        // Failed?
        if (!validated) {
            checkForCommunicationProblem(warnings, false, accountDescription);
            return Boolean.FALSE;
        }
        if (ignoreInvalidTransport) {
            // No need to check transport settings then
            return Boolean.TRUE;
        }
        // Now check transport server URL, if a transport server is present
        if (Strings.isNotEmpty(accountDescription.getTransportServer())) {
            validated = checkTransportServerURL(accountDescription, session, warnings, errorOnDenied);
            if (!validated) {
                checkForCommunicationProblem(warnings, true, accountDescription);
                return Boolean.FALSE;
            }
        }
        return Boolean.valueOf(validated);
    }

    /**
     * Validates mail transport from specified account description.
     *
     * @param accountDescription The account description
     * @param session The associated session
     * @param ignoreInvalidTransport
     * @param warnings The warnings list
     * @param errorOnDenied <code>true</code> to throw an error in case account description is denied (either by host or port); otherwise <code>false</code>
     * @return <code>true</code> for successful validation; otherwise <code>false</code>
     * @throws OXException If an severe error occurs
     */
    public static Boolean actionValidateMailTransportBoolean(MailAccountDescription accountDescription, ServerSession session, List<OXException> warnings, boolean errorOnDenied) throws OXException {
        // Check for primary account
        if (Account.DEFAULT_ID == accountDescription.getId()) {
            return Boolean.TRUE;
        }
        // Check transport server URL, if a transport server is present
        boolean validated = true;
        if (Strings.isNotEmpty(accountDescription.getTransportServer())) {
            validated = checkTransportServerURL(accountDescription, session, warnings, errorOnDenied);
            if (!validated) {
                checkForCommunicationProblem(warnings, true, accountDescription);
                return Boolean.FALSE;
            }
        }
        return Boolean.valueOf(validated);
    }

    private static String generateMailServerURL(JSONObject jAccount) throws OXException {
        String mailServer = jAccount.optString(MailAccountFields.MAIL_SERVER, null);
        if (com.openexchange.java.Strings.isEmpty(mailServer)) {
            return null;
        }

        boolean mailSecure = jAccount.optBoolean(MailAccountFields.MAIL_SECURE, false);
        String mailProtocol = jAccount.optString(MailAccountFields.MAIL_PROTOCOL, null);
        int mailPort = jAccount.optInt(MailAccountFields.MAIL_PORT, mailSecure ? URIDefaults.IMAP.getPort() : URIDefaults.IMAP.getSSLPort());
        try {
            return URITools.generateURI(mailSecure ? mailProtocol + 's' : mailProtocol, IDNA.toASCII(mailServer), mailPort).toString();
        } catch (URISyntaxException e) {
            final StringBuilder sb = new StringBuilder(32);
            sb.append(mailProtocol);
            if (mailSecure) {
                sb.append('s');
            }
            throw MailAccountExceptionCodes.INVALID_HOST_NAME.create(e, sb.append("://").append(mailServer).append(':').append(mailPort).toString());
        }
    }

    private static String generateTransportServerURL(JSONObject jAccount) throws OXException {
        String transportServer = jAccount.optString(MailAccountFields.TRANSPORT_SERVER, null);
        if (com.openexchange.java.Strings.isEmpty(transportServer)) {
            return null;
        }

        boolean transportSecure = jAccount.optBoolean(MailAccountFields.TRANSPORT_SECURE, false);
        String transportProtocol = jAccount.optString(MailAccountFields.TRANSPORT_PROTOCOL, null);
        int transportPort = jAccount.optInt(MailAccountFields.TRANSPORT_PORT, transportSecure ? URIDefaults.SMTP.getPort() : URIDefaults.SMTP.getSSLPort());
        try {
            return URITools.generateURI(transportSecure ? transportProtocol + 's' : transportProtocol, IDNA.toASCII(transportServer), transportPort).toString();
        } catch (URISyntaxException e) {
            final StringBuilder sb = new StringBuilder(32);
            sb.append(transportProtocol);
            if (transportSecure) {
                sb.append('s');
            }
            throw MailAccountExceptionCodes.INVALID_HOST_NAME.create(e, sb.append("://").append(transportServer).append(':').append(transportPort).toString());
        }
    }

}
