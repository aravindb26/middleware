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

import static com.openexchange.chronos.scheduling.analyzers.Utils.getAnalysis;
import static com.openexchange.chronos.scheduling.analyzers.Utils.getInsufficientPermissionsAnalysis;
import java.util.List;
import com.openexchange.chronos.CalendarObjectResource;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.scheduling.AnalyzedChange;
import com.openexchange.chronos.scheduling.ITipAnalysis;
import com.openexchange.chronos.scheduling.IncomingSchedulingMessage;
import com.openexchange.chronos.scheduling.SchedulingAnalyzer;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;

/**
 * {@link AbstractSchedulingAnalyzer}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public abstract class AbstractSchedulingAnalyzer implements SchedulingAnalyzer {

    /** The default event fields to load when retrieving the currently stored calendar object resources from the storage */
    protected static final EventField[] DEFAULT_FIELDS = { //@formatter:off
        EventField.ID, EventField.SERIES_ID, EventField.FOLDER_ID, EventField.UID, EventField.RECURRENCE_ID, EventField.RECURRENCE_RULE,
        EventField.RECURRENCE_DATES, EventField.DELETE_EXCEPTION_DATES, EventField.CHANGE_EXCEPTION_DATES, EventField.START_DATE, 
        EventField.SEQUENCE, EventField.DTSTAMP, EventField.ORGANIZER, EventField.ATTENDEES, EventField.SUMMARY
    }; //@formatter:on

    protected final ServiceLookup services;
    protected final SchedulingMethod method;

    /**
     * Initializes a new {@link AbstractSchedulingAnalyzer}.
     * 
     * @param services A service lookup reference
     * @param method The handled scheduling method
     */
    protected AbstractSchedulingAnalyzer(ServiceLookup services, SchedulingMethod method) {
        super();
        this.services = services;
        this.method = method;
    }

    @Override
    public ITipAnalysis analyze(IncomingSchedulingMessage message, CalendarSession session) throws OXException {
        /*
         * lookup currently stored data associated with the targeted calendar object resource & check access permissions
         */
        CalendarUser originator = message.getSchedulingObject().getOriginator();
        int targetUser = message.getTargetUser();
        ObjectResourceProvider objectResourceProvider = new ObjectResourceProvider(session, message, getFieldsToLoad());
        if (false == Check.hasAccess(services, session, targetUser, objectResourceProvider.getStoredResource())) {
            return getInsufficientPermissionsAnalysis(getMethod(), message.getResource().getUid());
        }
        /*
         * analyze individual changes from each event of the incoming resource, melt changes into overall analysis & return result
         */
        List<AnalyzedChange> analyzedChanges = analyze(session, objectResourceProvider, originator, targetUser);
        CalendarObjectResource storedResource = objectResourceProvider.getStoredResource();
        CalendarObjectResource storedRelatedResource = null != storedResource ? null : objectResourceProvider.getStoredRelatedResource();
        return getAnalysis(getMethod(), message.getResource().getUid(), analyzedChanges, storedResource, storedRelatedResource);
    }

    /**
     * Gets the event fields to load when retrieving the currently stored calendar object resources from the storage, or <code>null</code>
     * to load all fields.
     * <p/>
     * By default, the {@link #DEFAULT_FIELDS} are used.
     * 
     * @return The fields, to load, or <code>null</code> to load all fields
     */
    protected EventField[] getFieldsToLoad() {
        return DEFAULT_FIELDS;
    }

    /**
     * Analyzes each event from an incoming scheduling message.
     * 
     * @param session The calendar session
     * @param objectResourceProvider The provider yielding access to the incoming and stored calendar object resource
     * @param originator The originator of the scheduling message
     * @param targetUser The targeted calendar user
     * @return The analyzed changes
     */
    protected abstract List<AnalyzedChange> analyze(CalendarSession session, ObjectResourceProvider objectResourceProvider, CalendarUser originator, int targetUser) throws OXException;

    @Override
    public SchedulingMethod getMethod() {
        return method;
    }

}
