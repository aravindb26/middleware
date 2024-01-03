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

package com.openexchange.chronos.impl;

import static com.openexchange.chronos.impl.Utils.postProcess;
import static com.openexchange.chronos.impl.scheduling.SchedulingUtils.usesOrganizerCopy;
import static com.openexchange.chronos.scheduling.common.Utils.optSentByResource;
import org.slf4j.LoggerFactory;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.impl.performer.ResolvePerformer;
import com.openexchange.chronos.impl.scheduling.AddProcessor;
import com.openexchange.chronos.impl.scheduling.AttendeeUpdateProcessor;
import com.openexchange.chronos.impl.scheduling.CancelProcessor;
import com.openexchange.chronos.impl.scheduling.CounterProcessor;
import com.openexchange.chronos.impl.scheduling.RefreshProcessor;
import com.openexchange.chronos.impl.scheduling.ReplyProcessor;
import com.openexchange.chronos.impl.scheduling.RequestProcessor;
import com.openexchange.chronos.scheduling.IncomingSchedulingMessage;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.chronos.scheduling.SchedulingSource;
import com.openexchange.chronos.service.CalendarResult;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.SchedulingUtilities;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;
import com.openexchange.osgi.annotation.SingletonService;
import com.openexchange.server.ServiceLookup;

/**
 * {@link SchedulingUtilitiesImpl} - Class providing utility method for processing and updating the calendar based
 * on am incoming scheduling message
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.4
 */
@SingletonService
public class SchedulingUtilitiesImpl implements SchedulingUtilities {

    protected final ServiceLookup services;

    /**
     * Initializes a new {@link SchedulingUtilitiesImpl}.
     * 
     * @param serviceLookup The {@link ServiceLookup}
     *
     */
    public SchedulingUtilitiesImpl(ServiceLookup serviceLookup) {
        super();
        this.services = serviceLookup;
    }

    @Override
    public CalendarResult processAdd(CalendarSession session, SchedulingSource source, IncomingSchedulingMessage message, Attendee attendee) throws OXException {
        return postProcess(services, new InternalCalendarStorageOperation<InternalCalendarResult>(session) {

            @Override
            protected InternalCalendarResult execute(CalendarSession session, CalendarStorage storage) throws OXException {
                AddProcessor addProcessor = new AddProcessor(storage, session, getTargetFolder(session, storage, message), source);
                addProcessor.process(message);
                return new AttendeeUpdateProcessor(addProcessor, message.getMethod()).process(message, attendee);
            }
        }.executeUpdate()).getUserizedResult();
    }

    @Override
    public CalendarResult processCancel(CalendarSession session, SchedulingSource source, IncomingSchedulingMessage message) throws OXException {
        return postProcess(services, new InternalCalendarStorageOperation<InternalCalendarResult>(session) {

            @Override
            protected InternalCalendarResult execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new CancelProcessor(storage, session, getTargetFolder(session, storage, message), source).process(message);
            }
        }.executeUpdate()).getUserizedResult();
    }

    @Override
    public CalendarResult processReply(CalendarSession session, SchedulingSource source, IncomingSchedulingMessage message) throws OXException {
        return postProcess(services, new InternalCalendarStorageOperation<InternalCalendarResult>(session) {

            @Override
            protected InternalCalendarResult execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new ReplyProcessor(storage, session, getTargetFolder(session, storage, message), source).process(message);
            }
        }.executeUpdate()).getUserizedResult();
    }

    @Override
    public CalendarResult processRequest(CalendarSession session, SchedulingSource source, IncomingSchedulingMessage message, Attendee attendee) throws OXException {
        return postProcess(services, new InternalCalendarStorageOperation<InternalCalendarResult>(session) {

            @Override
            protected InternalCalendarResult execute(CalendarSession session, CalendarStorage storage) throws OXException {
                /*
                 * perform immediate attendee updates when operating on the organizer copy of known calendar object resources from internal notification mails
                 */
                if (usesOrganizerCopy(session, storage, message)) {
                    return new AttendeeUpdateProcessor(storage, session, getTargetFolder(session, storage, message), message.getMethod()).process(message, attendee);
                }
                /*
                 * otherwise, perform attendee updates after applying the incoming REQUEST
                 */
                RequestProcessor requestProcessor = new RequestProcessor(session, storage, getTargetFolder(session, storage, message), source);
                requestProcessor.process(message);
                return new AttendeeUpdateProcessor(requestProcessor, message.getMethod()).process(message, attendee);
            }
        }.executeUpdate()).getUserizedResult();
    }

    @Override
    public CalendarResult processRefresh(CalendarSession session, IncomingSchedulingMessage message) throws OXException {
        return postProcess(services, new InternalCalendarStorageOperation<InternalCalendarResult>(session) {

            @Override
            protected InternalCalendarResult execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new RefreshProcessor(session, storage, getTargetFolder(session, storage, message)).process(message);
            }
        }.executeUpdate()).getUserizedResult();
    }

    @Override
    public CalendarResult processCounter(CalendarSession session, SchedulingSource source, IncomingSchedulingMessage message, boolean decline) throws OXException {
        if (false == SchedulingSource.API.equals(source)) {
            LoggerFactory.getLogger(SchedulingUtilitiesImpl.class).debug("Stop auto-processing of COUNTER. The changes need to be applied manually.");
            return null;
        }
        return postProcess(services, new InternalCalendarStorageOperation<InternalCalendarResult>(session) {

            @Override
            protected InternalCalendarResult execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new CounterProcessor(session, storage, getTargetFolder(session, storage, message)).process(message, decline);
            }
        }.executeUpdate()).getUserizedResult();
    }

    /*
     * ============================== HELPERS ==============================
     */

    static CalendarFolder getTargetFolder(CalendarSession session, CalendarStorage storage, IncomingSchedulingMessage message) throws OXException {
        int sentByResource = optSentByResource(session, message);
        if (0 < sentByResource) {
            return Utils.getResourceCalendarFolder(session, sentByResource);
        }
        String uid = message.getResource().getUid();
        int calendarUserId = message.getTargetUser();
        boolean fallbackToDefault = SchedulingMethod.REQUEST.equals(message.getMethod());
        String folderId = new ResolvePerformer(session, storage).resolveFolderIdByUID(uid, calendarUserId, fallbackToDefault);
        if (null == folderId) {
            throw CalendarExceptionCodes.EVENT_NOT_FOUND.create(uid);
        }
        return Utils.getFolder(session, folderId);
    }
}
