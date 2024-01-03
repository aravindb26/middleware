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

package com.openexchange.chronos.scheduling.impl;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.tools.arrays.Collections.isNullOrEmpty;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.annotation.NonNull;
import com.openexchange.chronos.CalendarObjectResource;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.DefaultCalendarResult;
import com.openexchange.chronos.scheduling.AnalyzedChange;
import com.openexchange.chronos.scheduling.AutoProcessIMip;
import com.openexchange.chronos.scheduling.ITipAction;
import com.openexchange.chronos.scheduling.ITipAnalysis;
import com.openexchange.chronos.scheduling.ITipProcessor;
import com.openexchange.chronos.scheduling.ITipProcessorService;
import com.openexchange.chronos.scheduling.IncomingSchedulingMessage;
import com.openexchange.chronos.scheduling.MessageStatus;
import com.openexchange.chronos.scheduling.ProcessResult;
import com.openexchange.chronos.scheduling.SchedulingAnalyzer;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.chronos.scheduling.analyzers.AddAnalyzer;
import com.openexchange.chronos.scheduling.analyzers.CancelAnalyzer;
import com.openexchange.chronos.scheduling.analyzers.CounterAnalyzer;
import com.openexchange.chronos.scheduling.analyzers.DeclineCounterAnalyzer;
import com.openexchange.chronos.scheduling.analyzers.PublishAnalyzer;
import com.openexchange.chronos.scheduling.analyzers.RefreshAnalyzer;
import com.openexchange.chronos.scheduling.analyzers.ReplyAnalyzer;
import com.openexchange.chronos.scheduling.analyzers.RequestAnalyzer;
import com.openexchange.chronos.scheduling.common.Utils;
import com.openexchange.chronos.service.CalendarResult;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.contact.common.ContactsParameters;
import com.openexchange.contact.provider.composition.IDBasedContactsAccess;
import com.openexchange.contact.provider.composition.IDBasedContactsAccessFactory;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.search.ContactsSearchObject;
import com.openexchange.java.Strings;
import com.openexchange.lock.LockService;
import com.openexchange.osgi.ServiceSet;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.id.IDMangler;

/**
 * 
 * {@link ITipProcessorServiceImpl}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.6
 */
public class ITipProcessorServiceImpl implements ITipProcessorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ITipProcessorServiceImpl.class);

    private ServiceSet<ITipProcessor> processors;

    private EnumMap<SchedulingMethod, SchedulingAnalyzer> analyzers;

    private final ServiceLookup services;

    /**
     * Initializes a new {@link ITipProcessorServiceImpl}.
     *
     * @param processors The processors to use
     * @param services The service lookup
     */
    public ITipProcessorServiceImpl(ServiceSet<ITipProcessor> processors, ServiceLookup services) {
        this.services = services;
        this.processors = processors;
        analyzers = new EnumMap<>(SchedulingMethod.class);
        addAnalyzer(new AddAnalyzer(services));
        addAnalyzer(new CancelAnalyzer(services));
        addAnalyzer(new CounterAnalyzer(services));
        addAnalyzer(new DeclineCounterAnalyzer(services));
        addAnalyzer(new PublishAnalyzer(services));
        addAnalyzer(new RefreshAnalyzer(services));
        addAnalyzer(new ReplyAnalyzer(services));
        addAnalyzer(new RequestAnalyzer(services));
    }

    private void addAnalyzer(SchedulingAnalyzer analyzer) {
        analyzers.put(analyzer.getMethod(), analyzer);
    }

    @Override
    public List<ITipAnalysis> analyze(List<IncomingSchedulingMessage> messages, CalendarSession session) throws OXException {
        List<ITipAnalysis> retval = new ArrayList<>();
        for (IncomingSchedulingMessage message : messages) {
            retval.add(analyze(message, session));
        }
        return retval;
    }

    @Override
    public ITipAnalysis analyze(IncomingSchedulingMessage message, CalendarSession session) throws OXException {
        SchedulingAnalyzer analyzer = analyzers.get(message.getMethod());
        return analyzer.analyze(message, session);
    }

    @Override
    public ProcessResult process(IncomingSchedulingMessage message, CalendarSession session) throws OXException {
        Lock lock = null;
        try {
            lock = services.getServiceSafe(LockService.class).getSelfCleaningLockFor(getLockName(session, message));
            lock.lock();
            /*
             * no need to analyze further in case of internal scheduling mails (applied implicitly)
             */
            if (Utils.isInternalSchedulingResource(session, message)) {
                LOGGER.debug("Skipping analysis for internal scheduling message {}", message);
                return new ProcessResultImpl(getEmptyResult(session, message.getTargetUser()), new ITipAnalysis(), MessageStatus.APPLIED);
            }
            /*
             * Analyze and check if we can apply the message
             */
            ITipAnalysis analysis = analyze(message, session);
            if (false == isApplicable(analysis)) {
                LOGGER.debug("Analysis yields no applicable changes: {}", analysis);
                return new ProcessResultImpl(getEmptyResult(session, message.getTargetUser()), analysis, MessageStatus.NEEDS_USER_INTERACTION);
            }
            /*
             * Check if configuration allows a processing
             */
            switch (getAutoProcessingValue(message, session)) {
                case ALWAYS:
                    break;
                case KNOWN:
                    CalendarUser originator = message.getSchedulingObject().getOriginator();
                    if (isContainedIn(analysis.getOriginalResource(), originator)) {
                        LOGGER.debug("Originator \"{}\" successfully looked up in targeted scheduling resource, continue processing", originator);
                        break;
                    }
                    if (isContainedIn(analysis.getStoredRelatedResource(), originator)) {
                        LOGGER.debug("Originator \"{}\" successfully looked up in existing related scheduling resource, continue processing", originator);
                        break;
                    }
                    if (message.getTargetUser() == session.getUserId() && (isKnownContact(session, originator) || // 
                        null != originator.getSentBy() && isKnownContact(session, originator.getSentBy()))) {
                        LOGGER.debug("Originator \"{}\" successfully looked up in user's address book, continue processing", originator);
                        break;
                    }
                    LOGGER.debug("Originator \"{}\" is unknown. Stop automatically processing", message.getSchedulingObject().getOriginator());
                    return new ProcessResultImpl(getEmptyResult(session, message.getTargetUser()), analysis, MessageStatus.NEEDS_USER_INTERACTION);
                case NEVER:
                default:
                    LOGGER.debug("Automatic processing disabled by configuration for user {} in context {}", I(session.getUserId()), I(session.getContextId()));
                    return new ProcessResultImpl(getEmptyResult(session, message.getTargetUser()), analysis, MessageStatus.NEEDS_USER_INTERACTION);
            }
            /*
             * Process message
             */
            for (ITipProcessor processor : processors) {
                CalendarResult result = processor.process(message, session);
                if (null != result) {
                    LOGGER.debug("Successfully applied {}", message);
                    return new ProcessResultImpl(result, analysis, MessageStatus.APPLIED);
                }
            }
            /*
             * Couldn't apply data. Needs to be handled by the user
             */
            return new ProcessResultImpl(getEmptyResult(session, message.getTargetUser()), analysis, MessageStatus.NEEDS_USER_INTERACTION);
        } finally {
            if (null != lock) {
                lock.unlock();
            }
        }
    }

    @Override
    public List<ProcessResult> process(List<IncomingSchedulingMessage> messages, CalendarSession session) throws OXException {
        List<ProcessResult> retval = new ArrayList<>();
        for (IncomingSchedulingMessage message : messages) {
            ProcessResult processResult = process(message, session);
            if (null != processResult) {
                retval.add(processResult);
            }
        }
        return retval;
    }

    /*
     * ============================== HELPERS ==============================
     */

    private @NonNull AutoProcessIMip getAutoProcessingValue(IncomingSchedulingMessage message, CalendarSession session) throws OXException {
        AutoProcessIMip autoProcessIMip = session.getConfig().getAutoProcessIMip(message.getTargetUser());
        if (null == autoProcessIMip) {
            return AutoProcessIMip.getConfiguredValue(session.getContextId(), message.getTargetUser(), services.getOptionalService(LeanConfigurationService.class));
        }
        return autoProcessIMip;
    }

    private static CalendarResult getEmptyResult(CalendarSession session, int calendarUserId) {
        return new DefaultCalendarResult(session.getSession(), calendarUserId, null, null, null, null);
    }

    /**
     * Gets a value indicating whether a calendar user is 'known' to an internal user in terms of being stored as contact in one of his
     * address books.
     *
     * @param session The user's session
     * @param calendarUser The calendar user to lookup
     * @return <code>true</code> if the calendar user is known, <code>false</code> otherwise
     */
    private boolean isKnownContact(CalendarSession session, CalendarUser calendarUser) {
        /*
         * extract email address & perform search
         */
        String mail = CalendarUtils.optEMailAddress(calendarUser.getUri());
        if (Strings.isEmpty(mail) || CalendarUtils.isICloudIMipMeCom(calendarUser)) {
            mail = calendarUser.getEMail();
            if (Strings.isEmpty(mail)) {
                LOGGER.trace("No email extracted from {}, contact lookup not possible.", calendarUser);
                return false;
            }
        }
        try {
            /*
             * Search for the originator in the contacts
             */
            IDBasedContactsAccessFactory factory = services.getOptionalService(IDBasedContactsAccessFactory.class);
            if (null != factory) {
                ContactsSearchObject cso = new ContactsSearchObject();
                cso.setAllEmail(mail);
                cso.setExactMatch(true);
                cso.setSubfolderSearch(true);
                cso.setOrSearch(true);
                IDBasedContactsAccess access = factory.createAccess(session.getSession());
                try {
                    access.set(ContactsParameters.PARAMETER_FIELDS, new ContactField[] { ContactField.OBJECT_ID });
                    access.set(ContactsParameters.PARAMETER_LEFT_HAND_LIMIT, I(0));
                    access.set(ContactsParameters.PARAMETER_RIGHT_HAND_LIMIT, I(1));
                    return false == isNullOrEmpty(access.searchContacts(cso));
                } finally {
                    access.finish();
                }
            }
        } catch (OXException e) {
            session.addWarning(e);
        }
        return false;
    }

    private String getLockName(CalendarSession session, IncomingSchedulingMessage message) {
        return IDMangler.mangle(this.getClass().getSimpleName(), message.getResource().getUid(), String.valueOf(session.getContextId()), String.valueOf(message.getTargetUser()));
    }

    /**
     * Gets a value indicating whether the supplied set of iTIP changes and their actions contains an action indicating 
     * that the changes can be applied to the user's calendar.
     * 
     * @param actions The actions from an iTIP analysis to check
     * @return <code>true</code> if the an applicable action is contained, <code>false</code>, otherwise
     */
    private static boolean isApplicable(ITipAnalysis analysis) {
        for (AnalyzedChange change : analysis.getAnalyzedChanges()) {
            Set<ITipAction> actions = change.getActions();
            if (null != actions && (actions.contains(ITipAction.APPLY_CREATE) || actions.contains(ITipAction.APPLY_CHANGE) || actions.contains(ITipAction.APPLY_REMOVE) || actions.contains(ITipAction.APPLY_RESPONSE))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isContainedIn(CalendarObjectResource resource, CalendarUser calendarUser) {
        return null != resource && (null != CalendarUtils.find(resource, calendarUser) || // 
            CalendarUtils.isSimilarICloudIMipMeCom(resource.getOrganizer(), calendarUser));
    }

}
