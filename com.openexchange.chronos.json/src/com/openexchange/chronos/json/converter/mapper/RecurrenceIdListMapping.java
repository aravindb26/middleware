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

package com.openexchange.chronos.json.converter.mapper;

import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.DefaultRecurrenceId;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.tools.mappings.json.DefaultJsonMapping;
import com.openexchange.session.Session;

/**
 * {@link RecurrenceIdListMapping}>
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.4
 */
public abstract class RecurrenceIdListMapping<O> extends DefaultJsonMapping<SortedSet<RecurrenceId>, O> {

    /**
     * Initializes a new {@link RecurrenceIdListMapping}.
     *
     * @param ajaxName The mapped ajax name
     * @param columnID The mapped column identifier
     */
    public RecurrenceIdListMapping(String ajaxName, Integer columnID) {
		super(ajaxName, columnID);
	}

    @Override
    public void deserialize(JSONObject from, O to) throws JSONException, OXException {
        if (from.isNull(getAjaxName())) {
            set(to, null);
        } else {
            JSONArray jsonArray = from.getJSONArray(getAjaxName());
            SortedSet<RecurrenceId> recurrenceIds = new TreeSet<RecurrenceId>();
            for (int i = 0; i < jsonArray.length(); i++) {
                recurrenceIds.add(new DefaultRecurrenceId(CalendarUtils.decode(jsonArray.getString(i))));
            }
            set(to, recurrenceIds);
        }
    }

    @Override
    public Object serialize(O from, TimeZone timeZone, Session session) throws JSONException {
        SortedSet<RecurrenceId> recurrenceIds = get(from);
        if (null == recurrenceIds) {
            return null;
        }
        JSONArray jsonArray = new JSONArray(recurrenceIds.size());
        int i = 0;
        for (RecurrenceId recurrenceId : recurrenceIds) {
            jsonArray.put(i++, recurrenceId.toString());
        }
        return jsonArray;
    }

}
