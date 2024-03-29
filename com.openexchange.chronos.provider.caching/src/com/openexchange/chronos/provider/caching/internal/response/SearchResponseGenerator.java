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

package com.openexchange.chronos.provider.caching.internal.response;

import java.util.List;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.provider.caching.basic.BasicCachingCalendarAccess;
import com.openexchange.chronos.provider.caching.internal.Services;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.SearchOptions;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.chronos.storage.operation.OSGiCalendarStorageOperation;
import com.openexchange.exception.OXException;
import com.openexchange.search.SearchTerm;

/**
 * {@link SearchResponseGenerator}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class SearchResponseGenerator extends ResponseGenerator {

    public SearchResponseGenerator(BasicCachingCalendarAccess cachedCalendarAccess) {
        super(cachedCalendarAccess);
    }

    /**
     * Searches for events using a generic search term.
     *
     * @param term The search term
     * @return The found events, or an empty list if there are none
     */
    public List<Event> generate(SearchTerm<?> term) throws OXException {
        CalendarParameters parameters = cachedCalendarAccess.getParameters();
        SearchOptions searchOptions = new SearchOptions(parameters);
        return new OSGiCalendarStorageOperation<List<Event>>(Services.getServiceLookup(), cachedCalendarAccess.getSession().getContextId(), cachedCalendarAccess.getAccount().getAccountId()) {

            @Override
            protected List<Event> call(CalendarStorage storage) throws OXException {
                CalendarParameters parameters = cachedCalendarAccess.getParameters();
                EventField[] fields = getFields(parameters.get(CalendarParameters.PARAMETER_FIELDS, EventField[].class), EventField.FOLDER_ID);
                List<Event> events = storage.getEventStorage().searchEvents(term, searchOptions, fields);
                List<Event> enhancedEvents = storage.getUtilities().loadAdditionalEventData(cachedCalendarAccess.getAccount().getUserId(), events, fields);
                return postProcess(enhancedEvents);
            }
        }.executeQuery();
    }

}
