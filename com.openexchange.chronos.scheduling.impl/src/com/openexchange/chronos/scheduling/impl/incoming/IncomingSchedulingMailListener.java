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

package com.openexchange.chronos.scheduling.impl.incoming;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.b;
import static com.openexchange.mailaccount.Account.DEFAULT_ID;
import static com.openexchange.tools.arrays.Arrays.isNullOrEmpty;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import java.net.IDN;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.mail.internet.InternetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.annotation.NonNull;
import com.openexchange.chronos.CalendarObjectResource;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.scheduling.AutoProcessIMip;
import com.openexchange.chronos.scheduling.ITipProcessorService;
import com.openexchange.chronos.scheduling.IncomingSchedulingMessage;
import com.openexchange.chronos.scheduling.MessageStatus;
import com.openexchange.chronos.scheduling.ProcessResult;
import com.openexchange.chronos.scheduling.PushedIMipResolveMode;
import com.openexchange.chronos.scheduling.common.ChronosITipData;
import com.openexchange.chronos.scheduling.common.MailPushListener;
import com.openexchange.chronos.scheduling.common.SyntheticPushSession;
import com.openexchange.chronos.scheduling.impl.ITipMailFlag;
import com.openexchange.chronos.scheduling.impl.MessageStatusServiceImpl;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.CalendarService;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.config.lean.DefaultProperty;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.config.lean.Property;
import com.openexchange.context.ContextService;
import com.openexchange.database.DatabaseService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.LdapExceptionCode;
import com.openexchange.java.Strings;
import com.openexchange.mail.MailAttributation;
import com.openexchange.mail.MailFetchArguments;
import com.openexchange.mail.MailFetchListener;
import com.openexchange.mail.MailFetchListenerResult;
import com.openexchange.mail.MailField;
import com.openexchange.mail.MailFields;
import com.openexchange.mail.api.IMailFolderStorage;
import com.openexchange.mail.api.IMailFolderStorageInfoSupport;
import com.openexchange.mail.api.IMailMessageStorage;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.dataobjects.MailFolder;
import com.openexchange.mail.dataobjects.MailFolderInfo;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.login.resolver.MailLoginResolverService;
import com.openexchange.mail.login.resolver.ResolverResult;
import com.openexchange.mailmapping.MailResolverService;
import com.openexchange.mailmapping.ResolvedMail;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.tools.strings.TimeSpanParser;
import com.openexchange.user.User;
import com.openexchange.user.UserExceptionCode;
import com.openexchange.user.UserService;

/**
 *
 * {@link IncomingSchedulingMailListener}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public class IncomingSchedulingMailListener implements MailFetchListener, MailPushListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(IncomingSchedulingMailListener.class);

    /** The general server property if auto processing of iMIP messages within this listener is enabled */
    private static final Property ON_MAIL_FETCH_PROPERTY = DefaultProperty.valueOf("com.openexchange.calendar.autoProcessIMipOnMailFetch", Boolean.TRUE);

    /** The property allowing to configure the maximum age of automatically processed messages */
    private static final Property PROPERTY_AUTOSCHEDULE_TIMEFRAME = DefaultProperty.valueOf("com.openexchange.calendar.autoProcessIMipTimeframe", "4W");

    /** The maximum number of scheduling messages to process while mails are being fetched */
    private static final int MAX_MESSAGES = 100;

    private final ServiceLookup services;

    private final IncomingSchedulingMailFactoryImpl factory;

    private final MessageStatusServiceImpl messageStatusService;

    /**
     * Initializes a new {@link IncomingSchedulingMailListener}.
     *
     * @param services The service lookup
     * @param factory The factory
     * @param messageStatusService The message status service
     */
    public IncomingSchedulingMailListener(ServiceLookup services, IncomingSchedulingMailFactoryImpl factory, MessageStatusServiceImpl messageStatusService) {
        super();
        this.services = services;
        this.factory = factory;
        this.messageStatusService = messageStatusService;
    }

    @Override
    public boolean accept(MailMessage[] mailsFromCache, MailFetchArguments fetchArguments, Session session) throws OXException {
        /*
         * Do nothing here as we need the mail access to check the BODYSTRUCUTRE capability of the mail. W/O we need to load mails from the storage ...
         */
        return true;
    }

    @Override
    public MailAttributation onBeforeFetch(MailFetchArguments fetchArguments, MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess, Map<String, Object> state) throws OXException {
        /*
         * check if applicable
         */
        if (DEFAULT_ID != mailAccess.getAccountId() || onlyIds(fetchArguments)) {
            return MailAttributation.NOT_APPLICABLE;
        }
        CalendarSession calendarSession = initCalendarSession(services, mailAccess.getSession());
        if (null == calendarSession || isAutoProcessingDisabled(calendarSession)) {
            return MailAttributation.NOT_APPLICABLE;
        }
        /*
         * remember session in state & indicate needed mail fields
         */
        state.put(CalendarSession.class.getName(), calendarSession);
        return MailAttributation.builder(fetchArguments.getFields(), fetchArguments.getHeaderNames()) //@formatter:off
            .addField(MailField.CONTENT_TYPE)
            .addField(MailField.FROM)
            .addField(MailField.ATTACHMENT_NAME)
            .addField(MailField.FLAGS)
        .build(); //@formatter:on
    }

    private static final MailFields FIELDS_ID = new MailFields(MailField.ID, MailField.FOLDER_ID, MailField.ORIGINAL_ID, MailField.ORIGINAL_FOLDER_ID);

    /**
     * Checks if client initially requested only identifier fields; such as mail identifier, folder identifier, etc.
     *
     * @param fetchArguments The arguments to examine
     * @return <code>true</code> if only identifier fields are requested; otherwise <code>false</code>
     */
    private static boolean onlyIds(MailFetchArguments fetchArguments) {
        if (fetchArguments.getHeaderNames() != null && fetchArguments.getHeaderNames().length > 0) {
            // Contains headers
            return false;
        }

        return !(new MailFields(fetchArguments.getInitialFields()).retainAll(FIELDS_ID));
    }

    @Override
    public MailFetchListenerResult onAfterFetch(MailMessage[] mails, boolean cacheable, MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess, Map<String, Object> state) throws OXException {
        MailFetchListenerResult result = MailFetchListenerResult.neutral(mails, cacheable);
        if (DEFAULT_ID == mailAccess.getAccountId() && null != mails && 0 < mails.length) {
            try {
                CalendarSession calendarSession = (CalendarSession) state.remove(CalendarSession.class.getName());
                process(mailAccess, calendarSession, Arrays.asList(mails));
            } catch (Exception e) {
                LOGGER.warn("Unexpected error handling mail fetch result: {}", e.getMessage(), e);
            }
        }
        return result;
    }

    @Override
    public MailMessage onSingleMailFetch(MailMessage mail, MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess) throws OXException {
        if (DEFAULT_ID == mailAccess.getAccountId() && null != mail) {
            try {
                CalendarSession calendarSession = initCalendarSession(services, mailAccess.getSession());
                if (null != calendarSession && !isAutoProcessingDisabled(calendarSession)) {
                    process(mailAccess, calendarSession, Collections.singletonList(mail));
                }
            } catch (Exception e) {
                LOGGER.warn("Unexpected error handling mail fetch result: {}", e.getMessage(), e);
            }
        }
        return mail;
    }

    @Override
    public void pushMail(MailPushListener.PushMail pushMail) throws OXException {
        LOGGER.trace("Handling pushed mail event \"{}\" for user \"{}\"", pushMail.getEvent(), pushMail.getUser());
        MailMessage mail = pushMail.getMail();
        if (null != ITipMailFlag.fromUserFlags(mail.getUserFlags()) || isNullOrEmpty(mail.getTo())) {
            // Already processed or we can't check target user
            LOGGER.trace("Skipping pushed message for user \"{}\": {}", pushMail.getUser(), isNullOrEmpty(mail.getTo()) ? "Missing 'To'" : "Already processed");
            return;
        }
        /*
         * Resolve user(s) and process message if applicable
         */
        List<ResolvedMail> resolvedMails = resolveUser(mail, pushMail.getUser());
        if (resolvedMails.isEmpty()) {
            LOGGER.trace("Skipping pushed message for user \"{}\": Unable to resolve user from \"{}\"", pushMail.getUser(), mail.getTo());
            return;
        }
        ChronosITipData iTipData = optITipData(mail);
        for (ResolvedMail resolvedMail : resolvedMails) {
            if (null != iTipData && iTipData.matches(services.getServiceSafe(DatabaseService.class).getServerUid(), resolvedMail.getContextID())) {
                LOGGER.trace("Skipping pushed message for user \"{}\" (\"{}\"), targeted at resolved user {} in context {}: Internal scheduling mail for same context detected.",
                    pushMail.getUser(), mail.getTo(), I(resolvedMail.getUserID()), I(resolvedMail.getContextID()));
                continue;
            }
            User user = services.getServiceSafe(UserService.class).getUser(resolvedMail.getUserID(), resolvedMail.getContextID());
            String userMail = user.getMail();
            String[] aliases = user.getAliases();
            SyntheticPushSession syntheticSession = new SyntheticPushSession(resolvedMail.getUserID(), resolvedMail.getContextID());
            IncomingSchedulingMessage message = factory.createPatched(mail, syntheticSession);
            if (contains(mail.getTo(), userMail) && isContainedIn(message.getResource(), userMail)) {
                LOGGER.trace("Processing incoming {} message with UID \"{}\" for \"{}\", targeted at resolved user {} in context {}", //
                    message.getMethod(), message.getResource().getUid(), userMail, I(resolvedMail.getUserID()), I(resolvedMail.getContextID()));
                process(message, syntheticSession);
            } else {
                for (String alias : aliases) {
                    if (contains(mail.getTo(), alias) && isContainedIn(message.getResource(), alias)) {
                        LOGGER.trace("Processing incoming {} message with UID \"{}\" for \"{}\", targeted at resolved user {} in context {}", //
                            message.getMethod(), message.getResource().getUid(), alias, I(resolvedMail.getUserID()), I(resolvedMail.getContextID()));
                        process(message, syntheticSession);
                    }
                }
            }
        }
    }

    /*
     * ============================== HELPERS ==============================
     */

    private List<ResolvedMail> resolveUser(MailMessage mail, String user) throws OXException {
        LeanConfigurationService configService = services.getService(LeanConfigurationService.class);
        if (configService == null) {
            LOGGER.debug("Missing service {}.", LeanConfigurationService.class.getSimpleName());
            return emptyList();
        }

        PushedIMipResolveMode mode = PushedIMipResolveMode.getConfiguredValue(configService);
        switch (mode) {
            case SYNTHETIC:
                return synthetic(user);
            case LOGININFO:
                return loginInfo(user);
            case MAILLOGIN:
                return mailLogin(user);
            case RECIPIENTONLY:
            default:
                return recipientOnly(mail);
        }
    }

    private List<ResolvedMail> mailLogin(String user) throws OXException {
        MailLoginResolverService mailLoginResolver = services.getServiceSafe(MailLoginResolverService.class);
        ResolverResult result = mailLoginResolver.resolveMailLogin(-1, user);
        return singletonList(new ResolvedMail(result.getUserId(), result.getContextId()));
    }

    private List<ResolvedMail> recipientOnly(MailMessage mail) throws OXException {
        MailResolverService mailResolver = services.getServiceSafe(MailResolverService.class);
        if (isNullOrEmpty(mail.getTo())) {
            return emptyList();
        }
        int size = mail.getTo().length;
        ArrayList<String> recipient = new ArrayList<>(size);
        for (InternetAddress internetAddress : mail.getTo()) {
            recipient.add(IDN.toUnicode(internetAddress.getAddress()));
        }
        ArrayList<ResolvedMail> result = new ArrayList<>(size);
        for (ResolvedMail resolvedMail : mailResolver.resolveMultiple(recipient.toArray(new String[size]))) {
            if (null != resolvedMail && resolvedMail.getUserID() > 0 && resolvedMail.getContextID() > 0) {
                result.add(resolvedMail);
            }
        }
        return result;
    }

    private static List<ResolvedMail> synthetic(String user) {
        if (Strings.isEmpty(user) || user.indexOf('@') < 0) {
            LOGGER.debug("Invalid user format for systhetic mode.");
            return emptyList();
        }
        String[] split = Strings.splitBy(user, '@', false);
        if (split.length != 2) {
            LOGGER.debug("Invalid user format for systhetic mode.");
            return emptyList();
        }
        try {
            return singletonList(new ResolvedMail(Integer.parseInt(split[0].trim()), Integer.parseInt(split[1].trim())));
        } catch (NumberFormatException nfe) {
            LOGGER.debug("Invalid user format for systhetic mode. {}", nfe.getMessage(), nfe);
            return emptyList();
        }
    }

    private List<ResolvedMail> loginInfo(String user) throws OXException {
        ContextService contextService = services.getServiceSafe(ContextService.class);
        UserService userService = services.getServiceSafe(UserService.class);
        if (Strings.isEmpty(user)) {
            return emptyList();
        }
        String[] logininfo = split(user);
        int ctxId = contextService.getContextId(logininfo[1]);
        if (ctxId == -1) {
            LOGGER.debug("Missing context mapping for context \"{}\". Login failed.", logininfo[1]);
            return emptyList();
        }
        Context ctx = contextService.getContext(ctxId);

        int userId;
        try {
            userId = userService.getUserId(logininfo[0], ctx);
        } catch (OXException e) {
            if (e.equalsCode(LdapExceptionCode.USER_NOT_FOUND.getNumber(), UserExceptionCode.PROPERTY_MISSING.getPrefix())) {
                LOGGER.debug("Missing user mapping for user \"{}\". Login failed.", logininfo[0]);
                return emptyList();
            }
            throw e;
        }
        return singletonList(new ResolvedMail(userId, ctxId));
    }

    /**
     * Splits user name and context.
     *
     * @param loginInfo the composite login information separated by an <code>'@'</code> sign
     * @return A String Array providing context and user name
     * @throws OXException If no separator is found
     */
    private static String[] split(final String loginInfo) {
        int pos = loginInfo.lastIndexOf('@');
        String[] retval = new String[2];
        if (pos < 0) {
            retval[0] = loginInfo;
            retval[1] = "defaultcontext";
        } else {
            retval[0] = loginInfo.substring(0, pos);
            retval[1] = loginInfo.substring(pos + 1);
        }
        return retval;
    }

    private static boolean contains(InternetAddress[] to, String userMail) {
        for (InternetAddress internetAddress : to) {
            if (userMail.equalsIgnoreCase(internetAddress.getAddress())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isContainedIn(CalendarObjectResource resource, String userMail) {
        CalendarUser user = new CalendarUser();
        user.setEMail(userMail);
        user.setUri(CalendarUtils.getURI(userMail));
        for (Event event : resource.getEvents()) {
            if (null != CalendarUtils.find(event.getAttendees(), user)) {
                return true;
            }
            if (CalendarUtils.matches(resource.getOrganizer(), user)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Processes a pushed Event for a certain user.
     *
     * @param message The message that has been pushed and parsed
     * @param syntheticSession The synthetic session spawned for the resolved user targeted by the pushed message
     */
    private void process(IncomingSchedulingMessage message, Session syntheticSession) throws OXException {
        CalendarSession cSession = initCalendarSession(services, syntheticSession);
        if (cSession == null) {
            return;
        }
        ITipProcessorService processorService = cSession.getSchedulingService().getITipProcessorService();
        autoProcessSchedulingMessage(processorService, cSession, message);
    }

    /**
     * Try to process the messages
     *
     * @param session The calendar session
     * @param mailMessage the messages to process
     * @throws OXException In case service is missing
     */
    private void process(MailAccess<?, ?> access, CalendarSession session, List<MailMessage> mails) {
        /*
         * Dont't process mails for other mail accounts
         */
        if (DEFAULT_ID != access.getAccountId()) {
            return;
        }
        /*
         * Check that no iTIP related flag is set to the mail and that the mail is an iMIP mail
         */
        List<MailMessage> unprocessed = mails.stream() //@formatter:off
            .filter(Objects::nonNull)
            .filter(m -> null == ITipMailFlag.fromUserFlags(m.getUserFlags()))
            .filter(m -> isSchedulingMessage(access, m))
            .filter(m -> isInTimeframe(session, m))
            .limit(MAX_MESSAGES)
            .collect(Collectors.toList());//@formatter:on
        unprocessed = filterMailFolders(access, unprocessed);
        /*
         * process all unprocessed scheduling mails
         */
        for (MailMessage mail : unprocessed) {
            process(access, session, mail);
        }
    }

    private void process(MailAccess<?, ?> access, CalendarSession session, MailMessage message) {
        ITipProcessorService processorService;
        try {
            processorService = session.getSchedulingService().getITipProcessorService();
        } catch (OXException e) {
            LOGGER.info("{} not available. Can't automatically process iMIP messages.", ITipProcessorService.class.getSimpleName(), e);
            return;
        }
        /*
         * initialize scheduling message from mail & process it
         */
        MessageStatus messageStatus;
        try {
            IncomingSchedulingMessage schedulingMessage = factory.createPatched(access, message, null, session.getSession());
            messageStatus = autoProcessSchedulingMessage(processorService, session, schedulingMessage);
        } catch (OXException e) {
            LOGGER.warn("Unexpected error auto-processing incoming schheduling mail, marking for manual processing.", e);
            messageStatus = MessageStatus.NEEDS_USER_INTERACTION;
        }
        /*
         * apply message status after processing to avoid repeated processing
         */
        try {
            messageStatusService.setMessageStatus(access, message, messageStatus);
        } catch (OXException e) {
            LOGGER.warn("Unexpected error storing messages status", e);
        }
    }

    private static List<MailMessage> filterMailFolders(MailAccess<?, ?> access, List<MailMessage> mails) {
        /*
         * Get mails per folder
         */
        HashMap<String, List<MailMessage>> mailsPerFolder = new HashMap<>(mails.size());
        for (MailMessage mail : mails) {
            com.openexchange.tools.arrays.Collections.put(mailsPerFolder, mail.getFolder(), mail);
        }
        /*
         * Check each mail folder if it is blacklisted or not
         */
        IMailFolderStorage folderStorage;
        try {
            folderStorage = access.getFolderStorage();
        } catch (OXException e) {
            LOGGER.warn("Unable to get folder storage: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
        for (Iterator<Entry<String, List<MailMessage>>> iterator = mailsPerFolder.entrySet().iterator(); iterator.hasNext();) {
            Entry<String, List<MailMessage>> entry = iterator.next();
            try {
                String fullName = entry.getKey();
                IMailFolderStorageInfoSupport infoSupport = folderStorage.supports(IMailFolderStorageInfoSupport.class);
                if (null != infoSupport && infoSupport.isInfoSupported()) {
                    /*
                     * Get information (probably) from cache
                     */
                    MailFolderInfo folderInfo = infoSupport.getFolderInfo(fullName);
                    if (isBlacklistedFolder(folderInfo)) {
                        iterator.remove();
                    }
                } else {
                    /*
                     * Load folder and inspect
                     */
                    MailFolder folder = folderStorage.getFolder(fullName);
                    if (isBlacklistedFolder(folder)) {
                        iterator.remove();
                    }
                }
            } catch (OXException e) {
                LOGGER.warn("Unable to resolve mail folder {}. Skipping auto-processing for mails in this folder", e.getMessage(), e);
                iterator.remove();
            }
        }
        /*
         * Convert back to list
         */
        Collection<List<MailMessage>> values = mailsPerFolder.values();
        if (values.isEmpty()) {
            return Collections.emptyList();
        }
        List<MailMessage> result = new ArrayList<>(values.size());
        values.forEach(result::addAll);
        return result;
    }

    /**
     * Gets a value indicating whether automatically processing is allowed in the given mail folder or not
     *
     * @param info The mail folder to check
     * @return <code>true</code> if the folder is blacklisted, <code>false</code> otherwise
     */
    private static boolean isBlacklistedFolder(MailFolderInfo info) {
        if (info.isShared() || info.isPublic()) {
            return true;
        }
        return info.isSpam() || info.isTrash() || info.isDrafts() || info.isSent() || info.isConfirmedSpam() || info.isConfirmedHam();
    }

    /**
     * Gets a value indicating whether automatically processing is allowed in the given mail folder or not
     *
     * @param folder The mail folder to check
     * @return <code>true</code> if the folder is blacklisted, <code>false</code> otherwise
     */
    private static boolean isBlacklistedFolder(MailFolder folder) {
        if (folder.isShared() || folder.isPublic()) {
            return true;
        }
        return folder.isSpam() || folder.isTrash() || folder.isDrafts() || folder.isSent() || folder.isConfirmedSpam() || folder.isConfirmedHam();
    }

    private boolean isInTimeframe(CalendarSession session, MailMessage m) {
        long timespan;
        try {
            String value = services.getServiceSafe(LeanConfigurationService.class).getProperty(session.getUserId(), session.getContextId(), PROPERTY_AUTOSCHEDULE_TIMEFRAME);
            timespan = TimeSpanParser.parseTimespanToPrimitive(value);
        } catch (IllegalArgumentException | OXException e) {
            LOGGER.warn("Error getting value for {}, falling back to defaults.", PROPERTY_AUTOSCHEDULE_TIMEFRAME.getFQPropertyName(), e);
            timespan = TimeSpanParser.parseTimespanToPrimitive(PROPERTY_AUTOSCHEDULE_TIMEFRAME.getDefaultValue(String.class));
        }
        Date receivedDate = m.getReceivedDateDirect();
        if (0 >= timespan || null == receivedDate) {
            return true;
        }
        return receivedDate.getTime() > System.currentTimeMillis() - timespan;
    }

    /**
     * Checks if the mail is a scheduling mails and can be processed
     *
     * @param access The mail access
     * @param m The mail
     * @return <code>true</code> if the message is a scheduling mail, <code>false</code> otherwise
     */
    private static boolean isSchedulingMessage(MailAccess<?, ?> access, MailMessage m) {
        try {
            if (access.getMailConfig().getCapabilities().hasMailStructure() && null != MailUtils.getIcalAttachmenStructure(m.getMailStructure())) {
                return true;
            }
        } catch (OXException e) {
            LOGGER.debug("Unable to get body structure from mail: {}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * Tries to automatically process an {@link IncomingSchedulingMessage} to the calendar
     *
     * @param processorService The {@link ITipProcessorService}
     * @param session The session
     * @param schedulingMessage The message to process and apply to the calendar
     */
    @NonNull private static MessageStatus autoProcessSchedulingMessage(ITipProcessorService processorService, CalendarSession session, IncomingSchedulingMessage schedulingMessage) throws OXException {
        /*
         * Try to automatically apply changes
         */
        ProcessResult processResult = processorService.process(schedulingMessage, session);
        if (null == processResult) {
            LOGGER.debug("Scheduling message {} could not be applied", schedulingMessage);
            return MessageStatus.NEEDS_USER_INTERACTION;
        }
        LOGGER.debug("Processed scheduling message {} with result {}", schedulingMessage, processResult);
        return processResult.getMessageStatus();
    }

    /**
     * Initializes a {@link CalendarSession} for iMIP handling
     *
     * @param services The {@link ServiceLookup} to get the {@link CalendarService} from
     * @param session The session
     * @return A new {@link CalendarSession} or <code>null</code> if unable to do so
     */
    private static CalendarSession initCalendarSession(ServiceLookup services, Session session) {
        try {
            CalendarSession calendarSession = services.getServiceSafe(CalendarService.class).init(session);
            calendarSession.set(CalendarParameters.PARAMETER_IGNORE_STORAGE_WARNINGS, Boolean.TRUE);
            calendarSession.set(CalendarParameters.PARAMETER_SKIP_EXTERNAL_ATTENDEE_URI_CHECKS, Boolean.TRUE);
            return calendarSession;
        } catch (OXException e) {
            LOGGER.debug("Unable to initialize calendar session for user {} in context {}", I(session.getUserId()), I(session.getContextId()), e);
        }
        return null;
    }

    /**
     * Gets a value indicating whether the auto processing of iMIP mails is disabled or not
     * <p>
     * Validates properties in this order
     * <li>{@link #ON_MAIL_FETCH_PROPERTY} as server property</li>
     * <li>{@link com.openexchange.chronos.common.UserConfigWrapper#KEY_AUTO_PROCESS_IMIP} as user property from the JSLob</li>
     * <li>{@link com.openexchange.chronos.scheduling.AutoProcessIMip#AUTO_PROCESS_IMIP_PROPERTY} as server fallback for the user property</li>
     *
     * @param session The user session
     * @return <code>true</code> if the processing is <b>disabled</b>, <code>false</code> if enabled
     * @throws OXException in case of missing service
     */
    private boolean isAutoProcessingDisabled(CalendarSession session) throws OXException {
        LeanConfigurationService configService = services.getService(LeanConfigurationService.class);
        boolean isEnabled;
        if (null == configService) {
            LOGGER.debug("Missing service {}. Using fallbacks.", LeanConfigurationService.class.getSimpleName());
            isEnabled = b(ON_MAIL_FETCH_PROPERTY.getDefaultValue(Boolean.class));
        } else {
            isEnabled = configService.getBooleanProperty(session.getUserId(), session.getContextId(), ON_MAIL_FETCH_PROPERTY);
        }
        if (!isEnabled) {
            return true;
        }
        AutoProcessIMip processIMip = session.getConfig().getAutoProcessIMip(session.getUserId());
        if (null == processIMip) {
            processIMip = AutoProcessIMip.getConfiguredValue(session.getContextId(), session.getUserId(), configService);
        }
        return AutoProcessIMip.NEVER.equals(processIMip);
    }

    /**
     * Optionally gets and decodes the chronos-specific itip data if set as header within the supplied mail.
     *
     * @param mailMessage The mail message to retrieve the itip data from
     * @return The itip data, or <code>null</code> if not found or unparseable
     */
    private static ChronosITipData optITipData(MailMessage mailMessage) {
        if (null != mailMessage) {
            String itipDataValue = mailMessage.getFirstHeader(ChronosITipData.PROPERTY_NAME);
            if (Strings.isNotEmpty(itipDataValue)) {
                try {
                    return ChronosITipData.decode(itipDataValue);
                } catch (Exception e) {
                    LOGGER.warn("Error parsing chronos itip data from mail header, ignoring.", e);
                }
            }
        }
        return null;
    }

}
