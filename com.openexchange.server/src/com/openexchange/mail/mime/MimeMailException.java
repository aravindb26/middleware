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

package com.openexchange.mail.mime;

import static com.openexchange.exception.ExceptionUtils.isEitherOf;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.mail.MailServletInterface.mailInterfaceMonitor;
import static com.sun.mail.iap.ResponseCode.ALERT;
import static com.sun.mail.iap.ResponseCode.AUTHENTICATIONFAILED;
import java.io.IOException;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.MessageRemovedException;
import javax.mail.MessagingException;
import javax.mail.Store;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import com.openexchange.exception.OXException;
import com.openexchange.java.IOs;
import com.openexchange.java.Strings;
import com.openexchange.log.LogProperties;
import com.openexchange.log.LogProperties.Name;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.api.AuthType;
import com.openexchange.mail.api.MailConfig;
import com.openexchange.mailaccount.Account;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.oauth.API;
import com.openexchange.oauth.OAuthAccount;
import com.openexchange.oauth.OAuthExceptionCodes;
import com.openexchange.oauth.OAuthService;
import com.openexchange.osgi.ServiceListing;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.tools.exceptions.ExceptionUtils;
import com.sun.mail.iap.CommandFailedException;
import com.sun.mail.iap.Response;
import com.sun.mail.iap.ResponseCode;
import com.sun.mail.smtp.SMTPAddressFailedException;
import com.sun.mail.smtp.SMTPSendFailedException;
import com.sun.mail.smtp.SMTPSendTimedoutException;
import com.sun.mail.smtp.SMTPSenderFailedException;

/**
 * {@link OXException} - For MIME related errors.
 * <p>
 * Taken from {@link MailExceptionCode}:
 * <p>
 * The detail number range in subclasses generated in mail bundles is supposed to start with 2000 and may go up to 2999.
 * <p>
 * The detail number range in subclasses generated in transport bundles is supposed to start with 3000 and may go up to 3999.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class MimeMailException extends OXException {

    private static final transient org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MimeMailException.class);

    private static final long serialVersionUID = -3401580182929349354L;

    private static final AtomicReference<ServiceListing<MimeMailExceptionHandler>> EXCEPTION_HANDLERS_REF = new AtomicReference<ServiceListing<MimeMailExceptionHandler>>(null);

    /**
     * Sets the given exception handlers.
     *
     * @param handlers The handlers to set
     */
    public static void setExceptionHandlers(ServiceListing<MimeMailExceptionHandler> handlers) {
        EXCEPTION_HANDLERS_REF.set(handlers);
    }

    /**
     * Unsets the given exception handlers.
     *
     * @param handlers The handlers to set
     */
    public static void unsetExceptionHandlers() {
        EXCEPTION_HANDLERS_REF.set(null);
    }

    /**
     * Initializes a new {@link MimeMailException}.
     *
     * @param code
     * @param displayMessage
     * @param displayArgs
     */
    public MimeMailException(int code, String displayMessage, Object... displayArgs) {
        super(code, displayMessage, displayArgs);
    }

    /**
     * Initializes a new {@link MimeMailException}.
     *
     * @param code
     * @param displayMessage
     * @param cause
     * @param displayArgs
     */
    public MimeMailException(int code, String displayMessage, Throwable cause, Object... displayArgs) {
        super(code, displayMessage, cause, displayArgs);
    }

    /**
     * Handles given instance of {@link MessagingException} and creates an appropriate instance of {@link OXException}
     * <p>
     * This is just a convenience method that simply invokes {@link #handleMessagingException(MessagingException, MailConfig)} with the
     * latter parameter set to <code>null</code>.
     *
     * @param e The messaging exception
     * @return An appropriate instance of {@link OXException}
     */
    public static OXException handleMessagingException(MessagingException e) {
        return handleMessagingException(e, null, null);
    }

    /**
     * Handles given instance of {@link MessagingException} and creates an appropriate instance of {@link OXException}
     *
     * @param e The messaging exception
     * @param mailConfig The corresponding mail configuration used to add information like mail server etc.
     * @return An appropriate instance of {@link OXException}
     */
    public static OXException handleMessagingException(MessagingException e, MailConfig mailConfig) {
        return handleMessagingException(e, mailConfig, mailConfig.getSession());
    }

    /**
     * Handles given instance of {@link MessagingException} and creates an appropriate instance of {@link OXException}
     *
     * @param e The messaging exception
     * @param mailConfig The corresponding mail configuration used to add information like mail server etc.
     * @param session The session providing user information
     * @return An appropriate instance of {@link OXException}
     */
    public static OXException handleMessagingException(MessagingException e, MailConfig mailConfig, Session session) {
        return handleMessagingException(e, mailConfig, session, null);
    }

    private static final String STR_EMPTY = "";

    private static final String ERR_TMP = "temporary error, please try again later";

    private static final String ERR_TMP_FLR = "temporary failure";

    private static final String ERR_AUTH_FAILED = "bad authentication failed";

    private static final String ERR_MSG_TOO_LARGE = "message too large";

    private static final String ERR_QUOTA = "quota";

    /**
     * ConnectionResetException
     */
    private static final String EXC_CONNECTION_RESET_EXCEPTION = "ConnectionResetException";

    private static final Object[] EMPTY_ARGS = new Object[0];

    /** The SMTP error code <code>552</code> */
    private static final int SMTP_ERROR_CODE_552 = 552;

    /**
     * Handles given instance of {@link MessagingException} and creates an appropriate instance of {@link OXException}
     *
     * @param e The messaging exception
     * @param mailConfig The corresponding mail configuration used to add information like mail server etc.
     * @param session The session providing user information
     * @param folder The optional folder
     * @return An appropriate instance of {@link OXException}
     */
    public static OXException handleMessagingException(MessagingException e, MailConfig mailConfig, Session session, Folder folder) {
        try {
            // Put log properties
            if (null != mailConfig) {
                LogProperties.put(Name.MAIL_ACCOUNT_ID, Integer.valueOf(mailConfig.getAccountId()));
                LogProperties.put(Name.MAIL_HOST, mailConfig.getServer());
                LogProperties.put(Name.MAIL_LOGIN, mailConfig.getLogin());
                if (null != folder) {
                    LogProperties.put(Name.MAIL_FULL_NAME, folder.getFullName());
                }
            }
            // Consult exception handlers first
            {
                ServiceListing<MimeMailExceptionHandler> handlers = EXCEPTION_HANDLERS_REF.get();
                if (null != handlers) {
                    OXException handled = null;
                    for (MimeMailExceptionHandler handler : handlers) {
                        handled = handler.handle(e, mailConfig, session, folder);
                        if (null != handled) {
                            return handled;
                        }
                    }
                }
            }
            // Start examining MessageException
            if (e instanceof MessageRemovedException) {
                // Message has been removed in the meantime
                if (null != folder) {
                    throw MailExceptionCode.MAIL_NOT_FOUND.create(e, "", folder.getFullName());
                }
                throw MailExceptionCode.MAIL_NOT_FOUND_SIMPLE.create(e);
            }
            if ((e instanceof javax.mail.AuthenticationFailedException) || ((toLowerCase(e.getMessage(), "").indexOf(ERR_AUTH_FAILED) >= 0))) {
                // Authentication failed
                return handleAuthenticationFailedException(e, mailConfig, session);
            } else if (e instanceof javax.mail.FolderClosedException) {
                if (isTimeoutException(e)) {
                    // javax.mail.FolderClosedException through a read timeout
                    return MimeMailExceptionCode.READ_TIMEOUT.create(e, mailConfig == null ? STR_EMPTY : mailConfig.getServer(), mailConfig == null ? STR_EMPTY : mailConfig.getLogin());
                }

                Exception nextException = e.getNextException();
                if (nextException instanceof com.sun.mail.iap.ConnectionException) {
                    return handleConnectionException((com.sun.mail.iap.ConnectionException) nextException, mailConfig, e);
                }

                final Folder f = ((javax.mail.FolderClosedException) e).getFolder();
                if (null != mailConfig && null != session) {
                    return MimeMailExceptionCode.FOLDER_CLOSED_EXT.create(
                        e,
                        null == f ? appendInfo(e.getMessage(), folder) : f.getFullName(),
                            mailConfig.getServer(),
                            mailConfig.getLogin(),
                            Integer.valueOf(session.getUserId()),
                            Integer.valueOf(session.getContextId()));
                }
                return MimeMailExceptionCode.FOLDER_CLOSED.create(e, null == f ? appendInfo(e.getMessage(), folder) : f.getFullName());
            } else if (e instanceof javax.mail.FolderNotFoundException) {
                final Folder f = ((javax.mail.FolderNotFoundException) e).getFolder();
                if (null != mailConfig && null != session) {
                    return MimeMailExceptionCode.FOLDER_NOT_FOUND_EXT.create(
                        e,
                        null == f ? appendInfo(e.getMessage(), folder) : f.getFullName(),
                            mailConfig.getServer(),
                            mailConfig.getLogin(),
                            Integer.valueOf(session.getUserId()),
                            Integer.valueOf(session.getContextId()));
                }
                return MimeMailExceptionCode.FOLDER_NOT_FOUND.create(e, null == f ? appendInfo(e.getMessage(), folder) : f.getFullName());
            } else if (e instanceof javax.mail.IllegalWriteException) {
                return MimeMailExceptionCode.ILLEGAL_WRITE.create(e, appendInfo(e.getMessage(), folder));
            } else if (e instanceof javax.mail.MessageRemovedException) {
                return MimeMailExceptionCode.MESSAGE_REMOVED.create(e, appendInfo(e.getMessage(), folder));
            } else if (e instanceof javax.mail.MethodNotSupportedException) {
                return MimeMailExceptionCode.METHOD_NOT_SUPPORTED.create(e, appendInfo(e.getMessage(), folder));
            } else if (e instanceof javax.mail.NoSuchProviderException) {
                return MimeMailExceptionCode.NO_SUCH_PROVIDER.create(e, appendInfo(e.getMessage(), folder));
            } else if (e instanceof javax.mail.internet.ParseException) {
                if (e instanceof javax.mail.internet.AddressException) {
                    final String optRef = ((AddressException) e).getRef();
                    return MimeMailExceptionCode.INVALID_EMAIL_ADDRESS.create(e, optRef == null ? STR_EMPTY : optRef);
                }
                return MimeMailExceptionCode.PARSE_ERROR.create(e, appendInfo(e.getMessage(), folder));
            } else if (e instanceof javax.mail.ReadOnlyFolderException) {
                if (null != mailConfig && null != session) {
                    return MimeMailExceptionCode.READ_ONLY_FOLDER_EXT.create(
                        e,
                        appendInfo(e.getMessage(), folder),
                        mailConfig.getServer(),
                        mailConfig.getLogin(),
                        Integer.valueOf(session.getUserId()),
                        Integer.valueOf(session.getContextId()));
                }
                return MimeMailExceptionCode.READ_ONLY_FOLDER.create(e, appendInfo(e.getMessage(), folder));
            } else if (e instanceof javax.mail.search.SearchException) {
                return MimeMailExceptionCode.SEARCH_ERROR.create(e, appendInfo(e.getMessage(), folder));
            } else if (e instanceof com.sun.mail.smtp.SMTPSendTimedoutException) {
                // Encountered timeout while trying to send a message to a recipient
                SMTPSendTimedoutException timedoutException = (SMTPSendTimedoutException) e;
                String cmd = timedoutException.getCommand();
                InternetAddress addr = timedoutException.getAddr();
                return MimeMailExceptionCode.SEND_TIMED_OUT_ERROR.create(e, addr.toUnicodeString(), cmd);
            } else if (e instanceof com.sun.mail.smtp.SMTPSenderFailedException) {
                SMTPSenderFailedException failedException = (SMTPSenderFailedException) e;
                SmtpInfo smtpInfo = getSmtpInfo(failedException);

                // Message rejected?
                if (smtpInfo.retCode == SMTP_ERROR_CODE_552) {
                    return MimeMailExceptionCode.MESSAGE_REJECTED_EXT.create(failedException, smtpInfo.toString());
                }
                if (toLowerCase(smtpInfo.message, "").indexOf(ERR_MSG_TOO_LARGE) > -1) {
                    return MimeMailExceptionCode.MESSAGE_TOO_LARGE_EXT.create(failedException, smtpInfo.toString());
                }
                return MimeMailExceptionCode.SEND_FAILED_MSG_EXT_ERROR.create(failedException, failedException.getMessage(), smtpInfo.toString());
            } else if (e instanceof com.sun.mail.smtp.SMTPAddressFailedException) {
                SMTPAddressFailedException failedException = (SMTPAddressFailedException) e;
                SmtpInfo smtpInfo = getSmtpInfo(failedException);

                // Message rejected?
                if (smtpInfo.retCode == SMTP_ERROR_CODE_552) {
                    return MimeMailExceptionCode.MESSAGE_REJECTED_EXT.create(failedException, smtpInfo.toString());
                }
                if (toLowerCase(smtpInfo.message, "").indexOf(ERR_MSG_TOO_LARGE) > -1) {
                    return MimeMailExceptionCode.MESSAGE_TOO_LARGE_EXT.create(failedException, smtpInfo.toString());
                }
                return MimeMailExceptionCode.SEND_FAILED_MSG_EXT_ERROR.create(failedException, failedException.getMessage(), smtpInfo.toString());
            } else if (e instanceof com.sun.mail.smtp.SMTPSendFailedException) {
                SMTPSendFailedException sendFailedError = (SMTPSendFailedException) e;
                SmtpInfo smtpInfo = getSmtpInfo(sendFailedError);

                // Message rejected?
                if (smtpInfo.retCode == SMTP_ERROR_CODE_552) {
                    return MimeMailExceptionCode.MESSAGE_REJECTED_EXT.create(sendFailedError, smtpInfo.toString());
                }
                if (toLowerCase(smtpInfo.message, "").indexOf(ERR_MSG_TOO_LARGE) > -1) {
                    return MimeMailExceptionCode.MESSAGE_TOO_LARGE_EXT.create(sendFailedError, smtpInfo.toString());
                }
                // 452 - 452 4.1.0 ... temporary failure
                if ((sendFailedError.getReturnCode() == 452) && (toLowerCase(sendFailedError.getMessage(), "").indexOf(ERR_TMP_FLR) > -1)) {
                    return MimeMailExceptionCode.TEMPORARY_FAILURE.create(sendFailedError, getSmtpInfo(sendFailedError));
                }
                Address[] addrs = sendFailedError.getInvalidAddresses();
                if (null == addrs || addrs.length == 0) {
                    // No invalid addresses available
                    addrs = sendFailedError.getValidUnsentAddresses();
                    if (null == addrs || addrs.length == 0) {
                        // Neither valid unsent addresses
                        return MimeMailExceptionCode.SEND_FAILED_MSG_ERROR.create(sendFailedError, smtpInfo.toString());
                    }
                }

                return MimeMailExceptionCode.SEND_FAILED_EXT.create(sendFailedError, Arrays.toString(addrs), smtpInfo.toString());
            } else if (e instanceof javax.mail.SendFailedException exc) {
                SmtpInfo smtpInfo = null;
                Address[] invalidAddresses = exc.getInvalidAddresses();
                {
                    final Exception nextException = exc.getNextException();
                    if (nextException instanceof com.sun.mail.smtp.SMTPSendFailedException failedError) {
                        smtpInfo = getSmtpInfo(failedError);
                        if (invalidAddresses == null || invalidAddresses.length == 0) {
                            invalidAddresses = failedError.getInvalidAddresses();
                            if (null == invalidAddresses || invalidAddresses.length == 0) {
                                invalidAddresses = failedError.getValidUnsentAddresses();
                            }
                        }
                    } else if (nextException instanceof com.sun.mail.smtp.SMTPSenderFailedException failedError) {
                        smtpInfo = getSmtpInfo(failedError);
                        if (invalidAddresses == null || invalidAddresses.length == 0) {
                            invalidAddresses = failedError.getInvalidAddresses();
                            if (null == invalidAddresses || invalidAddresses.length == 0) {
                                invalidAddresses = failedError.getValidUnsentAddresses();
                            }
                        }
                    } else if (nextException instanceof com.sun.mail.smtp.SMTPAddressFailedException failedError) {
                        smtpInfo = getSmtpInfo(failedError);
                        if (invalidAddresses == null || invalidAddresses.length == 0) {
                            invalidAddresses = failedError.getInvalidAddresses();
                            if (null == invalidAddresses || invalidAddresses.length == 0) {
                                invalidAddresses = failedError.getValidUnsentAddresses();
                            }
                        }
                    }
                }

                // Message rejected?
                if (null != smtpInfo) {
                    if (smtpInfo.retCode == SMTP_ERROR_CODE_552) {
                        return MimeMailExceptionCode.MESSAGE_REJECTED_EXT.create(exc, smtpInfo.toString());
                    }
                    if (toLowerCase(smtpInfo.message, "").indexOf(ERR_MSG_TOO_LARGE) > -1) {
                        return MimeMailExceptionCode.MESSAGE_TOO_LARGE_EXT.create(exc, smtpInfo.toString());
                    }
                }

                // Others...
                if (null == invalidAddresses || invalidAddresses.length == 0) {
                    return MimeMailExceptionCode.SEND_FAILED_MSG_ERROR.create(exc, null == smtpInfo ? exc.getMessage() : smtpInfo.toString());
                }
                return MimeMailExceptionCode.SEND_FAILED_EXT.create(exc, Arrays.toString(invalidAddresses), null == smtpInfo ? exc.getMessage() : smtpInfo.toString());
            } else if (e instanceof javax.mail.StoreClosedException) {
                if (isTimeoutException(e)) {
                    // javax.mail.FolderClosedException through a read timeout
                    return MimeMailExceptionCode.READ_TIMEOUT.create(e, mailConfig == null ? STR_EMPTY : mailConfig.getServer(), mailConfig == null ? STR_EMPTY : mailConfig.getLogin());
                }

                Exception nextException = e.getNextException();
                if (nextException instanceof com.sun.mail.iap.ConnectionException connectionException) {
                    return handleConnectionException(connectionException, mailConfig, e);
                }

                if (null != mailConfig && null != session) {
                    return MimeMailExceptionCode.STORE_CLOSED_EXT.create(e, mailConfig.getServer(), mailConfig.getLogin(), Integer.valueOf(session.getUserId()), Integer.valueOf(session.getContextId()), EMPTY_ARGS);
                }
                return MimeMailExceptionCode.STORE_CLOSED.create(e, EMPTY_ARGS);
            }  else if (e instanceof FolderCreationFailedException fcfe) {
                return MimeMailExceptionCode.FOLDER_CREATION_FAILED.create(e, fcfe.getFullName(), mailConfig == null ? STR_EMPTY : mailConfig.getServer(), mailConfig == null ? STR_EMPTY : mailConfig.getLogin());
            } else if (e instanceof javax.mail.search.UnsupportedSearchTermException) {
                if (null != mailConfig) {
                    return MimeMailExceptionCode.UNSUPPORTED_SEARCH_TERM.create(e, mailConfig.getServer(), mailConfig.getLogin());
                }
            } else if (e instanceof javax.mail.MaxResponseLengthExceededException) {
                if (null != mailConfig) {
                    return MimeMailExceptionCode.COMMUNICATION_PROBLEM.create(e, mailConfig.getServer(), mailConfig.getLogin(), appendInfo(e.getMessage(), folder));
                }
            }
            final Exception nextException = e.getNextException();
            if (nextException == null) {
                if (toLowerCase(e.getMessage(), "").indexOf(ERR_QUOTA) != -1) {
                    return MimeMailExceptionCode.QUOTA_EXCEEDED.create(e, getInfo(skipTag(e.getMessage())));
                } else if ("Unable to load BODYSTRUCTURE".equals(e.getMessage())) {
                    return MimeMailExceptionCode.MESSAGE_NOT_DISPLAYED.create(e, EMPTY_ARGS);
                }
                /*
                 * Default case
                 */
                final String message = Strings.toLowerCase(e.getMessage());
                if ("failed to load imap envelope".equals(message)) {
                    return MimeMailExceptionCode.MESSAGE_NOT_DISPLAYED.create(e);
                }
                if ("connection failure".equals(e.getMessage())) {
                    return MimeMailExceptionCode.NO_ROUTE_TO_HOST.create(e, mailConfig == null ? STR_EMPTY : mailConfig.getServer());
                }
                return MimeMailExceptionCode.MESSAGING_ERROR.create(e, appendInfo(e.getMessage(), folder));
            }
            /*
             * Messaging exception has a nested exception
             */
            if (nextException instanceof java.net.BindException) {
                return MimeMailExceptionCode.BIND_ERROR.create(e, mailConfig == null ? STR_EMPTY : Integer.valueOf(mailConfig.getPort()));
            } else if (nextException instanceof com.sun.mail.iap.ConnectionException connectionException) {
                return handleConnectionException(connectionException, mailConfig, e);
            } else if (nextException instanceof java.net.ConnectException) {
                /*
                 * Most modern IP stack implementations sense connection idleness, and abort the connection attempt, resulting in a
                 * java.net.ConnectionException
                 */
                mailInterfaceMonitor.changeNumTimeoutConnections(true);
                final OXException me = MimeMailExceptionCode.CONNECT_ERROR.create(
                    e,
                    mailConfig == null ? STR_EMPTY : mailConfig.getServer(),
                    mailConfig == null ? STR_EMPTY : mailConfig.getLogin(),
                    mailConfig == null ? STR_EMPTY : Integer.toString(mailConfig.getMailProperties().getConnectTimeout())
                );
                return me;
            } else if (nextException.getClass().getName().endsWith(EXC_CONNECTION_RESET_EXCEPTION)) {
                mailInterfaceMonitor.changeNumBrokenConnections(true);
                if (null != mailConfig && null != session) {
                    String server = mailConfig.getServer();
                    String login = mailConfig.getLogin();
                    return MimeMailExceptionCode.CONNECTION_RESET_EXT.create(e, server, login, I(session.getUserId()), I(session.getContextId()));
                }
                return MimeMailExceptionCode.CONNECTION_RESET.create(e, appendInfo(nextException.getMessage(), folder));
            } else if (nextException instanceof java.net.NoRouteToHostException) {
                return MimeMailExceptionCode.NO_ROUTE_TO_HOST.create(e, mailConfig == null ? STR_EMPTY : mailConfig.getServer());
            } else if (nextException instanceof java.net.PortUnreachableException) {
                return MimeMailExceptionCode.PORT_UNREACHABLE.create(
                    e,
                    mailConfig == null ? STR_EMPTY : Integer.valueOf(mailConfig.getPort()));
            } else if (nextException instanceof java.net.SocketException) {
                /*
                 * Treat dependent on message
                 */
                final SocketException se = (SocketException) nextException;
                if ("Socket closed".equals(se.getMessage()) || "Connection reset".equals(se.getMessage())) {
                    mailInterfaceMonitor.changeNumBrokenConnections(true);
                    return MimeMailExceptionCode.BROKEN_CONNECTION.create(e, mailConfig == null ? STR_EMPTY : mailConfig.getServer());
                }

                return handleIOException(se, mailConfig, session, folder);
            } else if (nextException instanceof java.net.UnknownHostException) {
                return MimeMailExceptionCode.UNKNOWN_HOST.create(e, appendInfo(null == mailConfig ? e.getMessage() : mailConfig.getServer(), folder));
            } else if (nextException instanceof java.net.SocketTimeoutException) {
                mailInterfaceMonitor.changeNumBrokenConnections(true);
                return MimeMailExceptionCode.READ_TIMEOUT.create(
                    e,
                    mailConfig == null ? STR_EMPTY : mailConfig.getServer(),
                        mailConfig == null ? STR_EMPTY : mailConfig.getLogin());
            } else if (nextException instanceof com.openexchange.mail.mime.QuotaExceededException) {
                if (null != mailConfig && null != session) {
                    return MimeMailExceptionCode.QUOTA_EXCEEDED_EXT.create(
                        nextException,
                        mailConfig.getServer(),
                        mailConfig.getLogin(),
                        Integer.valueOf(session.getUserId()),
                        Integer.valueOf(session.getContextId()),
                        appendInfo(getInfo(skipTag(nextException.getMessage())), folder),
                        getDisplayName(mailConfig));
                }
                return MimeMailExceptionCode.QUOTA_EXCEEDED.create(nextException, appendInfo(getInfo(skipTag(nextException.getMessage())), folder));
            } else if (nextException instanceof com.sun.mail.iap.CommandFailedException) {
                com.sun.mail.iap.CommandFailedException cfe = (com.sun.mail.iap.CommandFailedException) nextException;
                OXException handled = handleProtocolExceptionByResponseCode(cfe, mailConfig, session, folder);
                if (null != handled) {
                    return handled;
                }

                String msg = Strings.toLowerCase(nextException.getMessage());
                if (isOverQuotaException(msg)) {
                    // Over quota
                    if (null != mailConfig && null != session) {
                        return MimeMailExceptionCode.QUOTA_EXCEEDED_EXT.create(
                            nextException,
                            mailConfig.getServer(),
                            mailConfig.getLogin(),
                            Integer.valueOf(session.getUserId()),
                            Integer.valueOf(session.getContextId()),
                            appendInfo(getInfo(skipTag(nextException.getMessage())), folder),
                            getDisplayName(mailConfig));
                    }
                    return MimeMailExceptionCode.QUOTA_EXCEEDED.create(nextException, appendInfo(getInfo(skipTag(nextException.getMessage())), folder));
                }
                // Regular processing error cause by arbitrary CommandFailedException
                if (null != mailConfig && null != session) {
                    return MimeMailExceptionCode.PROCESSING_ERROR_WE_EXT.create(
                        nextException,
                        mailConfig.getServer(),
                        mailConfig.getLogin(),
                        Integer.valueOf(session.getUserId()),
                        Integer.valueOf(session.getContextId()),
                        appendInfo(getInfo(skipTag(nextException.getMessage())), folder));
                }
                return MimeMailExceptionCode.PROCESSING_ERROR_WE.create(nextException, appendInfo(getInfo(skipTag(nextException.getMessage())), folder));
            } else if (nextException instanceof com.sun.mail.iap.BadCommandException) {
                com.sun.mail.iap.BadCommandException bce = (com.sun.mail.iap.BadCommandException) nextException;
                OXException handled = handleProtocolExceptionByResponseCode(bce, mailConfig, session, folder);
                if (null != handled) {
                    return handled;
                }

                if (null != mailConfig && null != session) {
                    return MimeMailExceptionCode.PROCESSING_ERROR_EXT.create(
                        nextException,
                        mailConfig.getServer(),
                        mailConfig.getLogin(),
                        Integer.valueOf(session.getUserId()),
                        Integer.valueOf(session.getContextId()),
                        appendInfo(nextException.getMessage(), folder));
                }
                return MimeMailExceptionCode.PROCESSING_ERROR.create(nextException, appendInfo(nextException.getMessage(), folder));
            } else if (nextException instanceof com.sun.mail.iap.ProtocolException) {
                com.sun.mail.iap.ProtocolException pe = (com.sun.mail.iap.ProtocolException) nextException;
                OXException handled = handleProtocolExceptionByResponseCode(pe, mailConfig, session, folder);
                if (null != handled) {
                    return handled;
                }

                Throwable protocolError = pe.getCause();
                if (protocolError instanceof IOException) {
                    return handleIOException((IOException) protocolError, mailConfig, session, folder);
                }
                if (protocolError instanceof javax.mail.search.UnsupportedSearchTermException) {
                    if (null != mailConfig) {
                        return MimeMailExceptionCode.UNSUPPORTED_SEARCH_TERM.create(nextException, mailConfig.getServer(), mailConfig.getLogin());
                    }
                }

                if (null != mailConfig && null != session) {
                    return MimeMailExceptionCode.PROCESSING_ERROR_EXT.create(
                        nextException,
                        mailConfig.getServer(),
                        mailConfig.getLogin(),
                        Integer.valueOf(session.getUserId()),
                        Integer.valueOf(session.getContextId()),
                        appendInfo(nextException.getMessage(), folder));
                }
                return MimeMailExceptionCode.PROCESSING_ERROR.create(nextException, appendInfo(nextException.getMessage(), folder));
            } else if (nextException instanceof java.io.IOException) {
                return handleIOException((IOException) nextException, mailConfig, session, folder);
            } else if (toLowerCase(e.getMessage(), "").indexOf(ERR_QUOTA) != -1) {
                return MimeMailExceptionCode.QUOTA_EXCEEDED.create(e, getInfo(skipTag(e.getMessage())));
            }
            /*
             * Default case
             */
            return MimeMailExceptionCode.MESSAGING_ERROR.create(e, appendInfo(nextException.getMessage(), folder));
        } catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
            LOG.warn("", t);
            /*
             * This routine should not fail since it's purpose is wrap a corresponding mail error around specified messaging error
             */
            return MimeMailExceptionCode.MESSAGING_ERROR.create(e, appendInfo(e.getMessage(), folder));
        }
    }

    /**
     * Handles specified IMAP protocol exception by its possibly available <a href="https://tools.ietf.org/html/rfc5530">response code</a>.<br>
     * If no such response code is present, <code>null</code> is returned.
     *
     * @param pe The IMAP protocol exception
     * @param mailConfig The optional mail configuration associated with affected user
     * @param session The optional affected user's session
     * @param folder The optional folder
     * @return The {@link OXException} instance suitable for response code or <code>null</code>
     */
    public static OXException handleProtocolExceptionByResponseCode(com.sun.mail.iap.ProtocolException pe, MailConfig mailConfig, Session session, Folder folder) {
        com.sun.mail.iap.ResponseCode rc = pe.getKnownResponseCode();
        if (null == rc) {
            return null;
        }

        switch (rc) {
            case ALREADYEXISTS:
                return MailExceptionCode.DUPLICATE_FOLDER_SIMPLE.create(pe, new Object[0]);
            case EXPIRED:
                //$FALL-THROUGH$
            case AUTHORIZATIONFAILED:
                //$FALL-THROUGH$
            case AUTHENTICATIONFAILED:
                {
                    Response offendingResponse = pe.getResponse();
                    javax.mail.AuthenticationFailedException afe = new javax.mail.AuthenticationFailedException(null == offendingResponse ? pe.getMessage() : offendingResponse.getRest(), pe).setReason(rc.getName());
                    return handleAuthenticationFailedException(afe, mailConfig, session);
                }
            case CANNOT:
                {
                    String command = pe.getCommand();
                    if (Strings.isNotEmpty(command)) {
                        return MailExceptionCode.INVALID_OPERATION_EXT.create(pe, command, pe.getResponseRest());
                    }
                    return MailExceptionCode.INVALID_OPERATION.create(pe, pe.getResponseRest());

                }
            case CLIENTBUG:
                break;
            case CONTACTADMIN:
                break;
            case CORRUPTION:
                break;
            case EXPUNGEISSUED:
                break;
            case INUSE:
                {
                    // Too many sessions in use
                    if (null != mailConfig && null != session) {
                        return MimeMailExceptionCode.IN_USE_ERROR_EXT.create(
                            pe,
                            mailConfig.getServer(),
                            mailConfig.getLogin(),
                            Integer.valueOf(session.getUserId()),
                            Integer.valueOf(session.getContextId()),
                            appendInfo(getInfo(skipTag(pe.getMessage())), folder)).setCategory(CATEGORY_USER_INPUT);
                    }
                    return MimeMailExceptionCode.IN_USE_ERROR.create(pe, appendInfo(getInfo(skipTag(pe.getMessage())), folder)).setCategory(CATEGORY_USER_INPUT);
                }
            case LIMIT:
                {
                    if (null != mailConfig && null != session) {
                        return MimeMailExceptionCode.PROCESSING_ERROR_WE_EXT.create(
                            pe,
                            mailConfig.getServer(),
                            mailConfig.getLogin(),
                            Integer.valueOf(session.getUserId()),
                            Integer.valueOf(session.getContextId()),
                            appendInfo(getInfo(skipTag(pe.getMessage())), folder));
                    }
                    return MimeMailExceptionCode.PROCESSING_ERROR_WE.create(pe, appendInfo(getInfo(skipTag(pe.getMessage())), folder));
                }
            case NONEXISTENT:
                break;
            case NOPERM:
                return MailExceptionCode.INSUFFICIENT_PERMISSIONS.create(pe, new Object[0]);
            case OVERQUOTA:
                {
                    // Over quota
                    if (null != mailConfig && null != session) {
                        return MimeMailExceptionCode.QUOTA_EXCEEDED_EXT.create(
                            pe,
                            mailConfig.getServer(),
                            mailConfig.getLogin(),
                            Integer.valueOf(session.getUserId()),
                            Integer.valueOf(session.getContextId()),
                            appendInfo(getInfo(skipTag(pe.getMessage())), folder),
                            getDisplayName(mailConfig));
                    }
                    return MimeMailExceptionCode.QUOTA_EXCEEDED.create(pe, appendInfo(getInfo(skipTag(pe.getMessage())), folder));
                }
            case PRIVACYREQUIRED:
                return MailExceptionCode.NONSECURE_CONNECTION_DENIED.create(pe, new Object[0]);
            case SERVERBUG:
                break;
            case UNAVAILABLE:
                return MailExceptionCode.SUBSYSTEM_DOWN.create(pe, new Object[0]);
            case TRYCREATE:
                return MimeMailExceptionCode.TRYCREATE.create(pe, new Object[0]);
            default:
                break;
        }

        return null;
    }

    private static OXException handleConnectionException(com.sun.mail.iap.ConnectionException connectionException, MailConfig mailConfig, MessagingException e) {
        mailInterfaceMonitor.changeNumBrokenConnections(true);
        if (isTimeoutException(connectionException)) {
            // A read timeout
            return MimeMailExceptionCode.READ_TIMEOUT.create(e, mailConfig == null ? STR_EMPTY : mailConfig.getServer(), mailConfig == null ? STR_EMPTY : mailConfig.getLogin());
        }
        if (isByeException(connectionException)) {
            // Unexpected connection close
            return MimeMailExceptionCode.CONNECTION_CLOSED.create(e, mailConfig == null ? STR_EMPTY : mailConfig.getServer(), mailConfig == null ? STR_EMPTY : mailConfig.getLogin());
        }
        if (isAbortedException(connectionException)) {
            // Reading responses has been aborted
            return MimeMailExceptionCode.READ_ABORTED.create(e, mailConfig == null ? STR_EMPTY : mailConfig.getServer(), mailConfig == null ? STR_EMPTY : mailConfig.getLogin());
        }

        return MimeMailExceptionCode.CONNECT_ERROR.create(e, mailConfig == null ? STR_EMPTY : mailConfig.getServer(), mailConfig == null ? STR_EMPTY : mailConfig.getLogin(), mailConfig == null ? STR_EMPTY : Integer.toString(mailConfig.getMailProperties().getConnectTimeout()));
    }

    private static OXException handleAuthenticationFailedException(MessagingException authFailed, MailConfig mailConfig, Session session) {
        // Authentication failed...
        javax.mail.AuthenticationFailedException afe = (authFailed instanceof javax.mail.AuthenticationFailedException) ? ((javax.mail.AuthenticationFailedException) authFailed) : null;
        if (afe != null && ALERT.getName().equals(afe.getReason())) {
            Exception e = afe.getNextException();
            return MailExceptionCode.DENIED_CONNECT_ATTEMPT.create(e, mailConfig == null ? STR_EMPTY : mailConfig.getServer(), mailConfig == null ? STR_EMPTY : mailConfig.getLogin(), afe.getMessage());
        }

        // OAuth token expired?
        if (null != mailConfig && AuthType.isOAuthType(mailConfig.getAuthType()) && afe != null && (afe.hasNoReason() || AUTHENTICATIONFAILED.getName().equals(afe.getReason()))) {
            if (Account.DEFAULT_ID == mailConfig.getAccountId()) {
                // OAuth token expired for primary account
                return createInvalidCredentialsException(authFailed, mailConfig, session);
            }

            Account account = mailConfig.getAccount();
            if (account instanceof MailAccount) {
                MailAccount mailAccount = (MailAccount) account;
                if (mailAccount.isMailOAuthAble() && mailAccount.getMailOAuthId() >= 0) {
                    OAuthService oauthService = ServerServiceRegistry.getInstance().getService(OAuthService.class);
                    if (null != oauthService) {
                        OAuthAccount oAuthAccount;
                        try {
                            oAuthAccount = oauthService.getAccount(session, mailAccount.getMailOAuthId());
                        } catch (Exception x) {
                            LOG.warn("Failed to load mail-associated OAuth account", x);
                            oAuthAccount = null;
                        }
                        if (null != oAuthAccount) {
                            API api = oAuthAccount.getAPI();
                            return OAuthExceptionCodes.OAUTH_ACCESS_TOKEN_INVALID.create(authFailed, api.getDisplayName(), I(oAuthAccount.getId()), I(session.getUserId()), I(session.getContextId()));
                        }
                    }
                }
            }
        }

        if (authFailed instanceof javax.mail.AuthorizationFailedException) {
            return MimeMailExceptionCode.AUTHORIZATION_FAILED.create(authFailed, mailConfig == null ? STR_EMPTY : mailConfig.getServer(), mailConfig == null ? STR_EMPTY : mailConfig.getLogin());
        } else if (authFailed instanceof javax.mail.TemporaryAuthenticationFailureException) {
            return MimeMailExceptionCode.TEMPORARY_AUTH_FAILURE.create(authFailed, mailConfig == null ? STR_EMPTY : mailConfig.getServer(), mailConfig == null ? STR_EMPTY : mailConfig.getLogin());
        } else if (authFailed instanceof javax.mail.PasswordExpiredException) {
            return MimeMailExceptionCode.PASSWORD_EXPIRED.create(authFailed, mailConfig == null ? STR_EMPTY : mailConfig.getServer(), mailConfig == null ? STR_EMPTY : mailConfig.getLogin());
        } else if (authFailed instanceof javax.mail.PrivacyRequiredException) {
            return MimeMailExceptionCode.PRIVACY_REQUIRED.create(authFailed, mailConfig == null ? STR_EMPTY : mailConfig.getServer(), mailConfig == null ? STR_EMPTY : mailConfig.getLogin());
        }

        // Primary account?
        if (null != mailConfig && Account.DEFAULT_ID == mailConfig.getAccountId()) {
            return MimeMailExceptionCode.LOGIN_FAILED.create(authFailed, mailConfig.getServer(), mailConfig.getLogin());
        }

        // Temporary nature?
        if ((authFailed.getMessage() != null) && ERR_TMP.equals(Strings.toLowerCase(authFailed.getMessage()))) {
            String server = mailConfig == null ? STR_EMPTY : mailConfig.getServer();
            String login = mailConfig == null ? STR_EMPTY : mailConfig.getLogin();
            return MimeMailExceptionCode.LOGIN_FAILED.create(authFailed, server, login);
        }

        // Advertise invalid credential w/ or w/o additional info
        return createInvalidCredentialsException(authFailed, mailConfig, session);
    }

    private static OXException createInvalidCredentialsException(Exception authenticationFailedException, MailConfig mailConfig, Session session) {
        if (null != mailConfig && null != session) {
            String server = mailConfig.getServer();
            String login = mailConfig.getLogin();
            return MimeMailExceptionCode.INVALID_CREDENTIALS_EXT.create(authenticationFailedException, server, login, I(session.getUserId()), I(session.getContextId()), authenticationFailedException.getMessage());
        }

        String server = mailConfig == null ? STR_EMPTY : mailConfig.getServer();
        return MimeMailExceptionCode.INVALID_CREDENTIALS.create(authenticationFailedException, server, authenticationFailedException.getMessage());
    }

    private static OXException handleIOException(IOException ioException, MailConfig mailConfig, Session session, Folder folder) {
        if (IOs.isConnectionReset(ioException)) {
            if (null != mailConfig && null != session) {
                String server = mailConfig.getServer();
                String login = mailConfig.getLogin();
                return MimeMailExceptionCode.CONNECTION_RESET_EXT.create(ioException, server, login, I(session.getUserId()), I(session.getContextId()));
            }
            return MimeMailExceptionCode.CONNECTION_RESET.create(ioException, appendInfo(ioException.getMessage(), folder));
        }

        if (IOs.isSSLHandshakeException(ioException)) {
            if (null != mailConfig && null != session) {
                String server = mailConfig.getServer();
                String login = mailConfig.getLogin();
                return MimeMailExceptionCode.SSL_ERROR_EXT.create(ioException, appendInfo(ioException.getMessage(), folder), server, login, I(session.getUserId()), I(session.getContextId()));
            }
            return MimeMailExceptionCode.SSL_ERROR.create(ioException, appendInfo(ioException.getMessage(), folder));
        }

        if (null != mailConfig && null != session) {
            String server = mailConfig.getServer();
            String login = mailConfig.getLogin();
            return MimeMailExceptionCode.IO_ERROR_EXT.create(ioException, appendInfo(ioException.getMessage(), folder), server, login, I(session.getUserId()), I(session.getContextId()));
        }
        return MimeMailExceptionCode.IO_ERROR.create(ioException, appendInfo(ioException.getMessage(), folder));
    }

    /**
     * Appends command information to given information string.
     *
     * @param info The information
     * @param folder The optional folder
     * @return The command with optional information appended
     */
    public static String appendInfo(String info, Folder folder) {
        if (null == folder) {
            return info;
        }
        final StringBuilder sb = null == info ? new StringBuilder(64) : new StringBuilder(info);
        sb.append(" (folder=\"").append(folder.getFullName()).append('"');
        final Store store = folder.getStore();
        if (null != store) {
            sb.append(", store=\"").append(store.toString()).append('"');
        }
        sb.append(')');
        return sb.toString();
    }

    private static String getInfo(String info) {
        if (null == info) {
            return info;
        }
        final int pos = Strings.toLowerCase(info).indexOf("error message: ");
        return pos < 0 ? info : info.substring(pos + 15);
    }

    private static final Pattern PATTERN_TAG = Pattern.compile("A[0-9]+ (.+)");

    private static String skipTag(String serverResponse) {
        if (null == serverResponse) {
            return null;
        }
        final Matcher m = PATTERN_TAG.matcher(serverResponse);
        if (m.matches()) {
            return m.group(1);
        }
        return serverResponse;
    }

    private static <E> E lookupNested(MessagingException e, Class<E> clazz) {
        if (null == e) {
            return null;
        }

        Exception exception = e.getNextException();
        if (clazz.isInstance(exception)) {
            return clazz.cast(exception);
        }
        return exception instanceof MessagingException ? lookupNested((MessagingException) exception, clazz) : null;
    }

    /**
     * Checks for possible exists error.
     */
    public static boolean isExistsException(MessagingException e) {
        if (null == e) {
            return false;
        }
        return isExistsException(e.getMessage());
    }

    /**
     * Checks for possible exists error.
     */
    public static boolean isExistsException(String msg) {
        if (null == msg) {
            return false;
        }
        final String m = Strings.toLowerCase(msg);
        return (m.indexOf("exists") >= 0);
    }

    /**
     * Checks for possible already-exists error.
     */
    public static boolean isAlreadyExistsException(MessagingException e) {
        if (null == e) {
            return false;
        }

        com.sun.mail.iap.ProtocolException pe = lookupNested(e, com.sun.mail.iap.ProtocolException.class);
        if (null != pe) {
            if (ResponseCode.ALREADYEXISTS == pe.getKnownResponseCode()) {
                return true;
            }
        }

        return isAlreadyExistsException(e.getMessage());
    }

    /**
     * Checks for possible already-exists error.
     */
    public static boolean isAlreadyExistsException(String msg) {
        if (null == msg) {
            return false;
        }
        final String m = Strings.toLowerCase(msg);
        return (m.indexOf("alreadyexists") >= 0);
    }

    /**
     * Checks for possible over-quota error.
     */
    public static boolean isOverQuotaException(MessagingException e) {
        if (null == e) {
            return false;
        }

        com.sun.mail.iap.ProtocolException pe = lookupNested(e, com.sun.mail.iap.ProtocolException.class);
        if (null != pe) {
            if (ResponseCode.OVERQUOTA == pe.getKnownResponseCode()) {
                return true;
            }
        }

        return isOverQuotaException(e.getMessage());
    }

    /**
     * Checks for possible over-quota error.
     */
    public static boolean isOverQuotaException(String msg) {
        if (null == msg) {
            return false;
        }
        String m = Strings.asciiLowerCase(msg);
        return (m.indexOf("quota") >= 0 || (m.indexOf("limit") >= 0 && m.indexOf("[limit]") < 0));
    }

    /**
     * Checks for possible in-use error.
     */
    public static boolean isInUseException(MessagingException e) {
        if (null == e) {
            return false;
        }

        com.sun.mail.iap.ProtocolException pe = lookupNested(e, com.sun.mail.iap.ProtocolException.class);
        if (null != pe) {
            if (ResponseCode.INUSE == pe.getKnownResponseCode()) {
                return true;
            }
        }

        return isInUseException(Strings.asciiLowerCase(e.getMessage()));
    }

    /**
     * Checks for possible in-use error.
     */
    public static boolean isInUseException(String msg) {
        if (null == msg) {
            return false;
        }
        return (Strings.toLowerCase(msg).indexOf("[inuse]") >= 0);
    }

    /**
     * Checks for possible command-failed error.
     */
    public static boolean isCommandFailedException(MessagingException e) {
        if (null == e) {
            return false;
        }
        CommandFailedException commandFailedError = lookupNested(e, com.sun.mail.iap.CommandFailedException.class);
        return null != commandFailedError;
    }

    /**
     * Checks if cause of specified exception indicates a communication problem; such as read timeout, EOF, etc.
     *
     * @param e The exception to examine
     * @return <code>true</code> if a communication problem is indicated; otherwise <code>false</code>
     */
    public static boolean isCommunicationException(OXException e) {
        if (null == e) {
            return false;
        }

        Throwable next = e.getCause();
        if (next instanceof OXException) {
            return isCommunicationException((OXException) next);
        }
        if (next instanceof MessagingException) {
            return isCommunicationException((MessagingException) next);
        }
        return isEitherOf(next == null ? e : next, com.sun.mail.iap.ByeIOException.class, java.net.SocketTimeoutException.class, java.io.EOFException.class);
    }

    /**
     * Checks if cause of specified messaging exception indicates a communication problem; such as read timeout, EOF, etc.
     *
     * @param e The messaging exception to examine
     * @return <code>true</code> if a communication problem is indicated; otherwise <code>false</code>
     */
    public static boolean isCommunicationException(MessagingException e) {
        if (null == e) {
            return false;
        }

        javax.mail.FolderClosedException folderClosedError = lookupNested(e, javax.mail.FolderClosedException.class);
        if (null != folderClosedError) {
            return true;
        }

        javax.mail.StoreClosedException storeClosedError = lookupNested(e, javax.mail.StoreClosedException.class);
        if (null != storeClosedError) {
            return true;
        }

        return isEitherOf(e, com.sun.mail.iap.ByeIOException.class, java.net.SocketTimeoutException.class, java.io.EOFException.class);
    }

    /**
     * Checks if cause of specified messaging exception indicates a connect problem.
     *
     * @param e The messaging exception to examine
     * @return <code>true</code> if a connect problem is indicated; otherwise <code>false</code>
     */
    public static boolean isConnectException(MessagingException e) {
        if (null == e) {
            return false;
        }

        return isEitherOf(e, java.net.ConnectException.class, com.sun.mail.iap.ConnectionException.class);
    }

    /**
     * Checks if cause of specified messaging exception indicates a timeout or connect problem.
     *
     * @param e The messaging exception to examine
     * @return <code>true</code> if a timeout or connect problem is indicated; otherwise <code>false</code>
     */
    public static boolean isTimeoutOrConnectException(MessagingException e) {
        if (null == e) {
            return false;
        }

        return isEitherOf(e, java.net.SocketTimeoutException.class, java.net.ConnectException.class, com.sun.mail.iap.ConnectionException.class);
    }

    /**
     * Checks if cause of specified messaging exception indicates a timeout problem.
     *
     * @param e The messaging exception to examine
     * @return <code>true</code> if a timeout problem is indicated; otherwise <code>false</code>
     */
    public static boolean isTimeoutException(MessagingException e) {
        if (null == e) {
            return false;
        }

        return isEitherOf(e, java.net.SocketTimeoutException.class);
    }

    /**
     * Checks if cause of specified connection exception indicates a timeout problem.
     *
     * @param e The connection exception to examine
     * @return <code>true</code> if a timeout problem is indicated; otherwise <code>false</code>
     */
    private static boolean isTimeoutException(com.sun.mail.iap.ConnectionException e) {
        if (null == e) {
            return false;
        }

        return isEitherOf(e, java.net.SocketTimeoutException.class);
    }

    /**
     * Checks if cause of specified connection exception indicates unexpected connection closure.
     *
     * @param e The connection exception to examine
     * @return <code>true</code> if unexpected connection closure is indicated; otherwise <code>false</code>
     */
    private static boolean isByeException(com.sun.mail.iap.ConnectionException e) {
        if (null == e) {
            return false;
        }

        Response response = e.getResponse();
        if (response != null && response.isBYE()) {
            return true;
        }

        return isEitherOf(e, com.sun.mail.iap.ByeIOException.class);
    }

    /**
     * Checks if cause of specified connection exception indicates that reading has been aborted.
     *
     * @param e The connection exception to examine
     * @return <code>true</code> if reading has been aborted; otherwise <code>false</code>
     */
    private static boolean isAbortedException(com.sun.mail.iap.ConnectionException e) {
        if (null == e) {
            return false;
        }

        return isEitherOf(e, java.io.InterruptedIOException.class);
    }

    /**
     * Checks if cause of specified exception indicates an SSL hand-shake problem.
     *
     * @param e The exception to examine
     * @return <code>true</code> if an SSL hand-shake problem is indicated; otherwise <code>false</code>
     */
    public static boolean isSSLHandshakeException(MessagingException e) {
        return IOs.isSSLHandshakeException(e);
    }

    /**
     * Checks if cause of specified exception indicates a failed authentication problem.
     *
     * @param e The exception to examine
     * @return <code>true</code> if a failed authentication problem is indicated; otherwise <code>false</code>
     */
    public static boolean isAuthenticationFailedException(OXException e) {
        if (null == e) {
            return false;
        }

        Throwable next = e.getCause();
        if (next instanceof OXException) {
            return isAuthenticationFailedException((OXException) next);
        }
        if (next instanceof MessagingException) {
            return isAuthenticationFailedException((MessagingException) next);
        }
        return isEitherOf(next == null ? e : next, javax.mail.AuthenticationFailedException.class);
    }

    /**
     * Checks if specified messaging exception or its cause chain indicates a failed authentication problem.
     *
     * @param e The messaging exception to examine
     * @return <code>true</code> if a failed authentication problem is indicated; otherwise <code>false</code>
     */
    private static boolean isAuthenticationFailedException(MessagingException e) {
        if (null == e) {
            return false;
        }

        if (e instanceof javax.mail.AuthenticationFailedException) {
            return true;
        }

        javax.mail.AuthenticationFailedException authFailedError = lookupNested(e, javax.mail.AuthenticationFailedException.class);
        if (null != authFailedError) {
            return true;
        }

        return isEitherOf(e, javax.mail.AuthenticationFailedException.class);
    }

    // ------------------------------------------------- SMTP error stuff ----------------------------------------------------------------

    private static final class SmtpInfo {

        final int retCode;
        final String message;

        SmtpInfo(int retCode, String message) {
            super();
            this.retCode = retCode;
            this.message = message;
        }

        @Override
        public String toString() {
            return new StringBuilder(64).append(retCode).append(" - ").append(message).toString();
        }
    }

    private static SmtpInfo getSmtpInfo(SMTPSendFailedException sendFailedError) {
        if (null == sendFailedError) {
            return null;
        }

        int retCode = sendFailedError.getReturnCode();
        if ((retCode >= 400 && retCode <= 499) || (retCode >= 500 && retCode <= 599)) {
            // An SMTP error
            return new SmtpInfo(sendFailedError.getReturnCode(), sendFailedError.getMessage());
        }

        // Check if nested exception reveals the actual SMTP error
        SmtpInfo smtpInfo = optSmtpInfo(sendFailedError.getNextException());
        if (null != smtpInfo) {
            return smtpInfo;
        }

        // Return specified exception's SMTP info as last resort
        return new SmtpInfo(sendFailedError.getReturnCode(), sendFailedError.getMessage());
    }

    private static SmtpInfo getSmtpInfo(SMTPAddressFailedException sendFailedError) {
        if (null == sendFailedError) {
            return null;
        }

        int retCode = sendFailedError.getReturnCode();
        if ((retCode >= 400 && retCode <= 499) || (retCode >= 500 && retCode <= 599)) {
            // An SMTP error
            return new SmtpInfo(sendFailedError.getReturnCode(), sendFailedError.getMessage());
        }

        // Check if nested exception reveals the actual SMTP error
        SmtpInfo smtpInfo = optSmtpInfo(sendFailedError.getNextException());
        if (null != smtpInfo) {
            return smtpInfo;
        }

        // Return specified exception's SMTP info as last resort
        return new SmtpInfo(sendFailedError.getReturnCode(), sendFailedError.getMessage());
    }

    private static SmtpInfo getSmtpInfo(SMTPSenderFailedException sendFailedError) {
        if (null == sendFailedError) {
            return null;
        }

        int retCode = sendFailedError.getReturnCode();
        if ((retCode >= 400 && retCode <= 499) || (retCode >= 500 && retCode <= 599)) {
            // An SMTP error
            return new SmtpInfo(sendFailedError.getReturnCode(), sendFailedError.getMessage());
        }

        // Check if nested exception reveals the actual SMTP error
        SmtpInfo smtpInfo = optSmtpInfo(sendFailedError.getNextException());
        if (null != smtpInfo) {
            return smtpInfo;
        }

        // Return specified exception's SMTP info as last resort
        return new SmtpInfo(sendFailedError.getReturnCode(), sendFailedError.getMessage());
    }

    private static SmtpInfo optSmtpInfo(Exception possibleSmtpException) {
        if (null == possibleSmtpException) {
            return null;
        }

        if (possibleSmtpException instanceof SMTPSendFailedException) {
            return getSmtpInfo((SMTPSendFailedException) possibleSmtpException);
        }
        if (possibleSmtpException instanceof SMTPAddressFailedException) {
            return getSmtpInfo((SMTPAddressFailedException) possibleSmtpException);
        }
        if (possibleSmtpException instanceof SMTPSenderFailedException) {
            return getSmtpInfo((SMTPSenderFailedException) possibleSmtpException);
        }

        return null;
    }

    /** ASCII-wise to lower-case */
    private static String toLowerCase(CharSequence chars, String defaultValue) {
        if (null == chars) {
            return defaultValue;
        }
        final int length = chars.length();
        final StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            final char c = chars.charAt(i);
            builder.append((c >= 'A') && (c <= 'Z') ? (char) (c ^ 0x20) : c);
        }
        return builder.toString();
    }

    /**
     * Retrieves the display name from the specified {@link MailConfig}.
     * If no {@link Account} is attached to the {@link MailConfig} or
     * no display name is present, then the login name is returned.
     *
     * @param mailConfig The {@link MailConfig}
     * @return The display name
     */
    private static String getDisplayName(MailConfig mailConfig) {
        if (null == mailConfig.getAccount()) {
            return mailConfig.getLogin();
        }
        Account account = mailConfig.getAccount();
        if (Strings.isEmpty(account.getName())) {
            return mailConfig.getLogin();
        }
        return mailConfig.getAccount().getName();
    }
}
