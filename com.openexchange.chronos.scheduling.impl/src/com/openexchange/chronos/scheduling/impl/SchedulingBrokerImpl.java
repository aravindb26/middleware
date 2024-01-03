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

import static com.openexchange.chronos.service.CalendarParameters.PARAMETER_DECLINE_COUNTER;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.b;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.annotation.Nullable;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.scheduling.ITipProcessor;
import com.openexchange.chronos.scheduling.IncomingSchedulingMessage;
import com.openexchange.chronos.scheduling.ScheduleStatus;
import com.openexchange.chronos.scheduling.SchedulingBroker;
import com.openexchange.chronos.scheduling.SchedulingMessage;
import com.openexchange.chronos.scheduling.SchedulingSource;
import com.openexchange.chronos.scheduling.TransportProvider;
import com.openexchange.chronos.service.CalendarResult;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.SchedulingUtilities;
import com.openexchange.exception.OXException;
import com.openexchange.osgi.RankingAwareNearRegistryServiceTracker;
import com.openexchange.osgi.annotation.SingletonService;
import com.openexchange.session.Session;

/**
 * {@link SchedulingBrokerImpl}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.3
 */
@SingletonService
public class SchedulingBrokerImpl extends RankingAwareNearRegistryServiceTracker<TransportProvider> implements SchedulingBroker, ITipProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulingBrokerImpl.class);

    /**
     * Defined {@link ScheduleStatus} in which sending can be considered a success.
     */
    private static final EnumSet<ScheduleStatus> SUCCESS = EnumSet.of(ScheduleStatus.SENT, ScheduleStatus.DELIVERED);

    /**
     * Initializes a new {@link SchedulingBrokerImpl}.
     *
     * @param context The {@link BundleContext}
     */
    public SchedulingBrokerImpl(BundleContext context) {
        super(context, TransportProvider.class);
    }

    @Override
    public List<ScheduleStatus> handleScheduling(Session session, List<SchedulingMessage> messages) {
        List<ScheduleStatus> result = new LinkedList<>();
        for (SchedulingMessage message : messages) {
            try {
                ScheduleStatus status = handle(session, message);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("A {} message for {} sent by {} has been handled with status {}.", message.getMethod(), message.getRecipient(), message.getOriginator(), status);
                }
                result.add(status);
            } catch (Exception e) {
                LOGGER.warn("Unexpected error handling {} message for {} sent by {}: {}", message.getMethod(), message.getRecipient(), message.getOriginator(), e.getMessage(), e);
            }
        }
        return result;
    }

    private ScheduleStatus handle(Session session, SchedulingMessage message) {
        if (null != message && null != session) {
            /*
             * Try to send message. Stop once the message is send successfully
             */
            for (Iterator<TransportProvider> iterator = iterator(); iterator.hasNext();) {
                TransportProvider transportProvider = iterator.next();
                ScheduleStatus status = transportProvider.send(session, message);
                if (SUCCESS.contains(status)) {
                    return status;
                }
            }
        }
        return ScheduleStatus.NO_TRANSPORT;
    }

    @Override
    public CalendarResult handleIncomingScheduling(CalendarSession session, SchedulingSource source, IncomingSchedulingMessage message, Attendee attendee) throws OXException {
        SchedulingUtilities utilities = session.getSchedulingService().getSchedulingUtilities();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("A message for user {} in context {} with method {} will be processed from source {}.", I(session.getUserId()), I(session.getContextId()), message.getMethod(), source);
        }
        /*
         * Process incoming
         */
        CalendarResult result;
        switch (message.getMethod()) {
            case ADD:
                result = utilities.processAdd(session, source, message, attendee);
                break;
            case CANCEL:
                result = utilities.processCancel(session, source, message);
                break;
            case REPLY:
                result = utilities.processReply(session, source, message);
                break;
            case REQUEST:
                result = utilities.processRequest(session, source, message, attendee);
                break;
            case REFRESH:
                result = utilities.processRefresh(session, message);
                break;
            case COUNTER:
                result = utilities.processCounter(session, source, message, b(session.get(PARAMETER_DECLINE_COUNTER, Boolean.class, Boolean.FALSE)));
                break;
            default:
                return null;
        }
        return result;
    }

    @Override
    @Nullable
    public CalendarResult process(IncomingSchedulingMessage message, CalendarSession session) throws OXException {
        try {
            return handleIncomingScheduling(session, SchedulingSource.SERVICE, message, null);
        } catch (OXException e) {
            if (false == CalendarExceptionCodes.PREFIX.equals(e.getPrefix())) {
                // No calendar specific exception, throw 
                throw e;
            }
            // Various reasons why data is not applied (permission, concurrency, etc.). Add warning to session, return empty result
            LOGGER.debug("Unable to apply message", e);
            session.addWarning(e);
        }
        return null;
    }

}
