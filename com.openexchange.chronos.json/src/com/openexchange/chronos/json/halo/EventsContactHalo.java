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

package com.openexchange.chronos.json.halo;

import static com.openexchange.chronos.json.action.ChronosJsonParameters.PARAM_EXPAND;
import static com.openexchange.chronos.json.action.ChronosJsonParameters.PARAM_FIELDS;
import static com.openexchange.chronos.json.action.ChronosJsonParameters.PARAM_ORDER;
import static com.openexchange.chronos.json.action.ChronosJsonParameters.PARAM_ORDER_BY;
import static com.openexchange.chronos.json.action.ChronosJsonParameters.PARAM_RANGE_END;
import static com.openexchange.chronos.json.action.ChronosJsonParameters.PARAM_RANGE_START;
import static com.openexchange.chronos.json.action.ChronosJsonParameters.parseParameters;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.tools.arrays.Collections.unmodifiableSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.common.SearchUtils;
import com.openexchange.chronos.json.converter.EventResultConverter;
import com.openexchange.chronos.provider.AccountAwareCalendarFolder;
import com.openexchange.chronos.provider.composition.IDBasedCalendarAccess;
import com.openexchange.chronos.provider.composition.IDBasedCalendarAccessFactory;
import com.openexchange.chronos.provider.groupware.GroupwareFolderType;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.EventsResult;
import com.openexchange.contact.halo.AbstractContactHalo;
import com.openexchange.contact.halo.HaloContactDataSource;
import com.openexchange.contact.halo.HaloContactQuery;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.Contact;
import com.openexchange.search.CompositeSearchTerm;
import com.openexchange.search.CompositeSearchTerm.CompositeOperation;
import com.openexchange.search.SearchTerm;
import com.openexchange.search.SingleSearchTerm.SingleOperation;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.User;

/**
 * {@link EventsContactHalo}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class EventsContactHalo extends AbstractContactHalo implements HaloContactDataSource {

    private static final Set<String> REQUIRED_PARAMETERS = unmodifiableSet(PARAM_RANGE_START, PARAM_RANGE_END);
    private static final Set<String> OPTIONAL_PARAMETERS = unmodifiableSet(PARAM_EXPAND, PARAM_ORDER_BY, PARAM_ORDER, PARAM_FIELDS);

    private final ServiceLookup services;

    /**
     * Initializes a new {@link EventsContactHalo}.
     *
     * @param services A service lookup reference
     */
    public EventsContactHalo(ServiceLookup services) {
        super();
        this.services = services;
    }

    @Override
    public String getId() {
        return "com.openexchange.halo.events";
    }

    @Override
    public boolean isAvailable(ServerSession session) {
        return session.getUserPermissionBits().hasCalendar();
    }

    @Override
    public AJAXRequestResult investigate(HaloContactQuery query, AJAXRequestData request, ServerSession session) throws OXException {
        /*
         * extract search term from halo query
         */
        SearchTerm<?> searchTerm = getSearchTerm(query, session);
        if (null == searchTerm) {
            return AJAXRequestResult.EMPTY_REQUEST_RESULT;
        }
        /*
         * init calendar access & search matching events & return appropriate result
         */
        Map<String, EventsResult> resultsPerFolder = null;
        IDBasedCalendarAccess calendarAccess = initCalendarAccess(request);
        boolean committed = false;
        try {
            calendarAccess.startTransaction();
            List<String> folderIds = getSearchedFolderIds(calendarAccess);
            if (folderIds.isEmpty()) {
                return AJAXRequestResult.EMPTY_REQUEST_RESULT;
            }
            resultsPerFolder = calendarAccess.searchEvents(folderIds, searchTerm);
            calendarAccess.commit();
            committed = true;
        } finally {
            if (false == committed) {
                calendarAccess.rollback();
            }
            calendarAccess.finish();
        }
        /*
         * convert to & return appropriate result
         */
        return getRequestResult(resultsPerFolder);
    }

    private static AJAXRequestResult getRequestResult(Map<String, EventsResult> resultsPerFolder) {
        List<OXException> warnings = new ArrayList<OXException>();
        long maximumTimestamp = 0L;
        List<Event> events = new ArrayList<Event>();
        for (EventsResult result : resultsPerFolder.values()) {
            if (null != result.getEvents()) {
                events.addAll(result.getEvents());
                maximumTimestamp = Math.max(maximumTimestamp, result.getTimestamp());
            }
            if (null != result.getError()) {
                warnings.add(result.getError());
            }
        }
        AJAXRequestResult result = new AJAXRequestResult(events, 0L == maximumTimestamp ? null : new Date(maximumTimestamp), EventResultConverter.INPUT_FORMAT);
        result.addWarnings(warnings);
        return result;
    }

    private IDBasedCalendarAccess initCalendarAccess(AJAXRequestData requestData) throws OXException {
        CalendarParameters calendarParameters = parseParameters(requestData, REQUIRED_PARAMETERS, OPTIONAL_PARAMETERS);
        return services.getServiceSafe(IDBasedCalendarAccessFactory.class).createAccess(requestData.getSession(), calendarParameters);
    }

    private SearchTerm<?> getSearchTerm(HaloContactQuery query, ServerSession session) {
        if (null != query.getUser()) {
            return getUserSearchTerm(query.getUser(), session);
        }

        if (null != query.getContact()) {
            SearchTerm<?> participantSearchTerm = getParticipantSearchTerm(query.getContact());
            if (participantSearchTerm != null) {
                CompositeSearchTerm searchTerm = new CompositeSearchTerm(CompositeOperation.AND);
                searchTerm.addSearchTerm(participantSearchTerm);
                searchTerm.addSearchTerm(getUserSearchTerm(null, session));
                return searchTerm;
            }
        }

        if (null != query.getMergedContacts() && 0 < query.getMergedContacts().size()) {
            CompositeSearchTerm searchTerm = new CompositeSearchTerm(CompositeOperation.AND);
            for (Contact contact : query.getMergedContacts()) {
                SearchTerm<?> participantSearchTerm = getParticipantSearchTerm(contact);
                if (null != participantSearchTerm) {
                    searchTerm.addSearchTerm(searchTerm);
                    searchTerm.addSearchTerm(getUserSearchTerm(null, session));
                    return searchTerm;
                }
            }
        }

        return null;
    }

    private SearchTerm<?> getUserSearchTerm(User user, ServerSession session) {
        if (user == null) {
            return getSingleUserSearchTerm(session.getUserId());
        }

        //@formatter:off
        return new CompositeSearchTerm(CompositeOperation.AND)
            .addSearchTerm(getSingleUserSearchTerm(session.getUserId()))
            .addSearchTerm(getSingleUserSearchTerm(user.getId()));
        //@formatter:on
    }

    private SearchTerm<?> getSingleUserSearchTerm(int userId) {
        return SearchUtils.getSearchTerm(EventField.ATTENDEES, SingleOperation.EQUALS, I(userId));
    }

    private SearchTerm<?> getParticipantSearchTerm(Contact contact) {
        List<String> addresses = getEMailAddresses(contact);
        if (addresses.isEmpty()) {
            return null;
        }

        if (1 == addresses.size()) {
            return getSingleParticipantSearchTerm(addresses.get(0));
        }

        CompositeSearchTerm compositeTerm = new CompositeSearchTerm(CompositeOperation.OR);
        for (String address : addresses) {
            compositeTerm.addSearchTerm(getSingleParticipantSearchTerm(address));
        }
        return compositeTerm;
    }

    private SearchTerm<?> getSingleParticipantSearchTerm(String address) {
        return SearchUtils.getSearchTerm(EventField.ATTENDEES, SingleOperation.EQUALS, address);
    }

    private static List<String> getSearchedFolderIds(IDBasedCalendarAccess calendarAccess) throws OXException {
        List<String> folderIds = new ArrayList<String>();
        for (AccountAwareCalendarFolder folder : calendarAccess.getVisibleFolders(GroupwareFolderType.PRIVATE)) {
            folderIds.add(folder.getId());
        }
        for (AccountAwareCalendarFolder folder : calendarAccess.getVisibleFolders(GroupwareFolderType.PUBLIC)) {
            folderIds.add(folder.getId());
        }
        return folderIds;
    }
}
