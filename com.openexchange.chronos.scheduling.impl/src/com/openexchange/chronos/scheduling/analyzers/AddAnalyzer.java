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

package com.openexchange.chronos.scheduling.analyzers;

import java.util.ArrayList;
import java.util.List;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.scheduling.AnalyzedChange;
import com.openexchange.chronos.scheduling.ITipAction;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.chronos.scheduling.analyzers.annotations.AnnotationHelper;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;

/**
 * {@link AddAnalyzer} - Analyzer for the iTIP method <code>ADD</code>
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc5546#section-3.2.4">RFC 5546 Section 3.2.4</a>
 */
public class AddAnalyzer extends AbstractSchedulingAnalyzer {

    /**
     * Initializes a new {@link AddAnalyzer}.
     * 
     * @param services The service lookup
     */
    public AddAnalyzer(ServiceLookup services) {
        super(services, SchedulingMethod.ADD);
    }

    @Override
    protected EventField[] getFieldsToLoad() {
        return null;
    }

    @Override
    protected List<AnalyzedChange> analyze(CalendarSession session, ObjectResourceProvider objectResourceProvider, CalendarUser originator, int targetUser) throws OXException {
        List<AnalyzedChange> analyzedChanges = new ArrayList<AnalyzedChange>();
        for (Event patchedEvent : objectResourceProvider.getIncomingEvents()) {
            analyzedChanges.add(analyzeUnknownEvent(session, patchedEvent, originator, targetUser));
        }
        return analyzedChanges;
    }

    /**
     * Analyzes the changes for a single event as part of an incoming {@link SchedulingMethod#ADD} message from the originator, where no
     * corresponding stored event (occurrence) exists for.
     * 
     * @param session The underlying calendar session
     * @param event The (patched) event from the incoming scheduling object resource
     * @param originator The originator of the scheduling message
     * @param targetUser The user id of the scheduling message's recipient
     * @return The analyzed change
     */
    private AnalyzedChange analyzeUnknownEvent(CalendarSession session, Event event, CalendarUser originator, int targetUser) throws OXException {
        /*
         * add introductional annotation for the incoming message as such
         */
        AnnotationHelper annotationHelper = new AnnotationHelper(services, session);
        AnalyzedChange change = new AnalyzedChange();
        change.addAnnotation(annotationHelper.getAddIntroduction(event, originator));
        /*
         * ADD is not supported directly; say so & recommend to ask for the complete resource
         */
        change.addAnnotation(annotationHelper.getAddUnsupportedHint(targetUser));
        change.addAnnotation(annotationHelper.getAskForRefreshHint());
        change.addActions(ITipAction.IGNORE, ITipAction.REQUEST_REFRESH);
        return change;
    }

}
